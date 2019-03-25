package coop.rchain.api

import cats.effect._
import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import coop.rchain.repo._
import org.http4s.HttpRoutes
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import io.circe.syntax._
import kamon.Kamon

class UserApi[F[_]: Sync](repo: UserCache) extends Http4sDsl[F] {

  val log = Logger("UserApi")
  val routes: HttpRoutes[F] = HttpRoutes.of[F] {

    case GET -> Root / userId =>
      repo.getOrCreateUser(userId).fold(
        l => {
          computeHttpErr(l, userId, s"user")
        },
          r => {
            Kamon.counter(s"200 - get /user")
            Ok(
              User(id = userId,
                name = None,
                active = true,
                lastLogin = System.currentTimeMillis,
                playCount = r.playCount.current,
                metadata = Map("immersionUser" -> "ImmersionUser")).asJson)
          })
    case GET -> Root / id / "playcount" =>
        repo.getOrCreateUser(id)
        .fold(
          e =>
            computeHttpErr(e, id, s"get /user/playcount"),
          r => {
            Kamon.counter(s"200 - get /user/playcount")
            Ok(r.playCount.asJson)
          }
        )
  }

  private def computeHttpErr(e: Err, name: String, route: String) = {
    e.code match {
      case OpCode.nameToPar =>
        log.error(s"${e} name: ${name}, route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case OpCode.nameNotFound =>
        log.error(s"${e} name: ${name} , route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case OpCode.unregisteredUser =>
        log.error(s"${e} name: ${name} , route: $route")
        Kamon.counter(s"404 - ${route}")
        NotFound(name)
      case _ =>
        log.error(s"Server error for name: $name. ${e.toString}  route: $route")
        Kamon.counter(s"500 - ${route}")
        InternalServerError(name)
    }
  }
}
