package coop.rchain.utils

import coop.rchain.crypto.codec.Base16
import coop.rchain.domain.{Err, OpCode}

object HexUtil {

  def hex2bytes(hexString: String): Either[Err, Array[Byte]] =
    Base16.decode(hexString) match {
      case Some(e) => Right(e)
      case None => Left(Err(OpCode.rsongHexConversion, "Base16.decoder returned None"))
    }

  def bytes2hex(bytes: Array[Byte], sep: Option[String]): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _    => bytes.map("%02x".format(_)).mkString(sep.get)
    }
  }

  def bytes2hex(bytes: Array[Byte]): String =
    bytes.map("%02x".format(_)).mkString

  def chunk(buf: String): List[(String, Int)] =
    buf.grouped(1 + buf.size / 50000).zipWithIndex.toList
}
