import Model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s.*
import org.json4s.JsonDSL.WithDouble.*
import org.json4s.jackson.JsonMethods.*
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

import java.io.{File, PrintWriter}
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter.*
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder, DateTimeParseException}
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters.lastDayOfYear
import java.util.Date
import scala.annotation.targetName
import scala.io.Source

val dateFormatterBuilder = new DateTimeFormatterBuilder()

@main def main(url: String, lessThanDays: String, beforeDate: String, other: String*): Unit = {

  val optionalFormats = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'") :: DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss") :: DateTimeFormatter.ofPattern("yyyyMMdd") :: Nil
  optionalFormats.foreach(format => dateFormatterBuilder.appendOptional(format))
  dateFormatterBuilder.parseDefaulting(ChronoField.HOUR_OF_DAY, 0).parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0).parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
  val formatter = dateFormatterBuilder.toFormatter


  val gameJamUrl = if (url.nonEmpty) url else "http://www.indiegamejams.com/calfeed/index.php"
  val lessThanXDays = if (lessThanDays.nonEmpty) lessThanDays.toInt else 4
  val endDate = if (beforeDate.nonEmpty) beforeDate else formatter.format(LocalDate.now().`with`(lastDayOfYear))
  val parsedEndDay = LocalDateTime.parse(endDate, formatter)
  val endDay = parsedEndDay.toInstant(ZoneOffset.UTC)

  val gameJamList = fetchGameJams(url = gameJamUrl, formatter = formatter)

  val gameJamsOfXLength = gameJamList.filter(p => gameJamDayFilter(p, lessThanXDays))
  val gameJamsOfLengthAndInTimeInterval = gameJamsOfXLength.filter(p => gameJamPeriodFilter(p, Instant.now(), endDay))

  implicit val formats: Formats = Serialization.formats(NoTypeHints)
  val file = new File("gameJams.json")
  val pw = new PrintWriter(file)
  pw.write(write(gameJamsOfLengthAndInTimeInterval.map(_.classToDto())))
  pw.close()
}

def fetchGameJams(url: String, formatter: DateTimeFormatter): Array[GameJam] = {
  implicit val formats: Formats = DefaultFormats
  val jsonBufferedString = Source.fromURL(url)
  val jsonString = jsonBufferedString.mkString
  val json = parse(replaceDtstart(jsonString))

  def dtoMapper(dto: GameJamDTO): GameJam = {
    dto.dtoToClass(formatter)
  }

  json.camelizeKeys.extract[Array[GameJamDTO]].map(dtoMapper)
}

def replaceDtstart(jsonString: String): String = {
  val json = parse(jsonString)
  val updatedJson = json.transformField { case JField(key, value) if key.contains("dtstart") => ("dtstart", value)
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

def gameJamPeriodFilter(gameJam: GameJam, startDate: Instant, endDate: Instant): Boolean = gameJam.dtstart.toInstant(ZoneOffset.UTC).isAfter(startDate) && gameJam.dtend.toInstant(ZoneOffset.UTC).isBefore(endDate)

