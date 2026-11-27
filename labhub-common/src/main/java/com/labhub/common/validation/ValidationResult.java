package com.labhub.common.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects field-level and global validation errors.
 * Returned in the payload of VALIDATION_ERROR responses.
 */
public class ValidationResult {

    private final List<FieldError> fieldErrors = new ArrayList<>();
    private final List<String> globalErrors = new ArrayList<>();

    /**
     * Adds a field-level error.
     *
     * @param field   the field name as it appears in the UI form
     * @param message human-readable error message
     * @return this instance for chaining
     */
    public ValidationResult addFieldError(String field, String message) {
        fieldErrors.add(new FieldError(field, message));
        return this;
    }

    /**
     * Adds a global (non-field) error.
     *
     * @param message human-readable error message
     * @return this instance for chaining
     */
    public ValidationResult addGlobalError(String message) {
        globalErrors.add(message);
        return this;
    }

    /**
     * Returns true if no errors have been recorded.
     *
     * @return true if valid
     */
    public boolean isValid() {
        return fieldErrors.isEmpty() && globalErrors.isEmpty();
    }

    /**
     * Returns all field-level errors.
     *
     * @return list of field errors
     */
    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    /**
     * Returns all global errors.
     *
     * @return list of global error messages
     */
    public List<String> getGlobalErrors() {
        return globalErrors;
    }

    /**
     * Returns a combined summary message suitable for a dialog header.
     *
     * @return summary message
     */
    public String getSummaryMessage() {
        int total = fieldErrors.size() + globalErrors.size();
        if (total == 0) {
            return "No errors.";
        }
        if (total == 1) {
            if (!globalErrors.isEmpty()) {
                return globalErrors.get(0);
            }
            return fieldErrors.get(0).getMessage();
        }
        return total + " validation errors. Please review the highlighted fields.";
    }

    /**
     * A single field-level validation error.
     */
    public static class FieldError {

        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() { return field; }

        public String getMessage() { return message; }
    }
}
