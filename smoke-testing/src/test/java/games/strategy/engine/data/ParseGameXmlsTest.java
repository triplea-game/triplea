package games.strategy.engine.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.test.common.Integration;

@SuppressWarnings("unused")
@Integration
class ParseGameXmlsTest {

  @ParameterizedTest
  @MethodSource
  void parseGameFiles(final File xmlFile) throws Exception {
    try (InputStream inputStream = new FileInputStream(xmlFile)) {
      GameParser.parse(xmlFile.getAbsolutePath(), inputStream);
    }
  }

  static Collection<File> parseGameFiles() {
    return Arrays.stream(findMapXmlsFolder().listFiles())
        .sorted(Comparator.comparing(File::getName))
        .collect(Collectors.toList());
  }

  private static File findMapXmlsFolder() {
    final File mapXmls = Paths.get("src", "test", "resources", "map-xmls").toFile();
    if (mapXmls.exists()) {
      return mapXmls;
    }

    return Paths.get("smoke-testing", "src", "test", "resources", "map-xmls").toFile();
  }
}
