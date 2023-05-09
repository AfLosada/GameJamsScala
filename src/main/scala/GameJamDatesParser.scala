import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s.*
import org.json4s.JsonDSL.WithDouble.*
import org.json4s.jackson.JsonMethods.*
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter.*
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, DateTimeParseException}
import java.time.temporal.ChronoField
import java.util.Date
import scala.annotation.targetName
import scala.io.Source

val dateFormatterBuilder = new DateTimeFormatterBuilder()

@main def main(): Unit = {
  val optionalFormats = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") ::
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss") ::
    DateTimeFormatter.ofPattern("yyyyMMdd") ::
    Nil
  optionalFormats.foreach(format => dateFormatterBuilder.appendOptional(format))
  dateFormatterBuilder
    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)

  val formatter = dateFormatterBuilder.toFormatter

  val gameJamUrl = "http://www.indiegamejams.com/calfeed/index.php"
  val lessThanXDays = 4
  val parsedEndDay = LocalDateTime.parse("20231231T170000Z", formatter)
  val endDay = parsedEndDay.toInstant(ZoneOffset.UTC)

  val gameJamList = fetchGameJams(url = gameJamUrl, formatter = formatter)

  val gameJamsOfXLength = gameJamList.filter(p => gameJamDayFilter(p, lessThanXDays))
  val gameJamsOfLengthAndInTimeInterval = gameJamsOfXLength.filter(p => gameJamPeriodFilter(p, Instant.now(), endDay))

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  println(write(gameJamsOfLengthAndInTimeInterval.map(_.classToDto())))
}

def fetchGameJams(url: String, formatter: DateTimeFormatter): Array[GameJam] = {
  implicit val formats: Formats = DefaultFormats
  val jsonBufferedString = Source.fromURL(url)
  val jsonString = jsonBufferedString.mkString
  val json = parse(replaceDtstart(jsonString))

  def dtoMapper(dto: GameJamDTO): GameJam = {
    println(dto)
    dto.dtoToClass(formatter)
  }

  json.camelizeKeys.extract[Array[GameJamDTO]].map(dtoMapper)
}

def replaceDtstart(jsonString: String): String = {
  val json = parse(jsonString)
  val updatedJson = json.transformField {
    case JField(key, value) if key.contains("dtstart") => ("dtstart", value)
    case JField(key, value) if key.contains("dtend") => ("dtend", value)
    case other => other
  }
  compact(render(updatedJson))
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
                      transp: String = "",
                     ) {
  def dtoToClass(formatter: DateTimeFormatter): GameJam = {
    val newDtStart =
      try LocalDateTime.parse(dtstart, formatter)
      catch case
        _: DateTimeParseException => LocalDate.parse(dtstart, formatter).atStartOfDay()

    val newDtEnd =
      try LocalDateTime.parse(dtend, formatter)
      catch case
        _: DateTimeParseException => LocalDate.parse(dtend, formatter).atStartOfDay()
    new GameJam(
      newDtStart,
      newDtEnd,
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

  override def toString: String = {
    s"""$uid
       |$dtstart
       |$dtend
       |$description
       |""".stripMargin
  }
}

class GameJam(
               val dtstart: LocalDateTime,
               val dtend: LocalDateTime,
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
    dtstart.toString,
    dtend.toString,
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