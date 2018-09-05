/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.amqp

import akka.annotation.InternalApi

import java.util.{List => JavaList, Map => JavaMap}

import scala.collection.immutable
import scala.collection.JavaConverters._

/**
 * Internal API
 */
@InternalApi
sealed trait AmqpConnectorSettings {
  def connectionProvider: AmqpConnectionProvider
  def declarations: immutable.Seq[Declaration]
}

sealed trait AmqpSourceSettings extends AmqpConnectorSettings

final class NamedQueueSourceSettings private (
    val connectionProvider: AmqpConnectionProvider,
    val queue: String,
    val declarations: immutable.Seq[Declaration] = immutable.Seq.empty,
    val noLocal: Boolean = false,
    val exclusive: Boolean = false,
    val ackRequired: Boolean = true,
    val consumerTag: String = "default",
    val arguments: Map[String, AnyRef] = Map.empty
) extends AmqpSourceSettings {

  def withDeclarations(declaration: immutable.Seq[Declaration]): NamedQueueSourceSettings =
    copy(declarations = declarations)

  /**
   * Java API
   */
  def withDeclarations(declarations: JavaList[Declaration]): NamedQueueSourceSettings =
    copy(declarations = declarations.asScala.toIndexedSeq)

  def withNoLocal(noLocal: Boolean): NamedQueueSourceSettings =
    copy(noLocal = noLocal)

  def withExclusive(exclusive: Boolean): NamedQueueSourceSettings =
    copy(exclusive = exclusive)

  def withAckRequired(ackRequired: Boolean): NamedQueueSourceSettings =
    copy(ackRequired = ackRequired)

  def withConsumerTag(consumerTag: String): NamedQueueSourceSettings =
    copy(consumerTag = consumerTag)

  def withArguments(arguments: Map[String, AnyRef]): NamedQueueSourceSettings =
    copy(arguments = arguments)

  /**
   * Java API
   */
  def withArguments(arguments: JavaMap[String, Object]): NamedQueueSourceSettings =
    copy(arguments = arguments.asScala.toMap)

  private def copy(declarations: immutable.Seq[Declaration] = declarations,
                   noLocal: Boolean = noLocal,
                   exclusive: Boolean = exclusive,
                   ackRequired: Boolean = ackRequired,
                   consumerTag: String = consumerTag,
                   arguments: Map[String, AnyRef] = arguments) =
    new NamedQueueSourceSettings(
      connectionProvider,
      queue,
      declarations = declarations,
      noLocal = noLocal,
      exclusive = exclusive,
      ackRequired = ackRequired,
      consumerTag = consumerTag,
      arguments = arguments
    )

  override def toString: String =
    s"NamedQueueSourceSettings(connectionProvider=$connectionProvider, queue=$queue, declarations=$declarations, noLocal=$noLocal, exclusive=$exclusive, ackRequired=$ackRequired, consumerTag=$consumerTag, arguments=$arguments)"
}

object NamedQueueSourceSettings {
  def apply(connectionProvider: AmqpConnectionProvider, queue: String): NamedQueueSourceSettings =
    new NamedQueueSourceSettings(connectionProvider, queue)

  /**
   * Java API
   */
  def create(connectionProvider: AmqpConnectionProvider, queue: String): NamedQueueSourceSettings =
    NamedQueueSourceSettings(connectionProvider, queue)
}

final class TemporaryQueueSourceSettings private (
    val connectionProvider: AmqpConnectionProvider,
    val exchange: String,
    val declarations: immutable.Seq[Declaration] = Nil,
    val routingKey: Option[String] = None
) extends AmqpSourceSettings {

  def withDeclarations(declaration: immutable.Seq[Declaration]): TemporaryQueueSourceSettings =
    copy(declarations = declarations)

  /**
   * Java API
   */
  def withDeclarations(declarations: JavaList[Declaration]): TemporaryQueueSourceSettings =
    copy(declarations = declarations.asScala.toIndexedSeq)

  def withRoutingKey(routingKey: String): TemporaryQueueSourceSettings = copy(routingKey = Some(routingKey))

  private def copy(declarations: immutable.Seq[Declaration] = declarations, routingKey: Option[String] = routingKey) =
    new TemporaryQueueSourceSettings(connectionProvider, exchange, declarations = declarations, routingKey = routingKey)

  override def toString: String =
    s"TemporaryQueueSourceSettings(connectionProvider=$connectionProvider, exchange=$exchange, declarations=$declarations, routingKey=$routingKey)"
}

