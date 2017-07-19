package tools.map.xml.creator;

import org.junit.Before;
import org.junit.Ignore;

/**
 * Abstract class to be used as base for MapXmlHelperTest.
 */
public abstract class MapXmlCreatorTestBase {

  private MapXmlCreator mapXmlCreator;

  @Ignore
  @Before
  public void setUp() {
    setMapXmlCreator();
  }

  private void setMapXmlCreator() {
    mapXmlCreator = new MapXmlCreator();
  }

  protected MapXmlCreator getMapXmlCreator() {
    return mapXmlCreator;
  }
}
