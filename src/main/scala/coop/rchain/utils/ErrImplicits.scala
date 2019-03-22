package coop.rchain.utils

import coop.rchain.domain.{CachingException, Err, OpCode}
import scala.util.{Either, Failure, Left, Right, Success, Try}

object ErrImplicits {
  implicit class _Either_[T](t: Try[T]) {
    def asErr: Either[Err, T] = {
      t match {
        case Success(s) => Right(s)
        case Failure(e) =>
          Left(Err(OpCode.cacheLayer, e.getMessage))
      }
    }
  }

  implicit class _Try_[E,T](e: Either[E,T]) {
    def asTry: Try[T] = {
      e match {
        case Right(s) => Success(s)
        case Left(f) =>
          util.Failure(CachingException(f.toString))
      }
    }
  }
}

