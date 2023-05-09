package Model

import Model.*

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{LocalDate, LocalDateTime}

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
    val newDtStart = {
      try LocalDateTime.parse(dtstart, formatter)
      catch case
        _: DateTimeParseException => LocalDate.parse(dtstart, formatter).atStartOfDay()
    }
    val newDtEnd: LocalDateTime = {
      try LocalDateTime.parse(dtend, formatter)
      catch case
        _: DateTimeParseException => LocalDate.parse(dtend, formatter).atStartOfDay()
    }
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
