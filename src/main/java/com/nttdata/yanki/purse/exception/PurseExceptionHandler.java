package com.nttdata.yanki.purse.exception;

import com.nttdata.yanki.purse.model.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PurseExceptionHandler {

    @ExceptionHandler(PurseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCreditNotFoundException(PurseNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Purse Not Found");
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidPurseDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCreditDataException(InvalidPurseDataException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Invalid Purse Data");
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Internal Server Error");
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(PaymentDataException.class)
    public ResponseEntity<ErrorResponse> handlePaymentDataException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError("Invalid Payment Data");
        errorResponse.setMessage(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}