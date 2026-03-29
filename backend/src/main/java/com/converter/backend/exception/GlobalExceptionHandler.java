package com.converter.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler – production-safe.
 * <p>
 * Rules:
 *  - NO stacktrace, SQL detail or internal class names are ever returned to clients.
 *  - In dev/test profiles the {@code detail} field adds extra context to ease debugging.
 *  - Every exception is logged server-side with full detail so nothing is lost.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Environment environment;

    public GlobalExceptionHandler(Environment environment) {
        this.environment = environment;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isDevelopment() {
        return environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev", "test"));
    }

    private Map<String, Object> buildBody(int status, String error, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        return body;
    }

    private Map<String, Object> buildBodyWithDetail(int status, String error, String message, String detail) {
        Map<String, Object> body = buildBody(status, error, message);
        if (isDevelopment() && detail != null) {
            body.put("detail", detail);
        }
        return body;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Domain / Custom Exceptions
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        Map<String, Object> body = buildBodyWithDetail(
                HttpStatus.NOT_FOUND.value(), "Not Found",
                "The requested resource was not found.",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        log.warn("Email already exists: {}", ex.getMessage());
        Map<String, Object> body = buildBody(
                HttpStatus.CONFLICT.value(), "Conflict",
                "This email address is already registered. Please use a different email or sign in.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleLimitExceeded(LimitExceededException ex) {
        log.warn("User limit exceeded: {} | Type: {} | Usage: {}/{}", 
            ex.getMessage(), ex.getLimitType(), ex.getCurrentUsage(), ex.getLimitValue());
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", java.time.Instant.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "LIMIT_REACHED");
        body.put("message", ex.getMessage());
        body.put("limitType", ex.getLimitType()); // "DAILY" or "MONTHLY"
        body.put("currentUsage", ex.getCurrentUsage());
        body.put("limitValue", ex.getLimitValue());
        body.put("upgradePlanName", ex.getUpgradePlanName()); // "PRO" or "ENTERPRISE"
        body.put("upgradeUrl", ex.getUpgradeUrl()); // "/pricing?plan=PRO"
        
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Database / Constraint Violations  ← THE KEY SECURITY FIX
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // Log the FULL technical error server-side (never sent to client)
        log.error("Data integrity violation – root cause: {}", ex.getMostSpecificCause().getMessage());

        // Friendly, non-leaking message derived from known constraint names
        String friendlyMessage = resolveFriendlyConstraintMessage(ex);

        Map<String, Object> body = buildBodyWithDetail(
                HttpStatus.CONFLICT.value(), "Data Conflict",
                friendlyMessage,
                // In dev, expose the constraint name only (not the full SQL)
                isDevelopment() ? ex.getMostSpecificCause().getMessage() : null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /**
     * Maps known DB constraint names to user-readable messages.
     * Extend this as you add more unique / FK constraints.
     */
    private String resolveFriendlyConstraintMessage(DataIntegrityViolationException ex) {
        String cause = ex.getMostSpecificCause().getMessage();
        if (cause == null) return "A data conflict occurred. Please check your input.";

        // Unique constraint patterns
        if (cause.contains("users_email_key") || cause.contains("email")) {
            return "This email address is already in use.";
        }
        if (cause.contains("unique") && cause.contains("username")) {
            return "This username is already taken.";
        }
        if (cause.contains("transaction_id")) {
            return "This transaction has already been processed.";
        }
        // Foreign key patterns
        if (cause.contains("violates foreign key constraint")) {
            return "The operation references a resource that does not exist.";
        }
        // Not-null patterns
        if (cause.contains("null value in column")) {
            return "Required information is missing. Please fill in all required fields.";
        }
        // Generic fallback – still no technical detail
        return "A data conflict occurred. Please check your input and try again.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Validation Errors
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());

        log.warn("Validation failed: {}", errors);

        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "Input validation failed.");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(), "Missing Parameter",
                "Required parameter '" + ex.getParameterName() + "' is missing.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(), "Invalid Parameter",
                "Invalid value for parameter '" + ex.getName() + "'.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body: {}", ex.getMessage());
        Map<String, Object> body = buildBody(
                HttpStatus.BAD_REQUEST.value(), "Bad Request",
                "The request body is malformed or contains invalid JSON.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Security / Auth Exceptions
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        Map<String, Object> body = buildBody(
                HttpStatus.FORBIDDEN.value(), "Forbidden",
                "You do not have permission to perform this action.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        // Generic message – do NOT reveal whether email or password was wrong
        log.warn("Bad credentials attempt");
        Map<String, Object> body = buildBody(
                HttpStatus.UNAUTHORIZED.value(), "Authentication Failed",
                "Invalid email or password.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, Object>> handleDisabledAccount(DisabledException ex) {
        log.warn("Disabled account login attempt");
        Map<String, Object> body = buildBody(
                HttpStatus.UNAUTHORIZED.value(), "Account Disabled",
                "Your account is disabled. Please contact support.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLockedAccount(LockedException ex) {
        log.warn("Locked account login attempt");
        Map<String, Object> body = buildBody(
                HttpStatus.UNAUTHORIZED.value(), "Account Locked",
                "Your account is temporarily locked. Please try again later.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. HTTP Method / Upload Errors
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMethod());
        Map<String, Object> body = buildBody(
                HttpStatus.METHOD_NOT_ALLOWED.value(), "Method Not Allowed",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        Map<String, Object> body = buildBody(
                HttpStatus.PAYLOAD_TOO_LARGE.value(), "File Too Large",
                "The uploaded file exceeds the maximum allowed size.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. IllegalState / IllegalArgument
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(java.lang.IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(java.lang.IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        Map<String, Object> body = buildBodyWithDetail(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), "Invalid State",
                "The operation could not be completed due to an invalid state.",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        Map<String, Object> body = buildBodyWithDetail(
                HttpStatus.BAD_REQUEST.value(), "Invalid Argument",
                "Invalid input provided.",
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Catch-All Fallback  ← MUST remain last
    // ─────────────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // Log everything server-side – class name, message, full trace
        log.error("Unexpected error [{}]: {}", ex.getClass().getName(), ex.getMessage(), ex);

        // Send NOTHING technical to the client in production
        Map<String, Object> body = buildBodyWithDetail(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                isDevelopment() ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
