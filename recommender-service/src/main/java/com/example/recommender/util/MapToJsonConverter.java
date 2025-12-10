package com.example.recommender.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

@Converter
@Slf4j
public class MapToJsonConverter implements AttributeConverter<Map<String, Double>, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Double> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        // сериализуем мапу весов в json для jsonb колонки
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            log.warn("failed to serialize map to json", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Double> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        // десериализуем jsonb обратно в мапу весов
        try {
            return OBJECT_MAPPER.readValue(dbData, OBJECT_MAPPER.getTypeFactory()
                    .constructMapType(Map.class, String.class, Double.class));
        } catch (JsonProcessingException e) {
            log.warn("failed to deserialize json to map", e);
            return Collections.emptyMap();
        }
    }
}
