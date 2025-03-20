package com.arpanrec.bastet.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

@Converter
public class UserPermissionsAttributeConverter implements AttributeConverter<List<Role>, String> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Role> attribute) {
        try {
            return mapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert list of roles to JSON string", e);
        }
    }

    @Override
    public List<Role> convertToEntityAttribute(String dbData) {
        try {
            return mapper.readValue(dbData, mapper.getTypeFactory().constructCollectionType(List.class, Role.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON string to list of roles", e);
        }
    }
}