object TemporaryQueueSourceSettings {
  def apply(connectionProvider: AmqpConnectionProvider, exchange: String) =
    new TemporaryQueueSourceSettings(connectionProvider, exchange)

  /**
   * Java API
   */
  def create(connectionProvider: AmqpConnectionProvider, exchange: String): TemporaryQueueSourceSettings =
    TemporaryQueueSourceSettings(connectionProvider, exchange)
}

final class AmqpReplyToSinkSettings private (
    val connectionProvider: AmqpConnectionProvider,
    val failIfReplyToMissing: Boolean = true
) extends AmqpConnectorSettings {
  override final val declarations = Nil

  def withFailIfReplyToMissing(failIfReplyToMissing: Boolean): AmqpReplyToSinkSettings =
    copy(failIfReplyToMissing = failIfReplyToMissing)

  private def copy(connectionProvider: AmqpConnectionProvider = connectionProvider,
                   failIfReplyToMissing: Boolean = failIfReplyToMissing) =
    new AmqpReplyToSinkSettings(connectionProvider, failIfReplyToMissing)

  override def toString: String =
    s"AmqpReplyToSinkSettings(connectionProvider=$connectionProvider, failIfReplyToMissing=$failIfReplyToMissing)"
}

object AmqpReplyToSinkSettings {
  def apply(connectionProvider: AmqpConnectionProvider): AmqpReplyToSinkSettings =
    new AmqpReplyToSinkSettings(connectionProvider)

  /**
   * Java API
   */
  def create(connectionProvider: AmqpConnectionProvider): AmqpReplyToSinkSettings =
    AmqpReplyToSinkSettings(connectionProvider)
}

final class AmqpSinkSettings private (
    val connectionProvider: AmqpConnectionProvider,
    val exchange: Option[String] = None,
    val routingKey: Option[String] = None,
    val declarations: immutable.Seq[Declaration] = Nil
) extends AmqpConnectorSettings {

  def withExchange(exchange: String): AmqpSinkSettings =
    copy(exchange = Some(exchange))

  def withRoutingKey(routingKey: String): AmqpSinkSettings =
    copy(routingKey = Some(routingKey))

  def withDeclarations(declaration: immutable.Seq[Declaration]): AmqpSinkSettings =
    copy(declarations = declarations)

  /**
   * Java API
   */
  def withDeclarations(declarations: JavaList[Declaration]): AmqpSinkSettings =
    copy(declarations = declarations.asScala.toIndexedSeq)

  private def copy(connectionProvider: AmqpConnectionProvider = connectionProvider,
                   exchange: Option[String] = exchange,
                   routingKey: Option[String] = routingKey,
                   declarations: immutable.Seq[Declaration] = declarations) =
    new AmqpSinkSettings(connectionProvider, exchange, routingKey, declarations)

  override def toString: String =
    s"AmqpSinkSettings(connectionProvider=$connectionProvider, exchange=$exchange, routingKey=$routingKey, declarations=$declarations)"
}

object AmqpSinkSettings {
  def apply(connectionProvider: AmqpConnectionProvider): AmqpSinkSettings =
    new AmqpSinkSettings(connectionProvider)

  /**
   * Java API
   */
  def create(connectionProvider: AmqpConnectionProvider): AmqpSinkSettings =
    AmqpSinkSettings(connectionProvider)
}

sealed trait Declaration

