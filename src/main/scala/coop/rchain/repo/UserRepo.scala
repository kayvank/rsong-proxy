package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import scala.util._

trait UserRepo {
  def newUser(user: String): Either[Err, DeployAndProposeResponse]

  def decPlayCount(songId: String, userId: String): Either[Err, String]

  def fetchPlayCount(userId: String): Either[Err, PlayCount]

  def putPlayCountAtName(userId: String, playCountName: String): Either[Err, DeployAndProposeResponse]
}

object UserRepo {

  val COUNT_OUT = "COUNT-OUT"
  val log = Logger[UserRepo.type]

  def newUserRhoTerm(name: String): String =
    s"""@["Immersion", "newUserId"]!("${name}")"""

  def asInt(s: String): Either[Err, Int] = {
    Try(s.toInt) match {
      case Success(i) => Right(i)
      case Failure(e) =>
        Left(Err(OpCode.playCountConversion, e.getMessage))
    }
  }

  def apply(proxy: RNodeProxy) = new UserRepo {

    def newUser(user: String): Either[Err, DeployAndProposeResponse] =
      (newUserRhoTerm _ andThen proxy.deployAndPropose _) (user)

    def putPlayCountAtName(
      userId: String,
      playCountOut: String): Either[Err, DeployAndProposeResponse] =
      for {
        rhoName <- proxy.dataAtName(s"""" $userId"""")
        playCountArgs = s"""("$rhoName".hexToBytes(), "$playCountOut")"""
        term = s"""@["Immersion", "playCount"]!${playCountArgs}"""
        m <- proxy.deployAndPropose(term)
      } yield m

    def fetchPlayCount(userId: String): Either[Err, PlayCount] = {
      val playCountOut = s"$userId-${COUNT_OUT}-${System.currentTimeMillis()}"
      val pc = for {
        _ <- putPlayCountAtName(userId, playCountOut)
        count <- proxy.dataAtName(s""""$playCountOut"""")
        countAsInt <- asInt(count)
      } yield PlayCount(countAsInt)
      log.info(s"userid: $userId has ${pc}")
      pc
    }

    def decPlayCount(songId: String, userId: String) = {
      val permittedOut = s"${userId}-${songId}-permittedToPlay-${System.currentTimeMillis()}"
      val pOut = for {
        sid <- proxy.dataAtName(s""""${songId}_Stereo.izr""""")
        _ = log.info(s"rholangName= $sid for songId: $songId")
        uid <- proxy.dataAtName(s""""$userId"""")
        _ = log.info(s"rholangName= $uid for userId: $userId")
        parameters = s"""("$sid".hexToBytes(), "$uid".hexToBytes(), "$permittedOut")"""
        term = s"""@["Immersion", "play"]!${parameters}"""
        m <- proxy.deployAndPropose(term)
        p <- proxy.dataAtName(""""$permittedOut"""")
      } yield p
      log.info(s"user: $userId with song: $songId has permitedOut: $pOut")
      pOut
    }
  }
}
