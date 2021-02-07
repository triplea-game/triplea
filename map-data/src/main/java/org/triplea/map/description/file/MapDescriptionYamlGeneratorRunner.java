package org.triplea.map.description.file;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.Builder;
import org.triplea.io.FileUtils;

/**
 * Job that runs across all downloaded map folders and generates a 'map.yml' file for each map where
 * missing.
 */
@Builder
public class MapDescriptionYamlGeneratorRunner {

  /** Callback to be invoked if we start generating YAML files. */
  private final Consumer<Runnable> progressIndicator;

  /** Path to where downloaded maps can be found. */
  private final Path downloadedMapsFolder;

  public void generateYamlFiles() {
    final Collection<File> toGenerate =
        FileUtils.listFiles(downloadedMapsFolder.toFile()).stream()
            .filter(File::isDirectory)
            .filter(Predicate.not(MapDescriptionYaml::mapHasYamlDescriptor))
            .collect(Collectors.toList());
    if (toGenerate.size() > 4) {
      // only show a progress indicator if we have a few to generate
      progressIndicator.accept(() -> toGenerate.forEach(MapDescriptionYaml::generateForMap));
    }
  }
}
