package org.triplea.test.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtil {
  private static final ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.registerModule(new JavaTimeModule());
  }

  public static <T> String toJson(final T object) {
    try {
      return objectMapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new RuntimeException("Failed to convert to JSON string: " + object, e);
    }
  }
}
