package org.triplea.yaml;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.snakeyaml.engine.v2.api.Dump;
import org.snakeyaml.engine.v2.api.DumpSettings;

/** Methods useful for writing YAML POJO data to a YAML formatted String. */
@UtilityClass
public class YamlWriter {
  public static String writeToString(final Map<String, Object> input) {
    return new Dump(DumpSettings.builder().build()).dumpToString(input);
  }
}
