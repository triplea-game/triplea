package org.triplea.yaml;

import com.google.common.base.Strings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

/**
 * Methods useful for working reading and writing YAML data. This class largely is a wrapper around
 * snake yaml.
 */
@UtilityClass
public class YamlUtils {
  public <T> T readYaml(final String inputString) {
    final var inputStream =
        new ByteArrayInputStream(Strings.nullToEmpty(inputString).getBytes(StandardCharsets.UTF_8));
    return readYaml(inputStream);
  }

  /**
   * Reads from input stream a YAML data structure. YAML is assumed to be map at the top level.
   *
   * @throws InvalidYamlFormatException Thrown if the YAML is badly formatted.
   */
  @SuppressWarnings("unchecked")
  public <T> T readYaml(final InputStream inputStream) {
    final Load load = new Load(LoadSettings.builder().build());
    try {
      final var result = (T) load.loadFromInputStream(inputStream);
      return Optional.ofNullable(result)
          .orElseThrow(() -> new InvalidYamlFormatException("Unable to read yaml data"));
    } catch (final YamlEngineException | ClassCastException e) {
      throw new InvalidYamlFormatException(e);
    }
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

  public String writeToYamlString(final Map<String, Object> data) {
    return new Dump(DumpSettings.builder().build()).dumpToString(data);
  }
}
