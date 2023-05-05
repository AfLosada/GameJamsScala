import scala.io.Source
import org.json4s.*
import org.json4s.jackson.JsonMethods.*
import org.json4s.JsonDSL.WithDouble.*
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter.*

val demoJSON =
  """
    |[{
    |    "dtstart": "20230501T170000Z",
    |    "dtend": "20230515T170000Z",
    |    "dtstamp": "20230505T001554Z",
    |    "uid": "uglbnt7end1at48vi768v30mvk@google.com",
    |    "created": "20230503T232217Z",
    |    "description": "https:\/\/itch.io\/jam\/2d-platform-action-game-ss2\n\n2D Side-Scrolling Jam powered by School of Interactive Design and Game Development, College of Creative Design and Entertainment Technology, Dhurakij Pundit University. Come to Join and you make a game or playable prototype with Any Game Engines.\n\nThe online event is open to everyone, from experienced professionals to absolute newbies. :)\n",
    |    "last-modified": "20230503T232217Z",
    |    "location": "",
    |    "sequence": "0",
    |    "status": "CONFIRMED",
    |    "summary": "2D Platform Side Scrolling Action Game SS2",
    |    "transp": "OPAQUE"
    |  }]
    |""".stripMargin

@main def main(): Unit = {
  val gameJamUrl = "http://www.indiegamejams.com/calfeed/index.php"
  val lessThanXDays = 4
  val endDay = Instant.parse("2023-12-31T00:00:00.00Z")

  val gameJamList = fetchGameJams(gameJamUrl)

  val gameJamsOfXLength = gameJamList.filter(p => gameJamDayFilter(p, lessThanXDays))
  val gameJamsOfLengthAndInTimeInterval = gameJamsOfXLength.filter(p => gameJamPeriodFilter(p, Instant.now(), endDay))

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  println(write(gameJamsOfLengthAndInTimeInterval.map(_.classToDto())))
}

def fetchGameJams(url: String): Array[GameJam] = {
  implicit val formats: Formats = DefaultFormats
  val jsonBufferedString = Source.fromURL(url)
  val jsonString = jsonBufferedString.mkString
  val json = parse(jsonString)

  def dtoMapper(dto: GameJamDTO): GameJam = {
    println(dto)
    dto.dtoToClass()
  }

  json.camelizeKeys.extract[Array[GameJamDTO]].map(dtoMapper)
}

def gameJamDayFilter(gameJam: GameJam, lessThanXDays: Int): Boolean = {
  val now = Instant.now()
  val nowInXDays: Instant = now.plusSeconds((60 * 60 * 24) * lessThanXDays)
  val xDayDuration = Duration.between(nowInXDays, now)
  gameJam.duration.compareTo(xDayDuration) <= 0
}

def gameJamPeriodFilter(gameJam: GameJam, startDate: Instant, endDate: Instant): Boolean =
  Instant.from(gameJam.dtstart).isAfter(startDate) && Instant.from(gameJam.dtend).isBefore(endDate)

case class GameJamDTO(dtstart: String,
                      dtend: String,
                      dtstamp: String = "",
                      uid: String = "",
                      created: String = "",
                      description: String,
                      lastModified: String = "",
                      location: String = "",
                      sequence: String = "",
                      status: String = "",
                      summary: String = "",
                      transp: String = ""
                     ) {
  def dtoToClass(): GameJam = new GameJam(
    LocalDate.parse(dtstart),
    LocalDate.parse(dtend),
    dtstamp,
    uid,
    created,
    description,
    lastModified,
    location,
    sequence,
    status,
    summary,
    transp
  )

  override def toString(): String = {
    s"""$uid
      |$dtstart
      |$dtend
      |$description
      |""".stripMargin
  }
}

class GameJam(
               val dtstart: LocalDate,
               val dtend: LocalDate,
               val dtstamp: String,
               val uid: String,
               val created: String,
               val description: String,
               val lastModified: String,
               val location: String,
               val sequence: String,
               val status: String,
               val summary: String,
               val transp: String
             ) {
  val duration: Duration = Duration.between(dtend, dtstart)
  def classToDto(): GameJamDTO = GameJamDTO(
    dtstart.format(ISO_LOCAL_DATE),
    dtend.format(ISO_LOCAL_DATE),
    dtstamp,
    uid,
    created,
    description,
    lastModified,
    location,
    sequence,
    status,
    summary,
    transp
  )
}