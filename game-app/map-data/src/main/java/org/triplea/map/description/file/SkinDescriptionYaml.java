package org.triplea.map.description.file;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.triplea.io.FileUtils;
import org.triplea.yaml.YamlReader;

@Builder(access = AccessLevel.PRIVATE)
@Getter
public class SkinDescriptionYaml {
  @Nonnull private final Path filePath;
  @Nonnull private final String skinName;

  public static Optional<SkinDescriptionYaml> readSkinDescriptionYamlFile(Path skinYamlPath) {
    return FileUtils.openInputStream(skinYamlPath, inputStream -> parse(skinYamlPath, inputStream));
  }

  private static Optional<SkinDescriptionYaml> parse(Path filePath, InputStream inputStream) {
    final Map<String, Object> yamlData = YamlReader.readMap(inputStream);

    // TODO: validation that we read a valid yaml file, warn the user if we did not find
    // 'skin_name'!!

    return Optional.ofNullable(String.valueOf(yamlData.get("skin_name")))
        .map(
            skinName ->
                SkinDescriptionYaml.builder() //
                    .filePath(filePath)
                    .skinName(skinName)
                    .build());
  }
}
