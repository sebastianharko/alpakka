/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.amqp.impl

import akka.Done
import akka.annotation.InternalApi
import akka.stream.alpakka.amqp.{AmqpReplyToSinkSettings, OutgoingMessage}
import akka.stream.stage.{GraphStageLogic, GraphStageWithMaterializedValue, InHandler}
import akka.stream.{ActorAttributes, Attributes, Inlet, SinkShape}

import scala.concurrent.{Future, Promise}

/**
 * Connects to an AMQP server upon materialization and sends incoming messages to the server.
 * Each materialized sink will create one connection to the broker. This stage sends messages to
 * the queue named in the replyTo options of the message instead of from settings declared at construction.
 */
@InternalApi
private[amqp] final class AmqpReplyToSinkStage(settings: AmqpReplyToSinkSettings)
    extends GraphStageWithMaterializedValue[SinkShape[OutgoingMessage], Future[Done]] { stage =>

  val in = Inlet[OutgoingMessage]("AmqpReplyToSink.in")

  override def shape: SinkShape[OutgoingMessage] = SinkShape.of(in)

  override protected def initialAttributes: Attributes =
    Attributes
      .name("AmqpReplyToSink")
      .and(ActorAttributes.dispatcher("akka.stream.default-blocking-io-dispatcher"))

  override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Done]) = {
    val promise = Promise[Done]()
    (new GraphStageLogic(shape) with AmqpConnectorLogic {
      override val settings = stage.settings

      override def whenConnected(): Unit = pull(in)

      override def postStop(): Unit = {
        promise.tryFailure(new RuntimeException("stage stopped unexpectedly"))
        super.postStop()
      }

      override def onFailure(ex: Throwable): Unit = {
        promise.tryFailure(ex)
        super.onFailure(ex)
      }

      setHandler(
        in,
        new InHandler {

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(Done)
            super.onUpstreamFinish()
          }

          override def onPush(): Unit = {
            val elem = grab(in)

            val replyTo = elem.properties.map(_.getReplyTo)

            if (replyTo.isDefined) {
              channel.basicPublish(
                elem.routingKey.getOrElse(""),
                replyTo.get,
                elem.mandatory,
                elem.immediate,
                elem.properties.orNull,
                elem.bytes.toArray
              )
            } else if (settings.failIfReplyToMissing) {
              onFailure(new RuntimeException("Reply-to header was not set"))
            }

            tryPull(in)
          }
        }
      )

    }, promise.future)
  }

  override def toString: String = "AmqpReplyToSink"
}
