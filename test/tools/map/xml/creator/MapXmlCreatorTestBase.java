package tools.map.xml.creator;

import org.junit.Before;
import org.junit.Ignore;

public abstract class MapXmlCreatorTestBase {

  private MapXmlCreator mapXmlCreator;

  @Ignore
  @Before
  public void setUp() {
    setMapXmlCreator();
  }

  private void setMapXmlCreator() {
    // mapXmlCreator = new MapXmlCreator(true);
  }

  protected MapXmlCreator getMapXmlCreator() {
    return mapXmlCreator;
  }
}
