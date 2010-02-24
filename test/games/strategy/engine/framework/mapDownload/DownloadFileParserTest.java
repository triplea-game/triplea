package games.strategy.engine.framework.mapDownload;

import java.io.ByteArrayInputStream;
import java.util.List;

import junit.framework.TestCase;

public class DownloadFileParserTest extends TestCase {

	
	
	public void testParse() {
		List<DownloadFileDescription> games = new DownloadFileParser().parse(new ByteArrayInputStream(xml.getBytes()), "hostedurl");
		assertEquals(2, games.size());
		DownloadFileDescription desc = games.get(0);
		assertEquals(desc.getUrl(), "http://example.com/games/game.zip");
		assertTrue(desc.getDescription().contains("Some notes"));
		assertEquals(desc.getMapName(), "myGame");
		
		desc = games.get(1);
		assertEquals(desc.getUrl(), "http://example.com/games/game2.zip");
		assertTrue(desc.getDescription().contains("second game"));
		assertEquals(desc.getMapName(), "mySecondGame");
		assertEquals(desc.getHostedUrl(), "hostedurl");
	}
	
	
	
	private static final String xml = "<games>\n" + 
			"  <game>\n" + 
			"    <url>http://example.com/games/game.zip</url>\n" + 
			"    <mapName>myGame</mapName>\n" + 
			"    <description><![CDATA[\n" + 
			"	<pre>Some notes about the game, simple html allowed.\n" + 
			"	</pre>\n" + 
			"    ]]></description>\n" + 
			"  </game>\n" + 
			"  <game>\n" + 
			"    <url>http://example.com/games/game2.zip</url>\n" + 
			"    <mapName>mySecondGame</mapName>\n" + 
			"    <description><![CDATA[\n" + 
			"	<pre>this is the second game.\n" + 
			"	</pre>\n" + 
			"    ]]></description>\n" + 
			"  </game>\n" + 
			"\n" + 
			"</games>\n";
}
