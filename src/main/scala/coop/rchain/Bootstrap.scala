package coop.rchain

import cats.effect._
import cats.implicits._
import org.http4s.server.blaze.BlazeBuilder
import api._
import coop.rchain.domain.Server
import coop.rchain.repo._
import coop.rchain.utils.Globals
import kamon.Kamon
import utils.Globals._
import scala.concurrent.duration.Duration
import kamon.prometheus.PrometheusReporter


object Bootstrap extends IOApp {

  def run(args: List[String]) =
  ServerStream.stream[IO].compile.drain.as(ExitCode.Success)

}
object ServerStream {
  import coop.rchain.api.middleware.MiddleWear._

  val proxy: RNodeProxy = RNodeProxy(Server(Globals.rnodeHost, Globals.rnodePort))
  val redisServer: Server = Server(Globals.redisHost, Globals.redisPort)
  val cachedSongRepo: AssetCache = AssetCache(redisServer, AssetRepo(proxy))
  val cachedUserRepo: UserCache = UserCache(redisServer, UserRepo(proxy))

  def statusApi[F[_]: Effect] = new Status[F].routes
  def userApi[F[_]: Effect] = new UserApi[F](cachedUserRepo).routes
  def songApi[F[_]: Effect] = new SongApi[F](cachedSongRepo, cachedUserRepo).routes

  Kamon.addReporter(new PrometheusReporter())

  def stream[F[_]: ConcurrentEffect] =
    BlazeBuilder[F]
      .withIdleTimeout(Duration.Inf)
      .bindHttp(appCfg.getInt("api.http.port"), "0.0.0.0")
      .mountService(corsHeader(statusApi),s"/public")
      .mountService(corsHeader(statusApi),s"/")
      .mountService(corsHeader(userApi), s"/${apiVersion}/user")
      .mountService(corsHeader(songApi), s"/${apiVersion}")
      .serve
}
