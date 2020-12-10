package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.test.common.Integration;

@Integration
class ParseGameXmlsTest {

  @ParameterizedTest
  @MethodSource
  void parseGameFiles(final File xmlFile) {
    final Optional<GameData> result =
        GameParser.parse(
            xmlFile.toURI(), new XmlGameElementMapper(), new ProductVersionReader().getVersion());
    assertThat(result, OptionalMatchers.isPresent());
  }

  @SuppressWarnings("unused")
  static Collection<File> parseGameFiles() {
    return TestDataFileLister.listFilesInTestResourcesDirectory("map-xmls").stream()
        .sorted(Comparator.comparing(file -> file.getName().toUpperCase()))
        .collect(Collectors.toList());
  }
}
