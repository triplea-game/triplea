package games.strategy.engine.data;

import games.strategy.engine.data.gameparser.GameParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
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
    return TestDataFileLister.listFilesInTestResourcesDirectory("map-xmls");
  }
}
