package com.documenttimestamp.api;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        HttpStatus status = resolve(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        return ResponseEntity.status(status).body(ApiExceptionHandler.payload(status, message));
    }

    private HttpStatus resolve(Object statusCode) {
        if (statusCode instanceof Integer) {
            HttpStatus status = HttpStatus.resolve((Integer) statusCode);
            if (status != null) {
                return status;
            }
        }
        return HttpStatus.NOT_FOUND;
    }
}
