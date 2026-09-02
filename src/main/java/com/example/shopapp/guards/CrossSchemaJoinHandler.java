package com.example.shopapp.guards;

import com.example.shopapp.guards.AssertQueriesDontJoinSchemas.CrossSchemaJoinException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Answers a rejected cross-schema query with one WARN line and a short 500, instead of
 * letting Tomcat print an 80-line stack trace. The guard's verdict is the point; the
 * trace is not.
 */
@RestControllerAdvice
@Slf4j
class CrossSchemaJoinHandler {

    @ExceptionHandler(CrossSchemaJoinException.class)
    ResponseEntity<Map<String, Object>> onCrossSchemaJoin(CrossSchemaJoinException e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "status", 500,
                        "error", "cross-schema join rejected",
                        "schemas", e.schemas()));
    }
}
