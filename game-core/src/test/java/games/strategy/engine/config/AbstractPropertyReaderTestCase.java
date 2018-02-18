package games.strategy.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A fixture for testing the basic aspects of classes that implement {@link PropertyReader}.
 */
public abstract class AbstractPropertyReaderTestCase {
  private static final String ABSENT_PROPERTY_KEY = "absentKey";
  private static final String PRESENT_PROPERTY_KEY = "presentKey";
  private static final String PRESENT_PROPERTY_VALUE = "presentValue";

  private PropertyReader propertyReader;

  protected AbstractPropertyReaderTestCase() {}

  /**
   * Creates the property reader under test.
   *
   * @param properties The properties the new property reader should consider present when requested.
   *
   * @return A new property reader.
   *
   * @throws Exception If the property reader cannot be created.
   */
  protected abstract PropertyReader createPropertyReader(Map<String, String> properties) throws Exception;

  @BeforeEach
  public final void setupPropertyReader() throws Exception {
    propertyReader = createPropertyReader(Collections.singletonMap(PRESENT_PROPERTY_KEY, PRESENT_PROPERTY_VALUE));
  }

  @Test
  public final void readProperty_ShouldThrowExceptionWhenKeyIsNull() {
    assertThrows(NullPointerException.class, () -> propertyReader.readProperty(null));
  }

  @Test
  public final void readProperty_ShouldThrowExceptionWhenKeyIsEmptyOrOnlyWhitespace() {
    assertThrows(IllegalArgumentException.class, () -> propertyReader.readProperty(""));
    assertThrows(IllegalArgumentException.class, () -> propertyReader.readProperty("    "));
  }

  @Test
  public final void readProperty_ShouldReturnValueWhenKeyIsPresent() {
    assertThat(propertyReader.readProperty(PRESENT_PROPERTY_KEY), is(PRESENT_PROPERTY_VALUE));
  }

  @Test
  public final void readProperty_ShouldReturnTrimmedValueWhenKeyIsPresentAndValueHasLeadingAndTrailingWhitespace()
      throws Exception {
    final PropertyReader propertyReader = createPropertyReader(Collections.singletonMap(
        PRESENT_PROPERTY_KEY, "  " + PRESENT_PROPERTY_VALUE + "  "));

    assertThat(propertyReader.readProperty(PRESENT_PROPERTY_KEY), is(PRESENT_PROPERTY_VALUE));
  }

  @Test
  public final void readProperty_ShouldReturnEmptyWhenKeyIsAbsent() {
    assertThat(propertyReader.readProperty(ABSENT_PROPERTY_KEY), is(""));
  }
}
