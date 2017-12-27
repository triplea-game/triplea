package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import games.strategy.io.IoUtils;

public class DownloadFileParserTest {
  private static final String GAME_NAME = "myGame";


  @Test
  public void testParseMap() throws Exception {
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
  public void testParseModSkin() throws Exception {
    final List<DownloadFileDescription> games = parse(buildTestData());
    assertThat(games.size(), is(4));
    final DownloadFileDescription desc = games.get(2);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapSkin(), is(true));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  public void testParseMapTool() throws Exception {
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

  private static String createTypeTag(final DownloadFileDescription.DownloadType type) {
    return "  " + DownloadFileParser.Tags.mapType + ": " + type + "\n";
  }

  private static byte[] buildTestData() {
    String xml = "";
    xml += "- url: http://example.com/games/game.zip\n";
    xml += "  mapName: " + GAME_NAME + "\n";
    xml += "  version: 1\n";
    xml += createTypeTag(DownloadFileDescription.DownloadType.MAP);
    xml += "  description: |\n";
    xml += "     <pre>Some notes about the game, simple html allowed.\n";
    xml += "     </pre>\n";
    xml += "- url: http://example.com/games/mod.zip\n";
    xml += "  mapName: modName\n";
    xml += "  version: 1\n";
    // missing map type defaults to map
    xml += "  description: |\n";
    xml += "      map mod\n";
    xml += "- url: http://example.com/games/skin.zip\n";
    xml += "  mapName: skin\n";
    xml += "  version: 1\n";
    xml += createTypeTag(DownloadFileDescription.DownloadType.MAP_SKIN);
    xml += "  description: |\n";
    xml += "      map skin\n";
    xml += "- url: http://example.com/games/tool.zip\n";
    xml += "  mapName: mapToolName\n";
    xml += "  version: 1\n";
    xml += createTypeTag(DownloadFileDescription.DownloadType.MAP_TOOL);
    xml += "  description: |\n";
    xml += "       <pre>\n";
    xml += "       this is a map tool";
    xml += "    </pre>\n";
    return xml.getBytes();
  }


  @Test
  public void testMapTypeDefaultsToMap() throws Exception {
    final DownloadFileDescription download = parse(createSimpleGameXmlWithNoTypeTag()).get(0);

    assertThat(download.isMap(), is(true));
    assertThat(download.isMapSkin(), is(false));
    assertThat(download.isMapTool(), is(false));
  }

  private static byte[] createSimpleGameXmlWithNoTypeTag() {
    String xml = "";
    xml += "- url: http://example.com/games/mod.zip\n";
    xml += "  mapName: " + GAME_NAME + "\n";
    xml += "  version: 1\n";
    xml += "  description: |\n";
    xml += "      description\n";
    return xml.getBytes();
  }
}
