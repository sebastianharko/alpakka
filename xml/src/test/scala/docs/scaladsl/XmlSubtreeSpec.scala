/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.alpakka.xml.scaladsl.XmlParsing
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import docs.javadsl.XmlHelper
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class XmlSubtreeSpec extends WordSpec with Matchers with BeforeAndAfterAll {
  implicit val system: ActorSystem = ActorSystem("Test")
  implicit val mat: Materializer = ActorMaterializer()

  //#subtree
  val parse = Flow[String]
    .map(ByteString(_))
    .via(XmlParsing.parser)
    .via(XmlParsing.subtree("doc" :: "elem" :: "item" :: Nil))
    .toMat(Sink.seq)(Keep.right)
  //#subtree

  "XML subtree support" must {

    "properly extract subtree of events" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |    <item>i1</item>
          |    <item>i2</item>
          |    <item>i3</item>
          |  </elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)

      result.map(XmlHelper.asString(_).trim) should ===(
        Seq(
          "<item>i1</item>",
          "<item>i2</item>",
          "<item>i3</item>"
        )
      )
    }

    "properly extract subtree of nested events" in {

      //#subtree-usage
      val doc =
        """
          |<doc>
          |  <elem>
          |    <item>i1</item>
          |    <item><sub>i2</sub></item>
          |    <item>i3</item>
          |  </elem>
          |</doc>
        """.stripMargin
      val resultFuture = Source.single(doc).runWith(parse)
      //#subtree-usage

      val result = Await.result(resultFuture, 3.seconds)

      result.map(XmlHelper.asString(_).trim) should ===(
        Seq(
          "<item>i1</item>",
          "<item><sub>i2</sub></item>",
          "<item>i3</item>"
        )
      )
    }

    "properly ignore matches not deep enough" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |     I am lonely here :(
          |  </elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(Nil)
    }

    "properly ignore partial matches" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |     <notanitem>ignore me</notanitem>
          |     <notanitem>ignore me</notanitem>
          |     <foo>ignore me</foo>
          |  </elem>
          |  <bar></bar>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(Nil)
    }

    "properly filter from the combination of the above" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |    <notanitem>ignore me</notanitem>
          |    <notanitem>ignore me</notanitem>
          |    <foo>ignore me</foo>
          |    <item>i1</item>
          |    <item><sub>i2</sub></item>
          |    <item>i3</item>
          |  </elem>
          |  <elem>
          |    not me please
          |  </elem>
          |  <elem><item>i4</item></elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)

      result.map(XmlHelper.asString(_).trim) should ===(
        Seq(
          "<item>i1</item>",
          "<item><sub>i2</sub></item>",
          "<item>i3</item>",
          "<item>i4</item>"
        )
      )
    }

  }

  override protected def afterAll(): Unit = system.terminate()
}
