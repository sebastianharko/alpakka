/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.reference.impl

import akka.annotation.InternalApi
import akka.event.Logging
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import akka.stream.alpakka.reference.ReferenceWriteMessage
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

/**
 * INTERNAL API
 *
 * Private package hides the class from the API in Scala. However it is still
 * visible in Java. Use "InternalApi" annotation and "INTERNAL API" as the first
 * line in scaladoc to communicate to Java developers that this is private API.
 */
@InternalApi private[reference] final class ReferenceFlowStageLogic(
    val shape: FlowShape[ReferenceWriteMessage, ReferenceWriteMessage]
) extends GraphStageLogic(shape) {

  private def in = shape.in
  private def out = shape.out

  /**
   * Initialization logic
   */
  override def preStart(): Unit = {}

  setHandler(
    in,
    new InHandler {
      override def onPush(): Unit = {
        val msg = grab(in)
        val total = msg.metrics.values.sum
        push(out, msg.withMetrics(msg.metrics + ("total" -> total)))
      }
    }
  )

  setHandler(out, new OutHandler {
    override def onPull(): Unit = pull(in)
  })

  /**
   * Cleanup logic
   */
  override def postStop(): Unit = {}
}

/**
 * INTERNAL API
 */
@InternalApi private[reference] final class ReferenceFlow()
    extends GraphStage[FlowShape[ReferenceWriteMessage, ReferenceWriteMessage]] {
  val in: Inlet[ReferenceWriteMessage] = Inlet(Logging.simpleName(this) + ".in")
  val out: Outlet[ReferenceWriteMessage] = Outlet(Logging.simpleName(this) + ".out")

  override def initialAttributes: Attributes =
    Attributes.name(Logging.simpleName(this))

  override val shape: FlowShape[ReferenceWriteMessage, ReferenceWriteMessage] = FlowShape(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new ReferenceFlowStageLogic(shape)
}
