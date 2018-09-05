/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.sns.javadsl

import java.util.concurrent.CompletionStage

import akka.japi.function
import akka.stream.alpakka.sns.SnsPublishFlowStage
import akka.stream.javadsl.{Flow, Keep, Sink}
import akka.{Done, NotUsed}
import com.amazonaws.services.sns.AmazonSNSAsync
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}

object SnsPublisher {

  /**
   * Java API: creates a [[akka.stream.javadsl.Flow Flow]] to publish messages to a SNS topic using an [[com.amazonaws.services.sns.AmazonSNSAsync AmazonSNSAsync]]
   */
  def createFlow(topicArn: String, snsClient: AmazonSNSAsync): Flow[String, PublishResult, NotUsed] =
    Flow
      .fromFunction(new function.Function[String, PublishRequest] {
        override def apply(msg: String): PublishRequest = new PublishRequest().withMessage(msg)
      })
      .via(createPublishFlow(topicArn, snsClient))

  /**
   * Java API: creates a [[akka.stream.javadsl.Flow Flow]] to publish messages to a SNS topic using an [[com.amazonaws.services.sns.AmazonSNSAsync AmazonSNSAsync]]
   */
  def createPublishFlow(topicArn: String, snsClient: AmazonSNSAsync): Flow[PublishRequest, PublishResult, NotUsed] =
    Flow.fromGraph(new SnsPublishFlowStage(topicArn, snsClient))

  /**
   * Java API: creates a [[akka.stream.javadsl.Sink Sink]] to publish messages to a SNS topic using an [[com.amazonaws.services.sns.AmazonSNSAsync AmazonSNSAsync]]
   */
  def createSink(topicArn: String, snsClient: AmazonSNSAsync): Sink[String, CompletionStage[Done]] =
    createFlow(topicArn, snsClient).toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

  /**
   * Java API: creates a [[akka.stream.javadsl.Sink Sink]] to publish messages to a SNS topic using an [[com.amazonaws.services.sns.AmazonSNSAsync AmazonSNSAsync]]
   */
  def createPublishSink(topicArn: String, snsClient: AmazonSNSAsync): Sink[PublishRequest, CompletionStage[Done]] =
    createPublishFlow(topicArn, snsClient).toMat(Sink.ignore(), Keep.right[NotUsed, CompletionStage[Done]])

}
