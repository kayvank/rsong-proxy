package coop.rchain.repo

import coop.rchain.casper.protocol._
import coop.rchain.domain.{Err, OpCode}
import com.google.protobuf.empty._
import io.grpc.{ManagedChannel, ManagedChannelBuilder}
import coop.rchain.domain._
import com.typesafe.scalalogging.Logger
import coop.rchain.domain.OpCode.OpCode
import coop.rchain.either.{Either => CE}
import coop.rchain.protocol.ParUtil._
import coop.rchain.models.either.EitherHelper._
import coop.rchain.rholang.interpreter.{PrettyPrinter}

import scala.util._

object RNodeProxy {


  val MAXGRPCSIZE = 1024 * 1024 * 1024 *5// 10G for a song+metadat

  def apply(rnode: Server): RNodeProxy = {

    val channel =
      ManagedChannelBuilder
        .forAddress(rnode.hostName, rnode.port)
        .maxInboundMessageSize(MAXGRPCSIZE)
        .usePlaintext.build
    new RNodeProxy(channel)
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

class RNodeProxy(channel: ManagedChannel) {

import RNodeProxy._

  private lazy val grpc = DeployServiceGrpc.blockingStub(channel)
  private lazy val log = Logger[RNodeProxy]

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
      _ = log.info(s"Proposing contract $contract")
      p <- proposeBlock
    } yield DeployAndProposeResponse(d, p)
  }

  def proposeBlock: Either[Err, String] = grpc.createBlock(Empty()).asEither(OpCode.grpcDeploy)

  def dataAtName(name: String): Either[Err, String] = {
    name.asPar.flatMap( par =>{
      log.info(s"dataAtName received par ${par}")
      val res: CE = grpc.listenForDataAtName(DataAtNameQuery(Int.MaxValue, Some(par)))
      toEither[ListeningNameDataResponse](res)  match {
        case e if e.isRight =>
           val r =  for {
              x <-  e.right.get.blockResults
              y <- x.postBlockData
              z = PrettyPrinter().buildString(y)
            } yield(z)
          r.headOption match {
            case Some(s) => Right(s)
            case None => Left(Err(OpCode.nameNotFound, s"$name was not found"))
          }

        case e if e.isLeft => Left( Err(OpCode.listenAtName, e.left.get.toString) )
      }
  })}

}
