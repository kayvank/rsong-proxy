package coop.rchain.repo

import com.typesafe.scalalogging.Logger
import coop.rchain.crypto.codec.Base16
import coop.rchain.domain._

import scala.util._
import java.util.UUID

import coop.rchain.utils.HexUtil

object SongRepo {
  private val SONG_OUT = "SONG-OUT" // UUID.randomUUID.toString

  import Repo._

  val log = Logger[SongRepo.type ]

  val getRSongAsset: String =>  Either[Err, Array[Byte]] =
      assetName =>
    for {
      songId <- findByName(assetName)
      songIdOut = s"${songId}-${SONG_OUT}"
      _ = log.info(s"${songId}-${SONG_OUT}")
      retrieveSongArgs = s"""("$songId".hexToBytes(), "$songIdOut")"""
      termToRetrieveSong = s"""@["Immersion", "retrieveSong"]!$retrieveSongArgs"""
      _ = log.info(s"rholang to retrieve:${termToRetrieveSong}")
      _ <- deployAndPropose(termToRetrieveSong)
      songData <- findByName(songIdOut)
      binarySongData <- HexUtil.hex2bytes(songData)
    } yield binarySongData

}
