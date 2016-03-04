package games.strategy.engine.framework.map.download;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadFileParser;

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
    return "<" + DownloadFileParser.Tags.mapType + ">" + type + "</" + DownloadFileParser.Tags.mapType + ">";
  }

  private static byte[] buildTestXml() {
    String xml = "";
    xml += "<games>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/game.zip</url>\n";
    xml += "    <mapName>" + GAME_NAME + "</mapName>\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP);
    xml += "    <description><![CDATA[\n";
    xml += "	<pre>Some notes about the game, simple html allowed.\n";
    xml += "	</pre>\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/mod.zip</url>\n";
    xml += "    <mapName>mapModName</mapName>\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_MOD);
    xml += "    <description><![CDATA[\n";
    xml += "      map mod\n";
    xml += "	</pre>\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/skin.zip</url>\n";
    xml += "    <mapName>mapSkinName</mapName>\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_SKIN);
    xml += "    <description><![CDATA[\n";
    xml += "      map skin\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/tool.zip</url>\n";
    xml += "    <mapName>mapToolName</mapName>\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_TOOL);
    xml += "    <description><![CDATA[\n";
    xml += "       this is a map tool";
    xml += "    </pre>\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "</games>\n";
    return xml.getBytes();
  }


  // TODO we probably should do clientLogger.logError( ) to handle this, show an error
  // to the user and abort, rather than sending a stack trace and exception to the user.
  @Test(expected = IllegalStateException.class)
  public void testParseBadData() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildBadTestXml());
    DownloadFileParser.parse(inputStream);
  }

  private static byte[] buildBadTestXml() {
    String xml = "";
    xml += "<games>\n";
    return xml.getBytes();
  }

  @Test(expected = IllegalStateException.class)
  public void testDuplicateMapNames() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildDuplicateMapNameTestXml().getBytes());
    DownloadFileParser.parse(inputStream);
  }

  private static String buildDuplicateMapNameTestXml() {
    String xml = "";
    xml += "<games>\n";
      xml += createGameXml( );
      xml += createGameXml( );
    xml += "</games>\n";
    return xml;
  }

  private static String createGameXml( ) {
    String xml = "";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/mod.zip</url>\n";
    xml += "    <mapName>" + GAME_NAME + "</mapName>\n";
    xml += createTypeTag(DownloadFileParser.ValueType.MAP_MOD);
    xml += "    <description><![CDATA[\n";
    xml += "      description\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    return xml;
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
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/mod.zip</url>\n";
    xml += "    <mapName>" + GAME_NAME + "</mapName>\n";
    xml += "    <description><![CDATA[\n";
    xml += "      description\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    return xml.getBytes();
  }

}
