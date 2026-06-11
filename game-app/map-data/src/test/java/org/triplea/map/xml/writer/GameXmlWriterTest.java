package org.triplea.map.xml.writer;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.github.tomakehurst.wiremock.common.xml.Xml;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.map.data.elements.Game;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;
import org.xmlunit.util.DocumentBuilderFactoryConfigurer;

class GameXmlWriterTest {

  /**
   * In this test we'll assume 'XmlMapper' is correct and load a 'Game' object from XML. We'll then
   * write that same Game object back to another XML file and then verify that the read and written
   * XML files are equivalent.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "attachment-list.xml",
        "dice-sides.xml",
        "game.xml",
        "game-play.xml",
        "info.xml",
        "initialize.xml",
        "map.xml",
        "player-list.xml",
        "production.xml",
        "property-list.xml",
        "relationship-types.xml",
        "resource-list.xml",
        "technology.xml",
        "territory-effect-list.xml",
        "unit-list.xml",
        "variable-list.xml"
      })
  void verifyXmlOutput(final String gameXmlFileName) throws Exception {
    final URI uri = this.getClass().getClassLoader().getResource(gameXmlFileName).toURI();

    final XmlMapper mapper =
        new XmlMapper(this.getClass().getClassLoader().getResourceAsStream(gameXmlFileName));
    final Game game = mapper.mapXmlToObject(Game.class);

    final Path outputPath = Path.of("output" + gameXmlFileName);
    GameXmlWriter.exportXml(game, outputPath);
    outputPath.toFile().deleteOnExit();

    final String input = Files.readString(Path.of(uri));
    final String output = Files.readString(outputPath);

    // Allow DTD parsing as XMLs might contain <!DOCTYPE game SYSTEM "game.dtd">
    Diff diff =
        DiffBuilder.compare(Input.fromString(input))
            .withTest(Input.fromString(output))
            .withDocumentBuilderFactory(
                DocumentBuilderFactoryConfigurer.DefaultWithDTDParsing.configure(
                    Xml.newDocumentBuilderFactory()))
            .build();
    assertFalse(diff.hasDifferences());
  }
}
