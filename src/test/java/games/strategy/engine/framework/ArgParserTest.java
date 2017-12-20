package games.strategy.engine.framework;

import static games.strategy.engine.framework.ArgParser.CliProperties.MAP_FOLDER;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

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

    final boolean result = new ArgParser(TestData.samplePropertyNameSet).handleCommandLineArgs(TestData.sampleArgInput);

    assertThat("prop key was supplied as an available value, "
        + " which was passed as a test value - everything should "
        + "have parsed well, expect true result",
        result, is(true));
    assertThat("system property should now be set to our test value",
        System.getProperty(TestData.propKey), is(TestData.propValue));
  }

  @Test
  public void emptySystemPropertiesCanBeSet() {
    new ArgParser(Collections.singleton("a")).handleCommandLineArgs(new String[] {"-Pa="});
    assertThat("expecting the system property to be empty string instead of null",
        System.getProperty("a"), is(""));
  }

  @Test
  public void singleFileArgIsAssumedToBeGameProperty() {
    new ArgParser(Collections.singleton(TRIPLEA_GAME))
        .handleCommandLineArgs(new String[] {TestData.propValue});
    assertThat("if we pass only one arg, it is assumed to mean we are specifying the 'game property'",
        System.getProperty(TRIPLEA_GAME), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsAssumedToBeMapDownloadProperty() {
    final String testUrl = "triplea:" + TestData.propValue;
    new ArgParser(Collections.singleton(TRIPLEA_MAP_DOWNLOAD))
        .handleCommandLineArgs(new String[] {testUrl});
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it's assumed to mean we are specifying the 'map download property'",
        System.getProperty(TRIPLEA_MAP_DOWNLOAD), is(TestData.propValue));
  }

  @Test
  public void singleUrlArgIsUrlDecoded() {
    final String testUrl = "triplea:Something%20with+spaces%20and%20Special%20chars%20%F0%9F%A4%94";
    new ArgParser(Collections.singleton(TRIPLEA_MAP_DOWNLOAD))
        .handleCommandLineArgs(new String[] {testUrl});
    assertThat("if we pass only one arg prefixed with 'triplea:',"
        + " it should be properly URL-decoded as it's probably coming from a browser",
        System.getProperty(TRIPLEA_MAP_DOWNLOAD), is("Something with spaces and Special chars 🤔"));
  }

  @Test
  public void install4jSwitchesAreIgnored() {
    assertThat(new ArgParser(Collections.emptySet()).handleCommandLineArgs(new String[] {"-console"}), is(true));
  }

  @Test
  public void returnFalseIfWeCannotMapKeysToAvailableSet() {
    final Set<String> validKeys = new HashSet<>(Arrays.asList("a", "b"));
    Arrays.asList(
        new String[] {"-PnotMapped="},
        new String[] {"-PnotMapped=test"},
        new String[] {"-PnotMapped=test", "-Pa=valid"},
        new String[] {"-Pa=valid", "-PnotMapped=test"},
        new String[] {"-Pa=valid", "-PnotMapped=test", "-Pb=valid"},
        new String[] {"-Pa=valid", "-Pb=valid", "-PnotMapped=test"})
        .forEach(invalidInput -> assertThat("A key in the input is not in the valid key set, expecting this to be seen"
            + "as invalid: " + Arrays.asList(invalidInput),
            new ArgParser(validKeys).handleCommandLineArgs(invalidInput), is(false)));
  }

  @Test
  public void mapFolderOverrideClientSettingIsSetWhenSpecified() {
    ClientSetting.MAP_FOLDER_OVERRIDE.save("some value");
    final String mapFolderPath = "/path/to/maps";

    new ArgParser(Collections.singleton(MAP_FOLDER))
        .handleCommandLineArgs(new String[] {"-PmapFolder=" + mapFolderPath});

    assertThat(ClientSetting.MAP_FOLDER_OVERRIDE.value(), is(mapFolderPath));
  }

  @Test
  public void mapFolderOverrideClientSettingIsResetWhenNotSpecified() {
    ClientSetting.MAP_FOLDER_OVERRIDE.save("some value");

    new ArgParser(Collections.emptySet()).handleCommandLineArgs(new String[0]);

    assertThat(ClientSetting.MAP_FOLDER_OVERRIDE.value(), is(ClientSetting.MAP_FOLDER_OVERRIDE.defaultValue));
  }

  private interface TestData {
    String propKey = "key";
    String propValue = "value";
    String[] sampleArgInput = new String[] {"-P" + propKey + "=" + propValue};
    Set<String> samplePropertyNameSet = Collections.singleton(propKey);
  }
}
