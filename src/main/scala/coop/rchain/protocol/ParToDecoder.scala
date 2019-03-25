package coop.rchain.protocol

import java.io.StringReader

import com.typesafe.scalalogging.Logger
import coop.rchain.domain.{Err, OpCode}
import coop.rchain.models.{Expr, GPrivate, Par}
import monix.eval.Coeval

object ParUtil {

  val log = Logger("DeParConverter")

  import coop.rchain.rholang.interpreter.ParBuilder
  import ParBuilder._

  implicit class parOps(rTerm: String) {
    def asPar: Either[Err, Par] = {
      ParBuilder[Coeval].buildNormalizedTerm(rTerm).runAttempt() match {
        case Left(e) =>
          println(e)
          log.error(s"String2Par failed with Exception: ${e}")
          Left(Err(OpCode.nameToPar, e.getMessage))
        case Right(r) =>
          log.info(s"rTerm: ${rTerm} Par: ${r}")
          Right(r)
      }
    }
  }

}