package com.example.spring_boot_notes

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalValidationHandler {

    /*
    This function will now be called for every single MethodArgumentNotValidException that is thrown
    in some kind of endpoint and some code where client makes a request. This is type of validation that
    spring validation framework throws, with GlobalValidationHandler we tell this how we want to handle the exceptions across our codebase
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(e: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = e.bindingResult.allErrors.map {
            it.defaultMessage ?: "Invalid value"
        }
        return ResponseEntity
            .status(400)
            .body(mapOf("errors" to errors))
    }
}