package util.triplea.mapXmlCreator;



import org.junit.Before;

import junit.framework.TestCase;

public abstract class MapXmlCreatorTestBase extends TestCase {

  private MapXmlCreator mapXmlCreator;

  public MapXmlCreatorTestBase() {
    super();
  }

  public MapXmlCreatorTestBase(final String name) {
    super(name);
  }

  @Override
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
