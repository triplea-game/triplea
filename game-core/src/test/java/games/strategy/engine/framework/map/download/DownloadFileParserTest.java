package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

class DownloadFileParserTest {
  private static final String GAME_NAME = "myGame";

  @Test
  void testParseMap() throws Exception {
    final byte[] testData = buildTestData();

    final List<DownloadFileDescription> games =
        IoUtils.readFromMemory(testData, DownloadFileParser::parse);

    assertThat(games, hasSize(1));
    assertThat(games.get(0).getUrl(), is("http://example.com/games/game.zip"));
    assertThat(games.get(0).getDescription(), Matchers.containsString("Some notes"));
    assertThat(games.get(0).getMapName(), is(GAME_NAME));
  }

  private static byte[] buildTestData() {
    final String xml =
        ""
            + "- url: http://example.com/games/game.zip\n"
            + "  mapName: "
            + GAME_NAME
            + "\n"
            + "  version: 1\n"
            + "  description: |\n"
            + "     <pre>Some notes about the game, simple html allowed.\n"
            + "     </pre>\n";
    return xml.getBytes(StandardCharsets.UTF_8);
  }
}
