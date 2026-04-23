package com.example.delivery.user.domain.vo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UsernameConverter implements AttributeConverter<Username, String> {

    @Override
    public String convertToDatabaseColumn(Username attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public Username convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new Username(dbData);
    }
}
