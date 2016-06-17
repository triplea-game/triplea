package games.strategy.engine.config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import games.strategy.test.TestUtil;

public class GameEnginePropertyFileReaderTest {

  private final GameEngineProperty propKey = GameEngineProperty.MAP_LISTING_SOURCE_FILE;

  @Test
  public void testPropertiesAreParsed() {
    String input = " " + propKey + " = 1 ";

    File propFile = TestUtil.createTempFile(input);
    PropertyReader testObj = new GameEnginePropertyFileReader(propFile);

    assertThat("Property key value pair 'x = 1 '  should be trimmed, no spaces in value or key.",
        testObj.readProperty(propKey), is("1"));
  }

  @Test(expected = PropertyNotFoundException.class)
  public void testEmptyCase() {
    String input = "";

    File propFile = TestUtil.createTempFile(input);
    PropertyReader testObj = new GameEnginePropertyFileReader(propFile);

    assertThat("Simple empty input file, any key we read will return empty string",
        testObj.readProperty(propKey), is(""));
  }
}
