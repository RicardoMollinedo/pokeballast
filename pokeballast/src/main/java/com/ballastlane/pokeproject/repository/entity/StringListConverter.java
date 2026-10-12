package com.ballastlane.pokeproject.repository.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/**
 * Types and abilities are replicated, never filtered on and contain no commas, so a delimited
 * column is enough. Tags, which are filtered on, have their own table.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> values) {
        return values == null ? "" : String.join(DELIMITER, values);
    }

    @Override
    public List<String> convertToEntityAttribute(String column) {
        if (column == null || column.isBlank()) {
            return List.of();
        }
        return Arrays.stream(column.split(DELIMITER)).map(String::strip).toList();
    }
}
