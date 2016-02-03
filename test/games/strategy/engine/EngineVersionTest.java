package games.strategy.engine;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import games.strategy.test.TestUtil;
import games.strategy.util.Version;

public class EngineVersionTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testVersionParsingStopsAtFourNumbers() {
    final String EXPECTED_OUTPUT = "1.2.3.4";
    String input = EXPECTED_OUTPUT + ".5";


    EngineVersion testObj = createTestObj(input);
    assertThat(
        "We wrote to property file version:'1.2.3.4.5', and expect Version class to chomp off the fifth number and return '1.2.3.4'",
        testObj.getVersion(), is(new Version(1, 8, 0, 10)));

    input = EXPECTED_OUTPUT;
    testObj = createTestObj(input);
    assertThat(
        "We wrote to property file version:'1.2.3.4', and expect to get the same value back when reading from property file.",
        testObj.getVersion(), is(new Version(1, 8, 0, 10)));
  }

  private static EngineVersion createTestObj(String inputVersion) {
    final File propertyFile = TestUtil.createTempFile(EngineVersion.ENGINE_PROPERTY_KEY + " = " + inputVersion);
    EngineVersion version = new EngineVersion(propertyFile);
    return version;
  }


  @Test
  public void testGetExactVersion() {
    String input = "1.2.3.4.5";
    EngineVersion testObj = createTestObj(input);
    assertThat(
        "getExactVersion should return exactly what was set in the property file, no chomping of the fifth number.",
        testObj.getExactVersion(), is(input));
  }
}
