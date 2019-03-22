package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import coop.rchain.utils.Globals
import scalacache._
import scalacache.redis._
import scalacache.serialization.binary._

import scala.util._
import scalacache.modes.try_._
import coop.rchain.utils.ErrImplicits._


object RSongUserCache {


  private final val initialPlayCount=50
  val log = Logger[RSongUserCache.type]

  val (redisUrl, redisPort) =
    (Globals.appCfg.getString("redis.url"),
      Globals.appCfg.getInt("redis.port"))

  implicit val rsongUserCache: Cache[CachedRSongUser] =
    RedisCache(redisUrl, redisPort)

  val getOrCreateUser: String => Either[Err,CachedRSongUser] =
    name => {
      get(name) match {
        case Success(Some(user)) =>
          Right(user)
        case Success(None) =>
//          val _=Future { newUser(name)(proxy)}  //TODO micro batch will have to do this
          log.info(s"user: $name is not in cache. Creating user: $name")
          put(name)(CachedRSongUser(name, PlayCount(initialPlayCount)) )
          Right(CachedRSongUser(name, PlayCount(initialPlayCount)) )
        case Failure(e) =>
          Left(Err(OpCode.cacheLayer,e.getMessage))
      }
    }

  def viewPlayCount(userId: String): Either[Err,PlayCount] = {
    for {
      x <- get(userId).asErr
      z <- x match {
        case Some(CachedRSongUser(_, playCount,_,_)) => Right(playCount)
        case None => Left(Err(OpCode.unregisteredUser,
          s"user $userId is not registered"))
      }
    } yield (z)
  }

  private def updateCache(cachedUser: CachedRSongUser) = {
    for {
      _ <- remove(cachedUser.rsongUserId)
      v <- put(cachedUser.rsongUserId)(cachedUser)
    } yield(v)
    cachedUser
  }

  def decPlayCount(songId: String, userId: String) = {
    get(userId).asErr match {
      case Right(None) =>
        Left(Err(OpCode.unregisteredUser,
          s"Attempted to decrement playcount for unregeistered user with id=$userId!"))
      case Right(Some(u)) =>
         Right(updateCache(u.copy(
           playCount = PlayCount(u.playCount.current-1)
           )
         ))
      case Left(e)  => Left(e)
    }
  }
}
