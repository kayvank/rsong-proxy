package coop.rchain.repo

import coop.rchain.domain.Server
import coop.rchain.utils.Globals
import org.specs2.Specification

class RNodeProxySpec extends  Specification {
  def is = s2"""
      repo specs
          listen-for-name $e1
          fetch by asset by name $e2
    """

  val proxy: RNodeProxy = RNodeProxy(Server(Globals.rnodeHost, Globals.rnodePort))
  val repo: Repo = Repo(proxy)

  def e1 = {
    val computed = proxy.dataAtName(""""Broke.jpg"""")

    println(s"==== broke.jpg = ${computed}")
    (computed.isRight == true &&
      ! computed.right.get.isEmpty ) === true
  }

  def e2 = {
    val computed = AssetRepo(proxy).getAsset("Broke.jpg")

    println(s"==== blocks are = ${computed}")
    computed.toOption.isDefined === true
  }
}
