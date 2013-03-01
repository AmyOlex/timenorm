package info.bethard.timenorm

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeBuilder
import org.threeten.bp.temporal.ChronoField._
import org.threeten.bp.temporal.ChronoUnit._
import org.threeten.bp.temporal.Temporal
import org.threeten.bp.temporal.TemporalAccessor
import org.threeten.bp.temporal.TemporalField
import org.threeten.bp.temporal.TemporalUnit
import org.threeten.bp.temporal.ValueRange

object TimeSpan {
  def startingAt(start: ZonedDateTime, period: Period, modifier: Modifier): TimeSpan = {
    TimeSpan(start, period.addTo(start), period, modifier)
  }

  def endingAt(end: ZonedDateTime, period: Period, modifier: Modifier): TimeSpan = {
    TimeSpan(period.subtractFrom(end), end, period, modifier)
  }

  def truncate(time: ZonedDateTime, unit: TemporalUnit): ZonedDateTime = {
    this.unitToFieldsToTruncate(unit).foldLeft(time) {
      case (time, field) => time.`with`(field, field.range.getMinimum)
    }
  }

  private val fieldFormats = Map[TemporalField, String](
    DECADE -> "%03d",
    YEAR -> "%04d",
    MONTH_OF_YEAR -> "-%02d",
    DAY_OF_MONTH -> "-%02d",
    ALIGNED_WEEK_OF_YEAR -> "-W%02d",
    HOUR_OF_DAY -> "T%02d",
    MINUTE_OF_HOUR -> ":%02d",
    SECOND_OF_MINUTE -> ":%02d")

  private val unitToFieldsToDisplay = Map[TemporalUnit, Seq[TemporalField]](
    DECADES -> Seq(DECADE),
    YEARS -> Seq(YEAR),
    MONTHS -> Seq(YEAR, MONTH_OF_YEAR),
    WEEKS -> Seq(YEAR, ALIGNED_WEEK_OF_YEAR),
    DAYS -> Seq(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH),
    HOURS -> Seq(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY),
    MINUTES -> Seq(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR),
    SECONDS -> Seq(YEAR, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE))
  
  private val unitToFieldsToTruncate = Map[TemporalUnit, Seq[TemporalField]](
    DECADES -> Seq(YEAR_OF_DECADE, MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    YEARS -> Seq(MONTH_OF_YEAR, DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    MONTHS -> Seq(DAY_OF_MONTH, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    WEEKS -> Seq(DAY_OF_WEEK, HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    DAYS -> Seq(HOUR_OF_DAY, MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    HOURS -> Seq(MINUTE_OF_HOUR, SECOND_OF_MINUTE),
    MINUTES -> Seq(SECOND_OF_MINUTE),
    SECONDS -> Seq())
}

case class TimeSpan(
    start: ZonedDateTime,
    end: ZonedDateTime,
    period: Period,
    modifier: Modifier) {

  def timeMLValueOption: Option[String] = {
    if (this.start == this.end) {
      Some(this.start.getDateTime.toString)
    } else {
      this.period.unitAmounts.toList match {
        case List((unit, 1)) if TimeSpan.truncate(this.start, unit) == this.start =>
          val parts =
            for (field <- TimeSpan.unitToFieldsToDisplay(unit))
              yield TimeSpan.fieldFormats(field).format(this.start.get(field))
          Some(parts.mkString)
        case _ => None
      }
    }
  }
}
