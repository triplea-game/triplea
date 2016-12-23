package tools.map.xml.creator;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public final class MapXmlHelperTest {
  @Test
  public void testNotesShouldBePlacedWithinCdataSection() throws Exception {
    final String notes = "lorum<br/>ipsum";
    initMapXmlData().setNotes(notes);

    final Document document = MapXmlHelper.getXMLDocument();

    final NodeList nodes = selectNodes(document, "//property[@name=\"notes\"]/value/child::node()");
    assertThat(nodes.getLength(), is(1));
    assertThat(nodes.item(0), is(instanceOf(CDATASection.class)));
    assertThat(nodes.item(0).getTextContent(), is(notes));
  }

  private static MapXmlData initMapXmlData() {
    final MapXmlData mapXmlData = new MapXmlData();
    MapXmlHelper.setMapXmlData(mapXmlData);
    return mapXmlData;
  }

  private static NodeList selectNodes(final Document document, final String expression) throws Exception {
    final XPath xpath = XPathFactory.newInstance().newXPath();
    return (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);
  }
}
