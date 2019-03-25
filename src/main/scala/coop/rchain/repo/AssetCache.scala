package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import scalacache._
import scalacache.redis._
import scalacache.serialization.binary._
import scalacache.memoization._
import scala.util._
import scalacache.modes.try_._
import coop.rchain.utils.ErrImplicits._

trait AssetCache {
  val getMemoized: String => Either[Err, CachedAsset]
}

object AssetCache {
  val log = Logger[AssetCache.type]

  def apply(redisServer: Server, repo: SongRepo) =
    new AssetCache {
      val binaryAsset: String => Either[Err, Array[Byte]] = name =>
        repo.getAsset(name)

      implicit val __cache: Cache[CachedAsset] =
        RedisCache(redisServer.hostName, redisServer.port)

      __cache.config
      val getMemoized: String => Either[Err, CachedAsset] =
        name => {
          def __getMemoized(name: String): Try[CachedAsset] =
            memoize[Try, CachedAsset](None) {
              repo.getAsset(name).map(CachedAsset(name, _)) match {
                case Right(s) =>
                  log.debug(s"Found asset. ${s}")
                  s
                case Left(e) =>
                  log.error(s"Exception in RSongCache layer. ${e}")
                  throw CachingException(e.msg)
              }
            }

          log.info(s"in memoized, attempting to fetch $name")
          __getMemoized(name).asErr
        }
    }
}
