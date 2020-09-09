package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import java.io.File;
import java.util.Collection;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.test.common.Integration;

@Integration
class ParseGameXmlsTest {

  @ParameterizedTest
  @MethodSource
  void parseGameFiles(final File xmlFile) {
    final Optional<GameData> result = GameParser.parse(xmlFile.toURI(), new XmlGameElementMapper());
    assertThat(result, OptionalMatchers.isPresent());
  }

  @SuppressWarnings("unused")
  static Collection<File> parseGameFiles() {
    return TestDataFileLister.listFilesInTestResourcesDirectory("map-xmls");
  }
}
