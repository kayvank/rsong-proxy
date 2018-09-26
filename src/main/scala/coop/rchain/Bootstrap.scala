package coop.rchain

import cats.effect._
import cats.implicits._
import org.http4s.server.blaze.BlazeBuilder
import api._
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

  def statusApi[F[_]: Effect] = new Status[F].routes
  def userApi[F[_]: Effect] = new UserApi[F].routes
  def songApi[F[_]: Effect] = new SongApi[F](proxy).routes

  Kamon.addReporter(new PrometheusReporter())

  def stream[F[_]: ConcurrentEffect] =
    BlazeBuilder[F]
      .withIdleTimeout(Duration.Inf)
      .bindHttp(appCfg.getInt("api.http.port"), "0.0.0.0")
      .mountService(corsHeader(statusApi))
      .mountService(corsHeader(statusApi), s"/${apiVersion}/public")
      .mountService(corsHeader(userApi), s"/${apiVersion}/user")
      .mountService(corsHeader(songApi), s"/${apiVersion}")
      .serve
}
