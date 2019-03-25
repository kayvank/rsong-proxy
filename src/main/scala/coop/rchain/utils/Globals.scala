package coop.rchain.utils

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.Logger
import coop.rchain.repo.RNodeProxy

object Globals {
  private val log = Logger[Globals.type ]
  val cfg: Config = ConfigFactory.load
  val appCfg: Config = cfg.getConfig("coop.rchain.rsong")
  val apiVersion = appCfg.getString("api.version")
  val artpath = s"$apiVersion/art"
  val songpath = s"$apiVersion/song/music"
  val rsongHostUrl: String = appCfg.getString("my.host.url")
  val (rnodeHost, rnodePort) = (appCfg.getString("grpc.host"),
    appCfg.getInt("grpc.ports.external"))
  val (redisHost, redisPort) = (appCfg.getString("redis.host"),
    appCfg.getInt("redis.port"))

  log.info(s""""
              ----------------------------------------------------
             rnode server:   $rnodeHost:$rnodePort
             rsongHostUrl:  $rsongHostUrl"
             redis_url:     $redisHost:$redisPort
              ----------------------------------------------------
  """)

}
