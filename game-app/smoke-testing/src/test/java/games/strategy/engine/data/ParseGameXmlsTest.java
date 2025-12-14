package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import games.strategy.engine.data.gameparser.GameParser;
import games.strategy.engine.data.gameparser.XmlGameElementMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.config.product.ProductVersionReader;

class ParseGameXmlsTest {

  @ParameterizedTest
  @MethodSource
  void parseGameFiles(final Path xmlFile) {
    final Optional<GameData> result =
        GameParser.parse(
            xmlFile, new XmlGameElementMapper(), ProductVersionReader.getCurrentVersion(), false);
    assertThat(result, OptionalMatchers.isPresent());
  }

  @SuppressWarnings("unused")
  static Collection<Path> parseGameFiles() throws IOException {
    return TestDataFileLister.listFilesInTestResourcesDirectory("map-xmls").stream()
        .sorted(
            Comparator.comparing(file -> file.getFileName().toString().toUpperCase(Locale.ROOT)))
        .collect(Collectors.toList());
  }
}
