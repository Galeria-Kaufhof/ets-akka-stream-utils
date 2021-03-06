package de.kaufhof.ets.akkastreamutils.core

import java.nio.charset.{Charset, CodingErrorAction}

import akka.NotUsed
import akka.stream.IOResult
import akka.stream.scaladsl.{Flow, Keep, RunnableGraph, Sink, Source}
import akka.util.ByteString

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

//Documentation see StreamUtils.<method>Flow
object implicits {

  /************* Source/Flow shared ops ****************/

  implicit class SourceSharedOps[T, Mat](val source: Source[T, Mat]) extends AnyVal {

    def scanFinally[Out, State](initialState: => State)
                                 (f: (State, T) => (State, Option[Out]))
                                 (finalize: State => Option[Out]): Source[Out, Mat] =
      source.via(StreamUtils.scanFinallyFlow(initialState)(f)(finalize))

    def scanFinallyAsync[Out, State](initialState: => State)
                                      (f: (State, T) => Future[(State, Option[Out])])
                                      (finalize: State => Future[Option[Out]])
                                      (implicit ec: ExecutionContext): Source[Out, Mat] =
      source.via(StreamUtils.scanFinallyAsyncFlow(initialState)(f)(finalize))

    def statefulFold[State](z: => State)(f: (State, T) => State): Source[State, Mat] =
      source.via(StreamUtils.statefulFoldFlow(z)(f))

    def executeWithin(n: Int, d: FiniteDuration)(f: T => Future[Unit])(implicit ec: ExecutionContext): Source[T, Mat] =
      source.via(StreamUtils.executeWithinFlow(n, d)(f))

    def sideEffect(sideEffect: T => Unit): Source[T, Mat] =
      source.via(StreamUtils.sideEffectFlow(sideEffect))

    def toSourceSink(implicit ec: ExecutionContext): RunnableGraph[(Mat, Source[T, NotUsed])] =
      source.toMat(StreamUtils.fixedBroadcastHub[T])(Keep.both)

    def finallyFoldSink[Out](z: Out)(f: (Out, T) => Out)(implicit ec: ExecutionContext): RunnableGraph[(Mat, (Future[Unit], Future[Out]))] =
      source.toMat(StreamUtils.finallyFoldSink(z)(f))(Keep.both)

    def finallyLastOptionSink(implicit ec: ExecutionContext): RunnableGraph[(Mat, (Future[Unit], Future[Option[T]]))] =
      source.toMat(StreamUtils.finallyLastOptionSink)(Keep.both)

  }

  implicit class FlowSharedOps[In, T, Mat](val flow: Flow[In, T, Mat]) extends AnyVal {

    def scanFinally[Out, State](initialState: => State)
                                 (f: (State, T) => (State, Option[Out]))
                                 (finalize: State => Option[Out]): Flow[In, Out, Mat] =
      flow.via(StreamUtils.scanFinallyFlow(initialState)(f)(finalize))

    def scanFinallyAsync[Out, State](initialState: => State)
                                      (f: (State, T) => Future[(State, Option[Out])])
                                      (finalize: State => Future[Option[Out]])
                                      (implicit ec: ExecutionContext): Flow[In, Out, Mat] =
      flow.via(StreamUtils.scanFinallyAsyncFlow(initialState)(f)(finalize))

    def statefulFold[State](z: => State)(f: (State, T) => State): Flow[In, State, Mat] =
      flow.via(StreamUtils.statefulFoldFlow(z)(f))

    def executeWithin(n: Int, d: FiniteDuration)(f: T => Future[Unit])(implicit ec: ExecutionContext): Flow[In, T, Mat] =
      flow.via(StreamUtils.executeWithinFlow(n, d)(f))

    def sideEffect(sideEffect: T => Unit): Flow[In, T, Mat] =
      flow.via(StreamUtils.sideEffectFlow(sideEffect))

    def toSourceSink(implicit ec: ExecutionContext): Sink[In, (Mat, Source[T, NotUsed])] =
      flow.toMat(StreamUtils.fixedBroadcastHub[T])(Keep.both)

    def finallyFoldSink[Out](z: Out)(f: (Out, T) => Out)(implicit ec: ExecutionContext): Sink[In, (Mat, (Future[Unit], Future[Out]))] =
      flow.toMat(StreamUtils.finallyFoldSink(z)(f))(Keep.both)

    def finallyLastOptionSink(implicit ec: ExecutionContext): Sink[In, (Mat, (Future[Unit], Future[Option[T]]))] =
      flow.toMat(StreamUtils.finallyLastOptionSink)(Keep.both)

  }

  implicit class ByteStringSourceSharedOps[Mat](val source: Source[ByteString, Mat]) extends AnyVal {

    def decodeChar(charset: Charset,
                   bufferSize: Int = 8192,
                   onMalformedInput: CodingErrorAction = CodingErrorAction.REPLACE): Source[String, Mat] =
      source.via(StreamUtils.decodeCharFlow(charset, bufferSize, onMalformedInput))

    def minLength(minLength: Int = 64*1024): Source[ByteString, Mat] =
      source.via(StreamUtils.minLengthFlow(minLength))

    def gzipEncode(minBlockSize: Int = 64*1024): Source[ByteString, Mat] =
      source.via(StreamUtils.gzipEncodeFlow(minBlockSize))

    def stripBom: Source[ByteString, Mat] =
      source.via(StreamUtils.stripBomFlow)

  }

  implicit class ByteStringFlowSharedOps[In, Mat](val flow: Flow[In, ByteString, Mat]) extends AnyVal {

    def decodeChar(charset: Charset,
                   bufferSize: Int = 8192,
                   onMalformedInput: CodingErrorAction = CodingErrorAction.REPLACE): Flow[In, String, Mat] =
      flow.via(StreamUtils.decodeCharFlow(charset, bufferSize, onMalformedInput))

    def minLength(minLength: Int = 64*1024): Flow[In, ByteString, Mat] =
      flow.via(StreamUtils.minLengthFlow(minLength))

    def gzipEncode(minBlockSize: Int = 64*1024): Flow[In, ByteString, Mat] =
      flow.via(StreamUtils.gzipEncodeFlow(minBlockSize))

    def stripBom: Flow[In, ByteString, Mat] =
      flow.via(StreamUtils.stripBomFlow)

  }

  /************ Source only ****************/

  implicit class SourceOnlyOps[Out, Mat](val source: Source[Out, Mat]) extends AnyVal {

    def withPreMat: (Source[Out, NotUsed], Future[Mat]) = StreamUtils.preMatSource(source)

  }

  implicit class IoResultSourceOps[Out](val source: Source[Out, Future[IOResult]]) extends AnyVal {

    def failingIo(implicit ec: ExecutionContext): Source[Out, NotUsed] = StreamUtils.failingIoSource(source)

  }

  implicit class SourceFutureMatOps[Out, Mat](val source: Source[Out, Future[Mat]]) extends AnyVal {

    def withPreMat(implicit ec: ExecutionContext): (Source[Out, NotUsed], Future[Mat]) = StreamUtils.preMatFutureSource(source)

  }


}
