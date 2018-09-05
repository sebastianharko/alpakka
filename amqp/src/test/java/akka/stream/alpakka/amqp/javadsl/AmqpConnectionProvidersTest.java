/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.amqp.javadsl;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

import akka.japi.Pair;
import akka.stream.alpakka.amqp.*;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Test;

import java.net.ConnectException;

public class AmqpConnectionProvidersTest {
  @Test
  public void LocalAmqpConnectionCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void LocalAmqpConnectionReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionUriCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionUriReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionDetailsCreatesNewConnection() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionDetailsReleaseClosedConnectionDoNotError() throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void AmqpConnectionFactoryCreatesNewConnection() throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    Connection connection2 = connectionProvider.get();
    assertNotEquals(connection1, connection2);
    connectionProvider.release(connection1);
    connectionProvider.release(connection2);
  }

  @Test
  public void AmqpConnectionFactoryReleaseClosedConnectionDoNotError() throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    Connection connection1 = connectionProvider.get();
    connectionProvider.release(connection1);
    connectionProvider.release(connection1);
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndLocalAmqpConnectionReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndAmqpConnectionUriReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndAmqpConnectionDetailsReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithAutomaticReleaseAndmqpConnectionFactoryReusesConnection()
          throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertTrue(connection1.isOpen());
    assertTrue(connection2.isOpen());
    reusableConnectionProvider.release(connection2);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndLocalAmqpConnectionReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider = AmqpLocalConnectionProvider.getInstance();
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionUriReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpUriConnectionProvider.create("amqp://localhost:5672");
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionDetailsReusesConnection()
          throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void
      ReusableAMQPConnectionProviderWithoutAutomaticReleaseAndAmqpConnectionFactoryReusesConnection()
          throws Exception {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    @SuppressWarnings("unchecked")
    AmqpConnectionProvider connectionProvider =
        AmqpConnectionFactoryConnectionProvider.create(connectionFactory)
            .withHostAndPort("localhost", 5672);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    Connection connection1 = reusableConnectionProvider.get();
    Connection connection2 = reusableConnectionProvider.get();
    assertEquals(connection1, connection2);
    reusableConnectionProvider.release(connection1);
    assertFalse(connection1.isOpen());
    assertFalse(connection2.isOpen());
  }

  @Test
  public void ReusableAMQPConnectionProviderNeverLeftInInvalidStateUponException()
      throws Exception {
    AmqpConnectionProvider connectionProvider =
        AmqpDetailsConnectionProvider.create("localhost", 5673);
    AmqpConnectionProvider reusableConnectionProvider =
        AmqpCachedConnectionProvider.create(connectionProvider).withAutomaticRelease(false);
    try {
      reusableConnectionProvider.get();
    } catch (Exception e) {
      assertThat(e, instanceOf(ConnectException.class));
    }

    try {
      reusableConnectionProvider.get();
    } catch (Exception e) {
      assertThat(e, instanceOf(ConnectException.class));
    }
  }
}
