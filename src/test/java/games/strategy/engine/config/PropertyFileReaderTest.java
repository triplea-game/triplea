package games.strategy.engine.config;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Test;

import games.strategy.test.TestUtil;

public class PropertyFileReaderTest {

  @Test
  public void testPropertiesAreParsed() {
    final String input = " " + GameEnginePropertyReader.GameEngineProperty.MAP_LISTING_FILE + " = 1 ";

    final File propFile = TestUtil.createTempFile(input);
    final GameEnginePropertyReader testObj = new GameEnginePropertyReader(propFile);

    assertThat("Property key value pair 'x = 1 '  should be trimmed, no spaces in value or key.",
        testObj.readMapListingDownloadUrl(), is("1"));
  }

  @Test(expected = GameEnginePropertyReader.PropertyNotFoundException.class)
  public void testEmptyCase() {
    final String input = "";

    final File propFile = TestUtil.createTempFile(input);
    final GameEnginePropertyReader testObj = new GameEnginePropertyReader(propFile);

    assertThat("Simple empty input file, any key we read will return empty string",
        testObj.readMapListingDownloadUrl(), is(""));
  }
}
