package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.domain._
import scala.util._

import coop.rchain.utils.HexUtil

trait AssetRepo {
  def getAsset(assetName: String): Either[Err, Array[Byte]]
}

object AssetRepo {
  def apply(proxy: RNodeProxy) = new AssetRepo {
    val log = Logger[AssetRepo]
    private val _TAG_ = "_TAG_" //

    def getAsset(assetName: String): Either[Err, Array[Byte]] =
      for {
        songId <- proxy.dataAtName(s""""$assetName"""")
        termToRetrieveSong =
        s"""@["Immersion", "retrieveSong"] ! ("$songId".hexToBytes(), "${songId}${_TAG_}")"""
        d <- proxy.deployAndPropose(termToRetrieveSong)
        _ = log.info(s" deploy propose results = ${d}")
        songData <- proxy.dataAtName(s""""${songId}${_TAG_}"""")
        binarySongData <- HexUtil.hex2bytes(songData)
      } yield binarySongData

  }

}


