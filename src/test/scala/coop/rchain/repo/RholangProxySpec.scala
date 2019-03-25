package coop.rchain.repo

import coop.rchain.utils.Globals
import org.specs2.Specification

class RholangProxySpec extends  Specification {
  def is = s2"""
      repo specs
          listen to name $e1
          fetch by asset by name $e2
    """

  def e1 = {
    val computed = Repo.findByName("Broke.jpg")
    println(s"==== broke.jpg = ${computed}")
    (computed.isRight == true &&
      ! computed.right.get.isEmpty ) === true
  }

  def e2 = {
    val computed = SongRepo.getRSongAsset("Broke.jpg")

    println(s"==== blocks are = ${computed}")
    computed.toOption.isDefined === true
  }
}
