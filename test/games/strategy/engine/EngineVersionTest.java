package games.strategy.engine;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.config.GameEngineProperty;
import games.strategy.engine.config.PropertyReader;
import games.strategy.test.MockitoExtension;
import games.strategy.util.Version;

@ExtendWith(MockitoExtension.class)
public class EngineVersionTest {

  @Test
  public void testVersionParsingStopsAtFourNumbers() {
    final String EXPECTED_OUTPUT = "1.2.3.4";
    String input = EXPECTED_OUTPUT + ".5";


    EngineVersion testObj = createTestObj(input);
    assertThat(
        "We wrote to property file version:'1.2.3.4.5', and expect Version class to chomp off the fifth number and return '1.2.3.4'",
        testObj.getVersion(), is(new Version(1, 2, 3, 4)));

    input = EXPECTED_OUTPUT;
    testObj = createTestObj(input);
    assertThat(
        "We wrote to property file version:'1.2.3.4', and expect to get the same value back when reading from property file.",
        testObj.getVersion(), is(new Version(1, 2, 3, 4)));
  }

  @Mock
  private PropertyReader mockReader;

  private EngineVersion createTestObj(String inputVersion) {
    when(mockReader.readProperty(GameEngineProperty.ENGINE_VERSION)).thenReturn(inputVersion);
    EngineVersion version = new EngineVersion(mockReader);
    return version;
  }


  @Test
  public void testGetExactVersion() {
    String input = "1.2.3.4.5";
    EngineVersion testObj = createTestObj(input);
    assertThat(
        "getExactVersion should return exactly what was set in the property file, no chomping of the fifth number.",
        testObj.getFullVersion(), is(input));
  }
}
