package coop.rchain.repo

import coop.rchain.casper.protocol._
import coop.rchain.domain.{Err, OpCode}
import com.google.protobuf.empty._
import coop.rchain.models.{Expr, Par}
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import coop.rchain.domain._
import com.typesafe.scalalogging.Logger
import coop.rchain.domain.OpCode.OpCode

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

  implicit class EitherOps(val resp: coop.rchain.either.Either)  {
    def asEither: OpCode => Either[Err, String] = opcode =>
      resp match {
        case coop.rchain.either.Either(content) if content.isError =>
          Left(Err(
            opcode,
            content.error.map(x => x.messages.toString).getOrElse("No error message given!") ))
        case coop.rchain.either.Either(content) if content.isEmpty =>
          Left(Err(opcode, "No value was returned!"))
        case coop.rchain.either.Either(content) if content.isSuccess =>
          Right(content.success.head.getResponse.value.toStringUtf8)
      }
  }

}

class RholangProxy(channel: ManagedChannel) {

import RholangProxy._

  private lazy val grpc = DeployServiceGrpc.blockingStub(channel)
  private lazy val log = Logger[RholangProxy]

  def shutdown = channel.shutdownNow()

  def deploy(contract: String): Either[Err, String] =
    grpc.doDeploy(
      DeployData()
        .withTerm(contract)
        .withTimestamp(System.currentTimeMillis())
        .withPhloLimit(Long.MaxValue)
        .withPhloPrice(1L)
    ).asEither(OpCode.grpcDeploy)

  def showBlocks: List[coop.rchain.either.Either] = grpc.showBlocks(BlocksQuery(
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

  def proposeBlock: Either[Err, String] = grpc.createBlock(Empty()).asEither(OpCode.grpcDeploy)


  import coop.rchain.protocol.ParOps._
  def dataAtName(
      rholangName: String): Either[Err, String] = {
    log.info(s"dataAtName received name $rholangName")
    rholangName.asPar.flatMap(p => dataAtName(p))
  }

  private def dataAtName(par: Par): Either[Err, String] = {
    log.info(s"dataAtName received par ${par}")
    val res =
      grpc.listenForDataAtName(DataAtNameQuery(Int.MaxValue, Some(par))) //.asEither(OpCode.listenAtNam
    val ret =
    res.content match {
      case (s) if s.isError => Left(Err(OpCode.listenAtName, s.error.get.messages.toString))
      case (s) if  s.isEmpty => Left(Err(OpCode.listenAtName, "listenAtName returned no results!"))
      case (s) if  s.isSuccess => Right( s.success.map( x => x.toProtoString).getOrElse("") )
    }
    log.info(s"++ dataAtName results are : ${ret}")
    ret
  }

}
