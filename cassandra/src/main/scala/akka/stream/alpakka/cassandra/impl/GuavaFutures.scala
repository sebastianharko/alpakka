/*
 * Copyright (C) 2016-2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.cassandra.impl

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

private[cassandra] object GuavaFutures {

  def invokeTryCallback[T](listenableFuture: ListenableFuture[T],
                           executor: java.util.concurrent.Executor)(callback: Try[T] => Unit): Unit =
    Futures.addCallback(
      listenableFuture,
      new FutureCallback[T] {
        override def onSuccess(result: T): Unit = callback(Success(result))
        override def onFailure(t: Throwable): Unit = callback(Failure(t))
      },
      executor
    )

  implicit final class GuavaFutureOpts[A](val guavaFut: ListenableFuture[A]) extends AnyVal {
    def asScala(): Future[A] = {
      val p = Promise[A]()
      val callback = new FutureCallback[A] {
        override def onSuccess(a: A): Unit = p.success(a)
        override def onFailure(err: Throwable): Unit = p.failure(err)
      }
      Futures.addCallback(guavaFut, callback)
      p.future
    }
  }
}
