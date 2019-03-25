package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import scala.util._

object UserRepo {

  def apply(repo: Repo) = new UserRepo(repo)
}

  class UserRepo(repo: Repo) {
  val COUNT_OUT = "COUNT-OUT"
  val log = Logger[UserRepo]

  def newUserRhoTerm(name: String): String =
    s"""@["Immersion", "newUserId"]!("${name}")"""

  def asInt(s: String): Either[Err, Int] = {
    Try(s.toInt) match {
      case Success(i) => Right(i)
      case Failure(e) =>
        Left(Err(OpCode.playCountConversion, e.getMessage))
    }
  }

  val newUser: String => Either[Err, DeployAndProposeResponse] = user =>
    (newUserRhoTerm _ andThen repo.deployAndPropose _)(user)

  def putPlayCountAtName(
      userId: String,
      playCountOut: String)(proxy: RNodeProxy): Either[Err, DeployAndProposeResponse] =
    for {
      rhoName <- repo.findByName(userId)
      playCountArgs = s"""("$rhoName".hexToBytes(), "$playCountOut")"""
      term = s"""@["Immersion", "playCount"]!${playCountArgs}"""
      m <- proxy.deployAndPropose(term)
    } yield m

  def fetchPlayCount(userId: String)(proxy: RNodeProxy): Either[Err, PlayCount] = {
    val playCountOut = s"$userId-${COUNT_OUT}-${System.currentTimeMillis()}"
    val pc = for {
      _ <- putPlayCountAtName(userId, playCountOut)(proxy)
      count <- repo.findByName(playCountOut)
      countAsInt <- asInt(count)
    } yield PlayCount(countAsInt)
    log.info(s"userid: $userId has ${pc}")
    pc
  }

  def decPlayCount(songId: String, userId: String)(proxy: RNodeProxy) = {
    val permittedOut=s"${userId}-${songId}-permittedToPlay-${System.currentTimeMillis()}"
    val pOut = for {
      sid <- repo.findByName(s"${songId}_Stereo.izr")
      _=log.info(s"rholangName= $sid for songId: $songId")
      uid <-  repo.findByName(userId)
      _=log.info(s"rholangName= $uid for userId: $userId")
      parameters = s"""("$sid".hexToBytes(), "$uid".hexToBytes(), "$permittedOut")"""
      term = s"""@["Immersion", "play"]!${parameters}"""
      m <- proxy.deployAndPropose(term)
      p <- repo.findByName(permittedOut)
    } yield p
    log.info(s"user: $userId with song: $songId has permitedOut: $pOut")
    pOut
  }
}
