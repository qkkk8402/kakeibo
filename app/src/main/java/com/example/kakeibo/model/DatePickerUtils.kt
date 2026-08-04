package com.example.kakeibo.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material 3 DatePickerが要求するUTC基準のミリ秒へ変換する。 */
fun localDateToDatePickerMillis(date: LocalDate): Long =
    date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** DatePickerのUTC基準ミリ秒をカレンダー上の日付へ戻す。 */
fun datePickerMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
