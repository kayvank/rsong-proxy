package coop.rchain.api

import cats.effect._
import com.typesafe.scalalogging.Logger
import org.http4s.circe._
import coop.rchain.protocol.Protocol._
import coop.rchain.repo._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import io.circe.syntax._
import coop.rchain.domain._
import coop.rchain.service.moc.MocSongMetadata
import coop.rchain.service.moc.MocSongMetadata.mocSongs
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SongApi[F[_]: Sync](proxy: RholangProxy) extends Http4sDsl[F] {

  object perPage extends OptionalQueryParamDecoderMatcher[Int]("per_page")

  object page extends OptionalQueryParamDecoderMatcher[Int]("page")

  object userId extends QueryParamDecoderMatcher[String]("userId")

  val songRepo = SongRepo(proxy)
  val userRepo = UserRepo(proxy)
  val log = Logger("SongApi")

  val routes: HttpRoutes[F] =
    HttpRoutes.of[F] {
      case GET -> Root / "song" :? userId(id) +& perPage(pp) +& page(p) =>
        log.debug(s"GET / song request from user: $id")
        Ok(mocSongs.values.toList.asJson)

      case GET -> Root / "song" / id :? userId(uid) =>
        MocSongMetadata.mocSongs.get(id) match {
          case Some(m) =>
            Future { userRepo.incPlayCount(uid) }
            Ok(
              SongResponse(
                m,
                userRepo.fetchPlayCount(id).getOrElse(PlayCount(50))).asJson)
          case None => NotFound(id)
        }

      case GET -> Root / "song" / "music" / id  =>
        log.debug(
          s"GET / song /music /id request from user: for asset: $id")
        val link = RSongCache.getMemoizedAsset(id)(songRepo)
        link.fold(
          l => {
            computeHttpErr(l, id)
          },
          r => {
            Ok(r.binaryData,
               Header("Content-Type", "binary/octet-stream"),
               Header("Accept-Ranges", "bytes"))
          }
        )

      case GET -> Root / "art" / id ⇒
        log.debug(s"GET / art /id request for asset: $id")
        val link = songRepo.getBinaryData(id)
        link.fold(
          l => {
            computeHttpErr(l, id)
          },
          r =>
            Ok(r,
               Header("Content-Type", "binary/octet-stream"),
               Header("Accept-Ranges", "bytes"))
        )
    }
  private def computeHttpErr(e: Err, name: String) = {
    e.code match {
      case ErrorCode.nameToPar =>
        log.error(s"$name not found.  ${e.toString}")
        NotFound(name)
      case _ =>
        log.error(s"Server error for name: $name. ${e.toString}")
        InternalServerError(name)
    }
  }
}
