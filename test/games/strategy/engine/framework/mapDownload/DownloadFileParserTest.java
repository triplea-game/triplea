package games.strategy.engine.framework.mapDownload;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;

public class DownloadFileParserTest {

  @Test
  public void testParse() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildTestXml().getBytes());
    final List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream, "hostedurl");

    assertThat(games.size(), is(2));

    DownloadFileDescription desc = games.get(0);
    assertThat(desc.getUrl(), is("http://example.com/games/game.zip"));
    assertThat(desc.getDescription(), Matchers.containsString("Some notes"));
    assertThat(desc.getMapName(), is("myGame"));

    desc = games.get(1);
    assertThat(desc.getUrl(), is("http://example.com/games/game2.zip"));
    assertThat(desc.getDescription(), Matchers.containsString("second game"));
    assertThat(desc.getMapName(), is("mySecondGame"));
    assertThat(desc.getHostedUrl(), is("hostedurl"));
  }


  private static String buildTestXml() {
    String xml = "";
    xml += "<games>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/game.zip</url>\n";
    xml += "    <mapName>myGame</mapName>\n";
    xml += "    <description><![CDATA[\n";
    xml += "	<pre>Some notes about the game, simple html allowed.\n";
    xml += "	</pre>\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "  <game>\n";
    xml += "    <url>http://example.com/games/game2.zip</url>\n";
    xml += "    <mapName>mySecondGame</mapName>\n";
    xml += "    <description><![CDATA[\n";
    xml += "	<pre>this is the second game.\n";
    xml += "	</pre>\n";
    xml += "    ]]></description>\n";
    xml += "  </game>\n";
    xml += "</games>\n";
    return xml;
  }


  // TODO we probably should do clientLogger.logError( ) to handle this, show an error
  // to the user and abort, rather than sending a stack trace and exception to the user.
  @Test(expected = IllegalStateException.class)
  public void testParseBadData() {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(buildBadTestXml().getBytes());
    final List<DownloadFileDescription> games = DownloadFileParser.parse(inputStream, "hostedurl");
    assertThat(games.size(), is(0));
  }

  private static String buildBadTestXml() {
    String xml = "";
    xml += "<games>\n";
    return xml;
  }
}
