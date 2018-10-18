package games.strategy.engine.framework;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.framework.headless.game.server.HeadlessGameServerCliParam;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;

public class ArgParserTest extends AbstractClientSettingTestCase {

  @AfterEach
  public void teardown() {
    System.clearProperty(TRIPLEA_GAME);
  }

  @Test
  public void argsTurnIntoSystemProps() {
    assertThat("check precondition, system property for our test key should not be set yet.",
        System.getProperty(TestData.propKey), nullValue());

    ArgParser.handleCommandLineArgs(TestData.sampleArgInput);

    assertThat("system property should now be set to our test value",
        System.getProperty(TestData.propKey), is(TestData.propValue));
  }

  @Test
  public void emptySystemPropertiesCanBeSet() {
    ArgParser.handleCommandLineArgs("-Pa=");
    assertThat("expecting the system property to be empty string instead of null",
        System.getProperty("a"), is(""));
  }

  @Test
  public void singleFileArgIsAssumedToBeGameProperty() {
    ArgParser.handleCommandLineArgs(TestData.propValue);
    assertThat("if we pass only one arg, it is assumed to mean we are specifying the 'game property'",
        System.getProperty(TRIPLEA_GAME), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsAssumedToBeMapDownloadProperty() {
    final String testUrl = "triplea:" + TestData.propValue;
    ArgParser.handleCommandLineArgs(testUrl);
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it's assumed to mean we are specifying the 'map download property'",
        System.getProperty(TRIPLEA_MAP_DOWNLOAD), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsUrlDecoded() {
    final String testUrl = "triplea:Something%20with+spaces%20and%20Special%20chars%20%F0%9F%A4%94";
    ArgParser.handleCommandLineArgs(testUrl);
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it should be properly URL-decoded as it's probably coming from a browser",
        System.getProperty(TRIPLEA_MAP_DOWNLOAD), is("Something with spaces and Special chars 🤔"));
  }

  @Test
  public void install4jSwitchesAreIgnored() {
    ArgParser.handleCommandLineArgs("-console");
  }

  @Test
  public void mapFolderOverrideClientSettingIsSetWhenSpecified() {
    ClientSetting.mapFolderOverride.save("some value");
    final String mapFolderPath = "/path/to/maps";

    ArgParser.handleCommandLineArgs("-P" + HeadlessGameServerCliParam.MAP_FOLDER.getLabel() + "=" + mapFolderPath);

    assertThat(ClientSetting.mapFolderOverride.value(), is(mapFolderPath));
  }

  @Test
  public void mapFolderOverrideClientSettingIsResetWhenNotSpecified() {
    ClientSetting.mapFolderOverride.save("some value");

    ArgParser.handleCommandLineArgs();

    assertThat(ClientSetting.mapFolderOverride.getValue(), is(ClientSetting.mapFolderOverride.getDefaultValue()));
  }

  private interface TestData {
    String propKey = "key";
    String propValue = "value";
    String[] sampleArgInput = new String[] {"-P" + propKey + "=" + propValue};
  }
}
