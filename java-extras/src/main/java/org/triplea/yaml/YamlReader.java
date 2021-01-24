package org.triplea.yaml;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

/** Methods useful for reading YAML data from a String or InputStream. */
@UtilityClass
public class YamlReader {
  /**
   * Reads from input a YAML data structure. YAML is assumed to be a List at the top level.
   *
   * @throws InvalidYamlFormatException Thrown if the YAML is badly formatted.
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> readList(final String input) {
    try {
      return (List<Map<String, Object>>) readYaml(asStream(input));
    } catch (final YamlEngineException | ClassCastException e) {
      throw new InvalidYamlFormatException(e);
    }
  }

  /**
   * Reads from input a YAML data structure. YAML is assumed to be a List at the top level.
   *
   * @throws InvalidYamlFormatException Thrown if the YAML is badly formatted.
   */
  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> readList(final InputStream input) {
    try {
      return (List<Map<String, Object>>) readYaml(input);
    } catch (final YamlEngineException | ClassCastException e) {
      throw new InvalidYamlFormatException(e);
    }
  }

  /**
   * Reads from input a YAML data structure. YAML is assumed to be map at the top level.
   *
   * @throws InvalidYamlFormatException Thrown if the YAML is badly formatted.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> readMap(final String input) {
    try {
      return (Map<String, Object>) readYaml(asStream(input));
    } catch (final YamlEngineException | ClassCastException e) {
      throw new InvalidYamlFormatException(e);
    }
  }

  /**
   * Reads from input a YAML data structure. YAML is assumed to be map at the top level.
   *
   * @throws InvalidYamlFormatException Thrown if the YAML is badly formatted.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> readMap(final InputStream input) {
    try {
      return (Map<String, Object>) readYaml(input);
    } catch (final YamlEngineException | ClassCastException e) {
      throw new InvalidYamlFormatException(e);
    }
  }

  private static InputStream asStream(final String inputString) {
    return new ByteArrayInputStream(
        Strings.nullToEmpty(inputString).getBytes(StandardCharsets.UTF_8));
  }

  private static Object readYaml(final InputStream inputStream) {
    final Load load = new Load(LoadSettings.builder().build());
    final var result = load.loadFromInputStream(inputStream);
    return Optional.ofNullable(result)
        .orElseThrow(() -> new InvalidYamlFormatException("Unable to read yaml data"));
  }

  public static class InvalidYamlFormatException extends RuntimeException {
    private static final long serialVersionUID = -1920143388576824621L;

    public InvalidYamlFormatException(final String message) {
      super(message);
    }

    public InvalidYamlFormatException(final Exception e) {
      super("Invalid yaml format: " + e.getMessage(), e);
    }
  }
}
