/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.postgresqlcdc

import java.sql.{Connection, DriverManager}

import akka.NotUsed
import akka.stream.stage._
import akka.stream.{Attributes, Outlet, SourceShape}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.matching.Regex

/** Settings for PostgreSQL CDC
 *
 * @param connectionString PostgreSQL JDBC connection string
 * @param slotName Name of the "logical decoding" slot
 * @param maxItems Specifies how many rows are fetched in one batch
 * @param duration Duration between polls
 */
final case class PostgreSQLChangeDataCaptureSettings(connectionString: String,
                                                     slotName: String,
                                                     maxItems: Int = 128,
                                                     duration: FiniteDuration = 2000 milliseconds)

sealed trait Change {
  val schemaName: String
  val tableName: String
}

case class Field(columnName: String, columnType: String, value: String)

case class RowInserted(schemaName: String, tableName: String, fields: Set[Field]) extends Change

case class RowUpdated(schemaName: String, tableName: String, fields: Set[Field]) extends Change

case class RowDeleted(schemaName: String, tableName: String, fields: Set[Field]) extends Change

case class ChangeSet(transactionId: Long, changes: Set[Change]) // TODO: add timestamp

private[postgresqlcdc] object PgSQLChangeDataCaptureSourceStage {

  import Grammar._

  /** Represents a row in the table we get from PostgreSQL when we query
   * SELECT * FROM pg_logical_slot_get_changes(..)
   */
  case class SlotChange(transactionId: Long, location: String, data: String)

  def parseProperties(properties: String): Set[Field] =
    Property
      .findAllMatchIn(properties)
      .collect {
        case regexMatch if regexMatch.groupCount == 3 =>
          // note that there is group 0 that denotes the entire match - and it is not included in the groupCount
          val columnName: String = regexMatch.group(1)
          val columnType: String = regexMatch.group(2)
          val value: String = regexMatch.group(3) match {
            case SingleQuotedString2(content) => content
            case other => other
          }
          Field(columnName, columnType, value)
      }
      .toSet

  def transformSlotChanges(slotChanges: Set[SlotChange]): Set[ChangeSet] = {

    slotChanges.groupBy(_.transactionId).map {

      case (transactionId: Long, slotChanges: Set[SlotChange]) =>
        val changes: Set[Change] = slotChanges.collect {

          case SlotChange(_, _, ChangeStatement(schemaName, tableName, "UPDATE", properties)) =>
            RowUpdated(schemaName, tableName, parseProperties(properties))

          case SlotChange(_, _, ChangeStatement(schemaName, tableName, "DELETE", properties)) =>
            RowDeleted(schemaName, tableName, parseProperties(properties))

          case SlotChange(_, _, ChangeStatement(schemaName, tableName, "INSERT", properties)) =>
            RowInserted(schemaName, tableName, parseProperties(properties))
        }

        ChangeSet(transactionId, changes)

    }
  }.filter(_.changes.nonEmpty).toSet

  object Grammar {

    // We need to parse log statements such as the following:
    //
    //  BEGIN 690
    //  table public.data: INSERT: id[integer]:3 data[text]:'3'
    //  COMMIT 690 (at 2014-02-27 16:41:51.863092+01)
    //
    // Though it's complicated to parse it's not complicated enough to justify the cost of an additional dependency (could
    // have used FastParse or Scala Parser Combinators), hence we use standard library regular expressions.

    val Begin: Regex = "BEGIN (\\d+)".r

    val Commit
      : Regex = "COMMIT (\\d+) \\(at (\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}:\\d{2}\\.\\d+\\+\\d{2})\\)".r // matches
    // a commit message like COMMIT 2380 (at 2018-04-09 17:56:36.730413+00)

    val DoubleQuotedString: String = "\"(?:\\\\\"|\"{2}|[^\"])+\"" // matches "Scala", "'Scala'"
    // or "The ""Scala"" language", "The \"Scala\" language" etc.

    val SingleQuotedString1: String = "'(?:\\\\'|'{2}|[^'])+'" // matches 'Scala', 'The ''Scala'' language' etc.

    val SingleQuotedString2: Regex = "'((?:\\\\'|'{2}|[^'])+)'".r

    val UnquotedIdentifier: String = "[^ \"'\\.]+"

    val Identifier: String = s"(?:$UnquotedIdentifier)|(?:$DoubleQuotedString)"

    val SchemaIdentifier: String = Identifier

    val TableIdentifier: String = Identifier

    val FieldIdentifier: String = Identifier

    val ChangeType: String = "\\bINSERT|\\bDELETE|\\bUPDATE"

    val TypeDeclaration: String = "[a-zA-Z0-9 ]+" // matches: [character varying] or [integer]

    val NonStringValue: String = "[^ \"']+" // matches: true, false, 3.14, 42 etc.

    val Value: String = s"(?:$NonStringValue)|(?:$SingleQuotedString1)" // matches: true, false, 3.14 or 'Strings can have spaces'

    val ChangeStatement: Regex =
      s"table ($SchemaIdentifier)\\.($TableIdentifier): ($ChangeType): (.+)".r

    val Property: Regex = s"($FieldIdentifier)\\[($TypeDeclaration)\\]:($Value)".r

  }

}

private[postgresqlcdc] class PgSQLChangeDataCaptureSourceStage(settings: PostgreSQLChangeDataCaptureSettings)
    extends GraphStage[SourceShape[ChangeSet]] {

  import PgSQLChangeDataCaptureSourceStage._

  private val out: Outlet[ChangeSet] = Outlet[ChangeSet]("PostgreSQLCDC.out")

  private val driver = "org.postgresql.Driver"
  Class.forName(driver)
  private val conn = DriverManager.getConnection(settings.connectionString)

  private val getSlotChangesStmt = conn.prepareStatement(
    "SELECT * FROM " +
    "pg_logical_slot_get_changes(?, NULL, ?, 'include-timestamp', 'on')"
  )
  getSlotChangesStmt.setString(1, settings.slotName)
  getSlotChangesStmt.setInt(2, settings.maxItems)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with StageLogging {

      private val buffer = mutable.Queue[ChangeSet]()

      override def onTimer(timerKey: Any): Unit =
        retrieveChanges()

      def retrieveChanges(): Unit = {
        val result: Set[ChangeSet] = transformSlotChanges(getSlotChanges(conn))

        if (result.isEmpty) {
          if (isAvailable(out)) {
            scheduleOnce(NotUsed, settings.duration)
          }
        } else {
          buffer ++= result
          push(out, buffer.dequeue())
        }

      }

      setHandler(
        out,
        new OutHandler {
          override def onDownstreamFinish(): Unit = {
            conn.close()
            super.onDownstreamFinish()
          }

          override def onPull(): Unit =
            if (buffer.nonEmpty)
              push(out, buffer.dequeue())
            else
              retrieveChanges()
        }
      )
    }

  private def getSlotChanges(conn: Connection): Set[SlotChange] = {
    val rs = getSlotChangesStmt.executeQuery()
    val result = ArrayBuffer[SlotChange]()
    while (rs.next()) {
      val data = rs.getString("data")
      val location = rs.getString("location")
      val transactionId = rs.getLong("xid")
      result += SlotChange(transactionId, location, data)
    }
    result.toSet
  }

  override def shape: SourceShape[ChangeSet] = SourceShape(out)

}