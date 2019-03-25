package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._

trait Repo {
  def deployAndPropose(query: String): Either[Err, DeployAndProposeResponse]

  def findByName(name: String): Either[Err, String]
}

object Repo {

  def apply(proxy: RNodeProxy): Repo = new Repo {
    val log = Logger("Repo")

    def deployAndPropose(query: String): Either[Err, DeployAndProposeResponse] =
      proxy.deployAndPropose(query)

    def findByName(name: String): Either[Err, String] = findByName(proxy, name)

    private def findByName(proxy: RNodeProxy, name: String): Either[Err, String] =
      proxy.dataAtName( s""""$name"""")

  }
}
