package com.ballastlane.pokeproject.bl.model;

import com.ballastlane.pokeproject.exceptions.BallastErrorCode;
import com.ballastlane.pokeproject.exceptions.BallastException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The user-editable fields of a task, validated and normalized. All errors are reported at once,
 * one message per field (code 1008).
 */
public record TaskContent(String title, String description, LocalDate dueDate) {

    public static final int MAX_TITLE = 120;
    public static final int MAX_DESCRIPTION = 2000;

    /**
     * @param today           business date, from the injected Clock
     * @param currentDueDate  due date stored before this change (null on create): an unchanged past due date
     *                        is accepted on update, otherwise old tasks could never be edited
     */
    public static TaskContent of(String title, String description, LocalDate dueDate, LocalDate today,
                                 LocalDate currentDueDate) {
        Map<String, String> errors = new LinkedHashMap<>();
        String cleanTitle = title == null ? "" : title.strip();
        if (cleanTitle.isEmpty() || cleanTitle.length() > MAX_TITLE) {
            errors.put("title", "is required and must be at most " + MAX_TITLE + " characters");
        }
        String cleanDescription = description == null || description.isBlank() ? null : description.strip();
        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION) {
            errors.put("description", "must be at most " + MAX_DESCRIPTION + " characters");
        }
        boolean dueDateChanged = dueDate != null && !dueDate.equals(currentDueDate);
        if (dueDateChanged && dueDate.isBefore(today)) {
            errors.put("dueDate", "cannot be in the past");
        }
        if (!errors.isEmpty()) {
            throw new BallastException(BallastErrorCode.INVALID_TASK, BallastErrorCode.INVALID_TASK.defaultMessage(), errors);
        }
        return new TaskContent(cleanTitle, cleanDescription, dueDate);
    }
}
