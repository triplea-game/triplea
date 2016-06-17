package tools.map.xml.creator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class CanalDefinitionsPanelTest extends MapXmlCreatorTestBase {


  @Test
  public void testLayout() {
    // //TODO: find a way to allow Travis CI build without failing with
    // // "java.awt.HeadlessException:
    // // No X11 DISPLAY variable was set, but this program performed an operation which requires it."
    CanalDefinitionsPanel.layout(getMapXmlCreator());

    assertSame(getMapXmlCreator(), ImageScrollPanePanel.getMapXmlCreator());
    assertEquals(false, ImageScrollPanePanel.polygons.isEmpty());
    assertEquals(false, ImageScrollPanePanel.polygonsInvalid);

  }

  // TODO: Move to MapXmlHelperTest
  // @Test
  // public void testGetHtmlStringFromCanalDefinitions() {
  // final String htmlStringFromCanalDefinitions = MapXmlHelper.getHtmlStringFromCanalDefinitions();
  //
  // assertTrue(htmlStringFromCanalDefinitions.startsWith("<html"));
  // assertTrue(htmlStringFromCanalDefinitions.endsWith("</html>"));
  //
  // final int countCanals = MapXmlHelper.getCanalDefinitionsMap().size();
  // if (countCanals > 0) {
  // // #line breaks should equal #canals
  // final int countBrTags =
  // htmlStringFromCanalDefinitions.length() - htmlStringFromCanalDefinitions.replace("<br", "br").length();
  // assertEquals(countCanals, countBrTags);
  //
  // // test for whether all territories are listed
  // for (final Entry<String, CanalTerritoriesTuple> canalDefMapEntry : MapXmlHelper.getCanalDefinitionsMap()
  // .entrySet()) {
  // final String canalKeySearchString = CanalDefinitionsPanel.HTML_CANAL_KEY_PREFIX + canalDefMapEntry.getKey()
  // + CanalDefinitionsPanel.HTML_CANAL_KEY_POSTFIX;
  // final int indexKeyEnd =
  // htmlStringFromCanalDefinitions.indexOf(canalKeySearchString) + canalKeySearchString.length();
  // final int indexEntryEnd = htmlStringFromCanalDefinitions.indexOf("<br", indexKeyEnd);
  // final String htmlExtract = htmlStringFromCanalDefinitions.substring(indexKeyEnd, indexEntryEnd);
  // for (final String value : canalDefMapEntry.getValue().getWaterTerritories()) {
  // assertTrue(htmlExtract.contains(value));
  // }
  // }
  // }
  // }

}

