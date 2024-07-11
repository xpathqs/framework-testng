package org.xpathqs.framework.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class DateType {
    DAY, WEEK, MONTH, YEAR
}

class DateTypeValue(
    val value: Int,
    val type: DateType
)

object DateTimeUtil {
    private var dateFormatter = DateTimeFormatter.ISO_DATE!!

    fun updateDateFormatter(formatter: DateTimeFormatter) {
        dateFormatter = formatter
    }

    fun LocalDateTime.toStringValue(): String {
        return this.format(dateFormatter)
    }

    fun LocalDate.toStringValue(): String {
        return this.format(dateFormatter)
    }

    val NOW: LocalDateTime
        get() {
            return LocalDateTime.now()
        }

    operator fun LocalDateTime.plus(arg: DateTypeValue): LocalDateTime {
        return when(arg.type) {
            DateType.DAY -> this.plusDays(arg.value.toLong())
            DateType.WEEK -> this.plusWeeks(arg.value.toLong())
            DateType.MONTH -> this.plusMonths(arg.value.toLong())
            DateType.YEAR -> this.plusYears(arg.value.toLong())
        }
    }

    operator fun LocalDateTime.minus(arg: DateTypeValue): LocalDateTime {
        return when(arg.type) {
            DateType.DAY -> this.minusDays(arg.value.toLong())
            DateType.WEEK -> this.minusWeeks(arg.value.toLong())
            DateType.MONTH -> this.minusMonths(arg.value.toLong())
            DateType.YEAR -> this.minusYears(arg.value.toLong())
        }
    }

    val Int.day: DateTypeValue
        get() = DateTypeValue(this, DateType.DAY)

    val Int.week: DateTypeValue
        get() = DateTypeValue(this, DateType.WEEK)

    val Int.month: DateTypeValue
        get() = DateTypeValue(this, DateType.MONTH)

    val Int.year: DateTypeValue
        get() = DateTypeValue(this, DateType.MONTH)
}