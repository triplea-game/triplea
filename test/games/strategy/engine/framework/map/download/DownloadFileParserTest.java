package games.strategy.engine.framework.map.download;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Test;

public class DownloadFileParserTest {

  private static final String GAME_NAME = "myGame";


  @Test
  public void testParseMap() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertEquals(4, games.size());
    DownloadFileDescription desc = games.get(0);
    assertEquals(desc.getUrl(), "http://example.com/games/game.zip");
    assertTrue(desc.getDescription().contains("Some notes"));
    assertEquals(GAME_NAME, desc.getMapName());


    assertTrue(desc.isMap());
    assertFalse(desc.isMapMod());
    assertFalse(desc.isMapSkin());
    assertFalse(desc.isMapTool());
  }

  @Test
  public void testParseMapMod() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertEquals(4, games.size());
    DownloadFileDescription desc = games.get(1);
    assertFalse(desc.isMap());
    assertTrue(desc.isMapMod());
    assertFalse(desc.isMapSkin());
    assertFalse(desc.isMapTool());
  }

  @Test
  public void testParseModSkin() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertEquals(4, games.size());
    DownloadFileDescription desc = games.get(2);
    assertFalse(desc.isMap());
    assertFalse(desc.isMapMod());
    assertTrue(desc.isMapSkin());
    assertFalse(desc.isMapTool());
  }

  @Test
  public void testParseMapTool() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml());
    List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream);
    assertEquals(4, games.size());
    DownloadFileDescription desc = games.get(3);
    assertFalse(desc.isMap());
    assertFalse(desc.isMapMod());
    assertFalse(desc.isMapSkin());
    assertTrue(desc.isMapTool());
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

    assertTrue(download.isMap());
    assertFalse(download.isMapSkin());
    assertFalse(download.isMapTool());
    assertFalse(download.isMapMod());
  }



  private static byte[] createSimpleGameXmlWithNoTypeTag() {
    String xml = "";
    xml += "- url: http://example.com/games/mod.zip\n";
    xml += "  mapName: " + GAME_NAME + "\n";
    xml += "  description: |\n";
    xml += "      description\n";
    return xml.getBytes();
  }

}
