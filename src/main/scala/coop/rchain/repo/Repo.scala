package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.models.Par
import coop.rchain.domain._
import coop.rchain.utils.Globals._

object Repo {

  val log = Logger("Repo")
  val (host, port) = (appCfg.getString("grpc.host"),
    appCfg.getInt("grpc.ports.external"))

  val proxy = RholangProxy(host, port)
  println(s""""
              ----------------------------------------------------
             GRPC server:   $host:$port
             rsongHostUrl:  $rsongHostUrl"
             redis_url:     ${appCfg.getString("redis.url")}
              ----------------------------------------------------
  """)

 def deployAndPropose(queryTerm:String):Either[Err, DeployAndProposeResponse] =
   proxy.deployAndPropose(queryTerm)

  def findByName( name: String): Either[Err, String] =  findByName(proxy, name)

  private  def findByName(proxy: RholangProxy, name: String): Either[Err, String] =
    proxy.dataAtName( s""""$name"""")


}
