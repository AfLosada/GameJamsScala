package Model

import java.time.{Duration, LocalDate, LocalDateTime}
import java.time.format.{DateTimeFormatter, DateTimeParseException}


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