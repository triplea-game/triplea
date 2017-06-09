package games.strategy.engine.framework;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.After;
import org.junit.Test;

public class ArgParserTest {


  @After
  public void teardown() {
    System.clearProperty(GameRunner.TRIPLEA_GAME_PROPERTY);
  }

  @Test
  public void argsTurnIntoSystemProps() {
    assertThat("check precondition, system property for our test key should not be set yet.",
        System.getProperty(TestData.propKey), nullValue());

    boolean result = ArgParser.handleCommandLineArgs(
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
          try {
            ArgParser.handleCommandLineArgs(invalidInput, new String[] {"a"});
            fail("Did not throw an exception as expected on input: " + Arrays.asList(invalidInput));
          } catch (IllegalArgumentException expected) {
            // expected
          }
        });
  }

  @Test
  public void singleArgIsAssumedToBeGameProperty() {
    ArgParser.handleCommandLineArgs(new String[] {TestData.propValue}, new String[] {GameRunner.TRIPLEA_GAME_PROPERTY});
    assertThat("if we pass only one arg, it is assumed to mean we are specifying the 'game property'",
        System.getProperty(GameRunner.TRIPLEA_GAME_PROPERTY), is(TestData.propValue));
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
        .forEach(invalidInput ->
            assertThat("A key in the input is not in the valid key set, expecting this to be seen"
                    + "as invalid: " + Arrays.asList(invalidInput),
                ArgParser.handleCommandLineArgs(invalidInput, validKeys), is(false)));
  }


  private interface TestData {
    String propKey = "key";
    String propValue = "value";
    String[] sampleArgInput = new String[] {propKey + "=" + propValue};
    String[] samplePropertyNameSet = new String[] {propKey};
  }
}
