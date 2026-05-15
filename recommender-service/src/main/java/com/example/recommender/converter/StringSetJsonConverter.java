package com.example.recommender.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Converter
public class StringSetJsonConverter implements AttributeConverter<Set<String>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(StringSetJsonConverter.class);

    @Override
    public String convertToDatabaseColumn(Set<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize preference set {}", attribute, e);
            return "[]";
        }
    }

    @Override
    public Set<String> convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return new LinkedHashSet<>();
        }
        try {
            String[] values = OBJECT_MAPPER.readValue(dbData, String[].class);
            return new LinkedHashSet<>(Arrays.asList(values));
        } catch (Exception e) {
            log.warn("Failed to deserialize preference set {}", dbData, e);
            return new LinkedHashSet<>();
        }
    }
}
