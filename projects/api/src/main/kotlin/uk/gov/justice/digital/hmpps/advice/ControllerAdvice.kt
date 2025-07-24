package uk.gov.justice.digital.hmpps.advice

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.model.ErrorResponse

@RestControllerAdvice(basePackages = ["uk.gov.justice.digital.hmpps"])
class ControllerAdvice {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(e: MethodArgumentNotValidException) = ResponseEntity
        .badRequest()
        .body(
            ErrorResponse(
                status = BAD_REQUEST.value(),
                message = "Validation failure",
                fields = e.bindingResult.fieldErrors.map { ErrorResponse.Field(it.code, it.defaultMessage, it.field) }
            )
        )

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleInvalidRequest(e: ConstraintViolationException) = ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(status = BAD_REQUEST.value(), message = e.message))

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(e: AccessDeniedException) = ResponseEntity
        .status(FORBIDDEN)
        .body(ErrorResponse(status = FORBIDDEN.value(), message = e.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException) = ResponseEntity
        .status(BAD_REQUEST)
        .body(ErrorResponse(status = BAD_REQUEST.value(), message = e.message))
}
