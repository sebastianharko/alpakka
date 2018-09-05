/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.impl

import akka.annotation.InternalApi
import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import com.datastax.driver.core.{ResultSet, Row, Session, Statement}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * INTERNAL API
 */
@InternalApi private[cassandra] final class CassandraSourceStage(futStmt: Future[Statement], session: Session)
    extends GraphStage[SourceShape[Row]] {
  val out: Outlet[Row] = Outlet("CassandraSource.out")
  override val shape: SourceShape[Row] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var maybeRs = Option.empty[ResultSet]
    var futFetchedCallback: Try[ResultSet] => Unit = _
    var isFetching = true // set to true until prestart's callback will set to false
    var minimumPreFetchSize: Int = _

    override def preStart(): Unit = {
      futFetchedCallback = getAsyncCallback[Try[ResultSet]](tryPushAfterFetch).invoke
      val exec = materializer.executionContext
      futStmt.foreach { stmt: Statement =>
        minimumPreFetchSize = math.max(1, stmt.getFetchSize / 2)
        val gFut = session.executeAsync(stmt)
        GuavaFutures.invokeTryCallback(gFut, exec)(futFetchedCallback)
      }(exec)
    }

    setHandler(
      out,
      new OutHandler {
        override def onPull(): Unit = maybeRs.foreach { rs =>
          val currentlyAvailableRows = rs.getAvailableWithoutFetching

          if (!isFetching && currentlyAvailableRows < minimumPreFetchSize) {
            isFetching = true
            // fetch next page
            val gFut = rs.fetchMoreResults()
            val exec = materializer.executionContext
            GuavaFutures.invokeTryCallback(gFut, exec)(futFetchedCallback)
          }

          if (currentlyAvailableRows > 0)
            push(out, rs.one())
        }
      }
    )

    private[this] def tryPushAfterFetch(rsOrFailure: Try[ResultSet]): Unit = rsOrFailure match {
      case Success(newRs) =>
        isFetching = false

        val rs = maybeRs.getOrElse {
          maybeRs = Some(newRs)
          newRs
        }

        if (rs.getAvailableWithoutFetching > 0) {
          if (isAvailable(out)) {
            push(out, rs.one())
          }
        } else {
          completeStage()
        }

      case Failure(failure) => failStage(failure)
    }
  }
}
