package com.provit.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EmptyStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String value) {
        return value != null && value.isEmpty() ? null : value;
    }

    @Override
    public String convertToEntityAttribute(String value) {
        return value;
    }
}
