package coop.rchain.repo

import coop.rchain.casper.protocol._
import coop.rchain.domain.{Err, ErrorCode}
import com.google.protobuf.empty._
import coop.rchain.models.{Expr, Par}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import coop.rchain.domain._
import com.typesafe.scalalogging.Logger

import scala.util._

object RholangProxy {

  val MAXGRPCSIZE = 1024 * 1024 * 1024 *5// 10G for a song+metadat

  def apply(host: String, port: Int): RholangProxy = {

    val channel =
      ManagedChannelBuilder
        .forAddress(host, port)
        .maxInboundMessageSize(MAXGRPCSIZE)
        .usePlaintext.build
    new RholangProxy(channel)
  }

}

class RholangProxy(channel: ManagedChannel) {

  private lazy val grpc = DeployServiceGrpc.blockingStub(channel)
  private lazy val log = Logger[RholangProxy]

  def shutdown = channel.shutdownNow()

  def deploy(contract: String): Either[Err, String] = {
    val resp = grpc.doDeploy(
      DeployData()
        .withTerm(contract)
        .withTimestamp(System.currentTimeMillis())
        .withPhloLimit(Int.MaxValue)
        .withPhloPrice(1)
        .withNonce(0)
        .withFrom("0x1")
    )
    if (resp.success)
      Right(resp.message)
    else Left(Err(ErrorCode.grpcDeploy, resp.message, Some(contract)))
  }

  def showBlocks: List[BlockInfoWithoutTuplespace] = grpc.showBlocks(BlocksQuery(
    Int.MaxValue)).toList

  def deployNoPropose(
      contract: String): Either[Err, DeployAndProposeResponse] = {
    for {
      d <- deploy(contract)
    } yield DeployAndProposeResponse(d, "")
  }

  def deployAndPropose(
      contract: String): Either[Err, DeployAndProposeResponse] = {
    for {
      d <- deploy(contract)
      _ = log.debug(s"Proposing contract $contract")
      p <- proposeBlock
    } yield DeployAndProposeResponse(d, p)
  }

  private def proposeBlock: Either[Err, String] = {
    val response: DeployServiceResponse = grpc.createBlock(Empty())
    if (response.success) {
      Right(response.message)
    } else {
      Left(Err(ErrorCode.grpcPropose, response.message, None))
    }
  }

  import coop.rchain.protocol.ParOps._
  def dataAtName(
      rholangName: String): Either[Err, ListeningNameDataResponse] = {
    log.info(s"dataAtName received name $rholangName")
    rholangName.asPar.flatMap(p => dataAtName(p))
  }

  private def dataAtName(par: Par): Either[Err, ListeningNameDataResponse] = {
    log.info(s"dataAtName received par ${par}")
    val res = grpc.listenForDataAtName(DataAtNameQuery(Int.MaxValue, Some(par)))
    res.status match {
      case "Success" if res.blockResults.headOption.isDefined =>
        Right(res)
      case "Success" if res.blockResults.headOption.isEmpty =>
        Left(Err(ErrorCode.emptyVectorReturned, s"${res}", None))
      case _ =>
        Left(Err(ErrorCode.nameNotFound, s"${res}", None))
    }
  }

}
