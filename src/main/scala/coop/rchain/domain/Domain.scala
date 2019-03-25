package coop.rchain.domain

sealed trait Domain

case class Entity(id: String, data: String) extends Domain

case class PlayList(entity: Entity) extends Domain

object TypeOfAsset {
  val t: Map[String, String] =
    Map("Stereo" -> "Stereo", "3D" -> "3D", "jpg" -> "jpg")
}

case class Interval[T](from: T, to: Option[T])

case class TemporalInterval(
  inMillis: Interval[Long],
  inUtc: Interval[String]
)

case class Cursor(from: Int, to: Int) extends Domain

case class Metadata(k: String, v: String) extends Domain

case class User(id: String,
  name: Option[String],
  active: Boolean,
  lastLogin: Long,
  playCount: Int = 100,
  metadata: Map[String, String]
) extends Domain

case class Server(hostName: String, port: Int)

case class Artwork(id: String, uri: String) extends Domain

case class Artist(
  id: String,
  title: String,
  name: String
) extends Domain

case class Audio(
  effect: String,
  uri: String,
  duration_ms: Long
) extends Domain

case class Song(
  id: String,
  title: String,
  name: String,
  audio: List[Audio],
  language: String
) extends Domain

case class Album(
  id: String,
  title: String,
  name: String,
  artworks: List[Artwork],
  duration_ms: Long,
  artists: List[Artist],
  uri: String
) extends Domain

case class SongMetadata(
  song: Song,
  artists: List[Artist],
  artwork: List[Artwork],
  album: Option[Album] = None
) extends Domain

case class PlayCount(
  current: Int // init to 100
) extends Domain

case class WorldView(
  user: User,
  songMetadata: SongMetadata,
  playCount: PlayCount
) extends Domain

case class DeployAndProposeResponse(fromDeploy: String, fromPropose: String)

case class RSongJsonAsset(
  id: String,
  assetData: String,
  jsonData: String
)

case class CachingException(message: String) extends Exception(message)

object NameKey extends Enumeration {
  type NameKey = Value
  val newUserId, store, playCount, retrieveSong, retrieveMetadata, remunerate,
  play = Value
}

case class CachedUser(
  rsongUserId: String,
  playCount: PlayCount,
  isSyncWithRChain: Boolean = false,
  rChainName: Option[String] = None
) extends Domain

case class CachedAsset(
  name: String,
  data: Array[Byte],
  isSyncWithRChain: Boolean = false,
  rChainName: Option[String] = None
) extends Domain

