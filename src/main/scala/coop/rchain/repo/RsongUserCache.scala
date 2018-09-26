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

  def getOrCreateUser: String => Either[Err,CachedRSongUser] =
    name =>  {
      get(name) match {
        case Success(Some(user)) =>
          Right(user)
        case Success(None) =>
          log.info(s"user: $name is not in cache. Creating user: $name")
          put(name)(CachedRSongUser(name, PlayCount(initialPlayCount)) )
          Right(CachedRSongUser(name, PlayCount(initialPlayCount)) )
        case Failure(e) =>
          Left(Err(ErrorCode.cacheLayer,e.getMessage,Some(name)))
      }
    }

  def viewPlayCount(userId: String): Either[Err,PlayCount] = {
    for {
      x <- get(userId).asErr
      z <- x match {
        case Some(CachedRSongUser(_, playCount,_,_)) => Right(playCount)
        case None => Left(Err(ErrorCode.unregisteredUser,
          s"user $userId is not registered", Some(userId)))
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
        Left(Err(ErrorCode.unregisteredUser, "Attempting to decrement playcount for unregeistered user!", Some(userId)))
      case Right(Some(u)) =>
         Right(updateCache(u.copy(
           playCount = PlayCount(u.playCount.current-1)
           )
         ))
      case Left(e)  => Left(e)
    }
  }
}
