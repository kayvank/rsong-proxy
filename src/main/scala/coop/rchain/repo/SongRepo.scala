package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.crypto.codec.Base16
import coop.rchain.domain._

import scala.util._
import java.util.UUID

import coop.rchain.utils.HexUtil

trait SongRepo {
  def getAsset(assetName: String): Either[Err, Array[Byte]]
}

object SongRepo {
  def apply(repo: Repo) = new SongRepo {
    val log = Logger[SongRepo]
    private val _TAG_ = "_TAG_" //

    def getAsset(assetName: String): Either[Err, Array[Byte]] =
      for {
        songId <- repo.findByName(assetName)
        songIdOut = s"${songId}${_TAG_}"
        termToRetrieveSong =
        s"""@["Immersion", "retrieveSong"] ! ("$songId".hexToBytes(), "$songIdOut")"""
        d <- repo.deployAndPropose(termToRetrieveSong)
        _ = log.info(s" deploy propose results = ${d}")
        songData <- repo.findByName(songIdOut)
        binarySongData <- HexUtil.hex2bytes(songData)
      } yield binarySongData

  }

}


