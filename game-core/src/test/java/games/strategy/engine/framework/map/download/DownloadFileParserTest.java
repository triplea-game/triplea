package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.triplea.io.IoUtils;

class DownloadFileParserTest {
  private static final String GAME_NAME = "myGame";

  @Test
  void testParseMap() throws Exception {
    final List<DownloadFileDescription> games = parse(buildTestData());
    assertThat(games.size(), is(4));
    final DownloadFileDescription desc = games.get(0);
    assertThat(desc.getUrl(), is("http://example.com/games/game.zip"));
    assertThat(desc.getDescription(), Matchers.containsString("Some notes"));
    assertThat(desc.getMapName(), is(GAME_NAME));

    assertThat(desc.isMap(), is(true));
    assertThat(desc.isMapSkin(), is(false));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  void testParseModSkin() throws Exception {
    final List<DownloadFileDescription> games = parse(buildTestData());
    assertThat(games.size(), is(4));
    final DownloadFileDescription desc = games.get(2);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapSkin(), is(true));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  void testParseMapTool() throws Exception {
    final List<DownloadFileDescription> games = parse(buildTestData());
    assertThat(games.size(), is(4));
    final DownloadFileDescription desc = games.get(3);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapSkin(), is(false));
    assertThat(desc.isMapTool(), is(true));
  }

  private static List<DownloadFileDescription> parse(final byte[] bytes) throws Exception {
    return IoUtils.readFromMemory(bytes, DownloadFileParser::parse);
  }

  private static String newTypeTag(final DownloadFileDescription.DownloadType type) {
    return "  " + DownloadFileParser.Tags.mapType + ": " + type + "\n";
  }

  private static byte[] buildTestData() {
    final String xml =
        ""
            + "- url: http://example.com/games/game.zip\n"
            + "  mapName: "
            + GAME_NAME
            + "\n"
            + "  version: 1\n"
            + newTypeTag(DownloadFileDescription.DownloadType.MAP)
            + "  description: |\n"
            + "     <pre>Some notes about the game, simple html allowed.\n"
            + "     </pre>\n"
            + "- url: http://example.com/games/mod.zip\n"
            + "  mapName: modName\n"
            + "  version: 1\n"
            // missing map type defaults to map
            + "  description: |\n"
            + "      map mod\n"
            + "- url: http://example.com/games/skin.zip\n"
            + "  mapName: skin\n"
            + "  version: 1\n"
            + newTypeTag(DownloadFileDescription.DownloadType.MAP_SKIN)
            + "  description: |\n"
            + "      map skin\n"
            + "- url: http://example.com/games/tool.zip\n"
            + "  mapName: mapToolName\n"
            + "  version: 1\n"
            + newTypeTag(DownloadFileDescription.DownloadType.MAP_TOOL)
            + "  description: |\n"
            + "       <pre>\n"
            + "       this is a map tool"
            + "    </pre>\n";
    return xml.getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void testMapTypeDefaultsToMap() throws Exception {
    final DownloadFileDescription download = parse(newSimpleGameXmlWithNoTypeTag()).get(0);

    assertThat(download.isMap(), is(true));
    assertThat(download.isMapSkin(), is(false));
    assertThat(download.isMapTool(), is(false));
  }

  private static byte[] newSimpleGameXmlWithNoTypeTag() {
    final String xml =
        ""
            + "- url: http://example.com/games/mod.zip\n"
            + "  mapName: "
            + GAME_NAME
            + "\n"
            + "  version: 1\n"
            + "  description: |\n"
            + "      description\n";
    return xml.getBytes(StandardCharsets.UTF_8);
  }
}
