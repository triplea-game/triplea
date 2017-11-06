package games.strategy.engine.framework;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import games.strategy.triplea.settings.ClientSetting;

public class ArgParserTest {


  @AfterEach
  public void teardown() {
    System.clearProperty(GameRunner.TRIPLEA_GAME_PROPERTY);
  }

  @Test
  public void argsTurnIntoSystemProps() {
    assertThat("check precondition, system property for our test key should not be set yet.",
        System.getProperty(TestData.propKey), nullValue());

    final boolean result = ArgParser.handleCommandLineArgs(
        TestData.sampleArgInput, TestData.samplePropertyNameSet);

    assertThat("prop key was supplied as an available value, "
        + " which was passed as a test value - everything should "
        + "have parsed well, expect true result",
        result, is(true));
    assertThat("system property should now be set to our test value",
        System.getProperty(TestData.propKey), is(TestData.propValue));
  }

  @Test
  public void emptySystemPropertiesCanBeSet() {
    ArgParser.handleCommandLineArgs(new String[] {"a="}, new String[] {"a"});
    assertThat("expecting the system property to be empty string instead of null",
        System.getProperty("a"), is(""));
  }

  @Test
  public void malformedInputThrowsException() {
    Arrays.asList(
        new String[] {"=a"}, // no key
        new String[] {"="},
        new String[] {"a=b", "a"},
        new String[] {"a=b", " "})
        .forEach(invalidInput -> {
          assertThrows(
              IllegalArgumentException.class,
              () -> ArgParser.handleCommandLineArgs(invalidInput, new String[] {"a"}),
              Arrays.toString(invalidInput));
        });
  }

  @Test
  public void singleFileArgIsAssumedToBeGameProperty() {
    ArgParser.handleCommandLineArgs(new String[] {TestData.propValue}, new String[] {GameRunner.TRIPLEA_GAME_PROPERTY});
    assertThat("if we pass only one arg, it is assumed to mean we are specifying the 'game property'",
        System.getProperty(GameRunner.TRIPLEA_GAME_PROPERTY), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsAssumedToBeMapDownloadProperty() {
    final String testUrl = "triplea:" + TestData.propValue;
    ArgParser.handleCommandLineArgs(new String[] {testUrl}, new String[] {GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY});
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it's assumed to mean we are specifying the 'map download property'",
        System.getProperty(GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsUrlDecoded() {
    final String testUrl = "triplea:Something%20with+spaces%20and%20Special%20chars%20%F0%9F%A4%94";
    ArgParser.handleCommandLineArgs(new String[] {testUrl}, new String[] {GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY});
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it should be properly URL-decoded as it's probably coming from a browser",
        System.getProperty(GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY), is("Something with spaces and Special chars 🤔"));
  }

  @Test
  public void commandLineSwitchesAreIgnored() {
    assertThat(ArgParser.handleCommandLineArgs(new String[] {"-console"}, new String[] {}), is(true));
  }

  @Test
  public void returnFalseIfWeCannotMapKeysToAvailableSet() {
    final String[] validKeys = {"a", "b"};
    Arrays.asList(
        new String[] {"notMapped="},
        new String[] {"notMapped=test"},
        new String[] {"notMapped=test", "a=valid"},
        new String[] {"a=valid", "notMapped=test"},
        new String[] {"a=valid", "notMapped=test", "b=valid"},
        new String[] {"a=valid", "b=valid", "notMapped=test"})
        .forEach(invalidInput -> assertThat("A key in the input is not in the valid key set, expecting this to be seen"
            + "as invalid: " + Arrays.asList(invalidInput),
            ArgParser.handleCommandLineArgs(invalidInput, validKeys), is(false)));
  }

  @Test
  public void mapFolderOverrideClientSettingIsSetWhenSpecified() {
    ClientSetting.MAP_FOLDER_OVERRIDE.save("some value");
    final String mapFolderPath = "/path/to/maps";

    ArgParser.handleCommandLineArgs(new String[] {"mapFolder=" + mapFolderPath}, new String[] {GameRunner.MAP_FOLDER});

    assertThat(ClientSetting.MAP_FOLDER_OVERRIDE.value(), is(mapFolderPath));
  }

  @Test
  public void mapFolderOverrideClientSettingIsResetWhenNotSpecified() {
    ClientSetting.MAP_FOLDER_OVERRIDE.save("some value");

    ArgParser.handleCommandLineArgs(new String[0], new String[0]);

    assertThat(ClientSetting.MAP_FOLDER_OVERRIDE.value(), is(ClientSetting.MAP_FOLDER_OVERRIDE.defaultValue));
  }

  private interface TestData {
    String propKey = "key";
    String propValue = "value";
    String[] sampleArgInput = new String[] {propKey + "=" + propValue};
    String[] samplePropertyNameSet = new String[] {propKey};
  }
}
