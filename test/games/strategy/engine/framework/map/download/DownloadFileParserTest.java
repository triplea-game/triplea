package games.strategy.engine.framework.map.download;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

public class DownloadFileParserTest {

  private static final String GAME_NAME = "myGame";


  @Test
  public void testParseMap() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertThat(games.size(), is(4));
    DownloadFileDescription desc = games.get(0);
    assertThat(desc.getUrl(), is("http://example.com/games/game.zip"));
    assertThat(desc.getDescription(), Matchers.containsString("Some notes"));
    assertThat(desc.getMapName(), is(GAME_NAME));


    assertThat(desc.isMap(), is(true));
    assertThat(desc.isMapMod(), is(false));
    assertThat(desc.isMapSkin(), is(false));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  public void testParseMapMod() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertThat(games.size(), is(4));
    DownloadFileDescription desc = games.get(1);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapMod(), is(true));
    assertThat(desc.isMapSkin(), is(false));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  public void testParseModSkin() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertThat(games.size(), is(4));
    DownloadFileDescription desc = games.get(2);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapMod(), is(false));
    assertThat(desc.isMapSkin(), is(true));
    assertThat(desc.isMapTool(), is(false));
  }

  @Test
  public void testParseMapTool() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertThat(games.size(), is(4));
    DownloadFileDescription desc = games.get(3);
    assertThat(desc.isMap(), is(false));
    assertThat(desc.isMapMod(), is(false));
    assertThat(desc.isMapSkin(), is(false));
    assertThat(desc.isMapTool(), is(true));
  }


  private static String createTypeTag(DownloadFileParser.ValueType type) {
    return "  " + DownloadFileParser.Tags.mapType + ": " + type + "\n";
  }

  private static byte[] buildTestXml() {
    String xml = "";
    xml += "- url: http://example.com/games/game.zip\n";
    xml += "  mapName: " + GAME_NAME + "\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP);
    xml += "  description: |\n";
    xml += "     <pre>Some notes about the game, simple html allowed.\n";
    xml += "     </pre>\n";
    xml += "- url: http://example.com/games/mod.zip\n";
    xml += "  mapName: mapModName\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_MOD);
    xml += "  description: |\n";
    xml += "      map mod\n";
    xml += "- url: http://example.com/games/skin.zip\n";
    xml += "  mapName: skin\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_SKIN);
    xml += "  description: |\n";
    xml += "      map skin\n";
    xml += "- url: http://example.com/games/tool.zip\n";
    xml += "  mapName: mapToolName\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_TOOL);
    xml += "  description: |\n";
    xml += "       <pre>\n";
    xml += "       this is a map tool";
    xml += "    </pre>\n";
    return xml.getBytes();
  }


  @Test
  public void testMapTypeDefaultsToMap() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(createSimpleGameXmlWithNoTypeTag());
    DownloadFileDescription download = DownloadFileParser.parse(inputStream).get(0);

    assertThat(download.isMap(), is(true));
    assertThat(download.isMapSkin(), is(false));
    assertThat(download.isMapTool(), is(false));
    assertThat(download.isMapMod(), is(false));
  }

  private static byte[] createSimpleGameXmlWithNoTypeTag( ) {
    String xml = "";
    xml += "- url: http://example.com/games/mod.zip\n";
    xml += "  mapName: " + GAME_NAME + "\n";
    xml += "  description: |\n";
    xml += "      description\n";
    return xml.getBytes();
  }
}