package util.triplea.mapXmlCreator;

import org.junit.Test;

public class CanalDefinitionsPanelTest extends MapXmlCreatorTestBase {


  @Test
  public void testLayout() {
    CanalDefinitionsPanel.layout(getMapXmlCreator());

    assertSame(getMapXmlCreator(), ImageScrollPanePanel.mapXMLCreator);
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

