package tools.map.xml.creator;



import org.junit.Before;

public abstract class MapXmlCreatorTestBase {

  private MapXmlCreator mapXmlCreator;

  @Before
  protected void setUp() {
    setMapXmlCreator();
  }

  private void setMapXmlCreator() {
    // mapXmlCreator = new MapXmlCreator(true);
  }

  protected MapXmlCreator getMapXmlCreator() {
    return mapXmlCreator;
  }

}
