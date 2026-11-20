package ru.nto.team

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

object RequestValidation {
    private val codePattern = Regex("[A-Za-z0-9]{4}")
    val phonePattern: Regex = Regex("\\+7\\d{10}")
    private val apiDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss").withResolverStyle(ResolverStyle.STRICT)

    fun requireValidCodeFormat(code: String) {
        if (!codePattern.matches(code)) throw ApiException(400, "Invalid code format")
    }

    fun parseStatusFilter(raw: String?): RequestStatus? {
        if (raw == null) return null
        return try {
            RequestStatus.valueOf(raw)
        } catch (e: IllegalArgumentException) {
            throw ApiException(400, "Invalid status")
        }
    }

    fun parseDateTime(raw: String): LocalDateTime? =
        try {
            LocalDateTime.parse(raw, apiDateTime)
        } catch (e: Exception) {
            null
        }
}
