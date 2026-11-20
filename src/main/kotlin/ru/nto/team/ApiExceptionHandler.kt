package ru.nto.team

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

class ApiException(val status: Int, val body: Map<String, Any>) : RuntimeException(body.toString()) {
    constructor(status: Int, error: String) : this(status, mapOf("error" to error))
}

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(ApiException::class)
    fun api(error: ApiException): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(error.status).body(error.body)
}