final class QueueDeclaration private (
    val name: String,
    val durable: Boolean = false,
    val exclusive: Boolean = false,
    val autoDelete: Boolean = false,
    val arguments: Map[String, AnyRef] = Map.empty
) extends Declaration {

  def withDurable(durable: Boolean): QueueDeclaration =
    copy(durable = durable)

  def withExclusive(exclusive: Boolean): QueueDeclaration =
    copy(exclusive = exclusive)

  def withAutoDelete(autoDelete: Boolean): QueueDeclaration =
    copy(autoDelete = autoDelete)

  def withArguments(arguments: Map[String, AnyRef]): QueueDeclaration =
    copy(arguments = arguments)

  /**
   * Java API
   */
  def withArguments(arguments: JavaMap[String, Object]): QueueDeclaration =
    copy(arguments = arguments.asScala.toMap)

  private def copy(name: String = name,
                   durable: Boolean = durable,
                   exclusive: Boolean = exclusive,
                   autoDelete: Boolean = autoDelete,
                   arguments: Map[String, AnyRef] = arguments) =
    new QueueDeclaration(name, durable, exclusive, autoDelete, arguments)

  override def toString: String =
    s"QueueDeclaration(name=$name, durable=$durable, exclusive=$exclusive, autoDelete=$autoDelete, arguments=$arguments)"
}

object QueueDeclaration {
  def apply(name: String): QueueDeclaration = new QueueDeclaration(name)

  /**
   * Java API
   */
  def create(name: String): QueueDeclaration = QueueDeclaration(name)
}

final class BindingDeclaration private (
    val queue: String,
    val exchange: String,
    val routingKey: Option[String] = None,
    val arguments: Map[String, AnyRef] = Map.empty
) extends Declaration {

  def withRoutingKey(routingKey: String): BindingDeclaration = copy(routingKey = Some(routingKey))

  def withArguments(arguments: Map[String, AnyRef]): BindingDeclaration =
    copy(arguments = arguments)

  /**
   * Java API
   */
  def withArguments(arguments: JavaMap[String, Object]): BindingDeclaration =
    copy(arguments = arguments.asScala.toMap)

  private def copy(routingKey: Option[String] = routingKey, arguments: Map[String, AnyRef] = arguments) =
    new BindingDeclaration(queue, exchange, routingKey, arguments)

  override def toString: String =
    s"BindingDeclaration(queue=$queue, exchange=$exchange, routingKey=$routingKey, arguments=$arguments)"
}

object BindingDeclaration {
  def apply(queue: String, exchange: String): BindingDeclaration =
    new BindingDeclaration(queue, exchange)

  /**
   * Java API
   */
  def create(queue: String, exchange: String): BindingDeclaration =
    BindingDeclaration(queue, exchange)
}

final class ExchangeDeclaration private (
    val name: String,
    val exchangeType: String,
    val durable: Boolean = false,
    val autoDelete: Boolean = false,
    val internal: Boolean = false,
    val arguments: Map[String, AnyRef] = Map.empty
) extends Declaration {

  def withDurable(durable: Boolean): ExchangeDeclaration = copy(durable = durable)

  def withAutoDelete(autoDelete: Boolean): ExchangeDeclaration = copy(autoDelete = autoDelete)

  def withInternal(internal: Boolean): ExchangeDeclaration = copy(internal = internal)

  def withArguments(arguments: Map[String, AnyRef]): ExchangeDeclaration =
    copy(arguments = arguments)

  /**
   * Java API
   */
  def withArguments(arguments: JavaMap[String, Object]): ExchangeDeclaration =
    copy(arguments = arguments.asScala.toMap)

  private def copy(durable: Boolean = durable,
                   autoDelete: Boolean = autoDelete,
                   internal: Boolean = internal,
                   arguments: Map[String, AnyRef] = arguments) =
    new ExchangeDeclaration(name, exchangeType, durable, autoDelete, internal, arguments)

  override def toString: String =
    s"ExchangeDeclaration(name=$name, exchangeType=$exchangeType, durable=$durable, autoDelete=$autoDelete, internal=$internal, arguments=$arguments)"
}

object ExchangeDeclaration {
  def apply(name: String, exchangeType: String): ExchangeDeclaration =
    new ExchangeDeclaration(name, exchangeType)

  /**
   * Java API
   */
  def create(name: String, exchangeType: String): ExchangeDeclaration = ExchangeDeclaration(name, exchangeType)
}
