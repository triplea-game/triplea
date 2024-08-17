package org.triplea.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** A fixture for testing the basic aspects of classes that implement {@link PropertyReader}. */
public abstract class AbstractPropertyReaderTestCase {
  @NonNls private static final String ABSENT_PROPERTY_KEY = "absentKey";
  @NonNls private static final String PRESENT_PROPERTY_KEY = "presentKey";

  protected AbstractPropertyReaderTestCase() {}

  /**
   * Creates the property reader under test.
   *
   * @param properties The properties the new property reader should consider present when
   *     requested.
   * @return A new property reader.
   * @throws Exception If the property reader cannot be created.
   */
  protected abstract PropertyReader newPropertyReader(Map<String, String> properties)
      throws Exception;

  private PropertyReader newEmptyPropertyReader() throws Exception {
    return newPropertyReader(Map.of());
  }

  private PropertyReader newSingletonPropertyReader(final String value) throws Exception {
    return newPropertyReader(Map.of(PRESENT_PROPERTY_KEY, value));
  }

  /** Test cases for {@link PropertyReader#readProperty(String)}. */
  @Nested
  public final class ReadPropertyTest {
    @NonNls private static final String PRESENT_PROPERTY_VALUE = "presentValue";

    private PropertyReader propertyReader;

    @BeforeEach
    public void setupPropertyReader() throws Exception {
      propertyReader = newSingletonPropertyReader(PRESENT_PROPERTY_VALUE);
    }

    @Test
    public void shouldThrowExceptionWhenKeyIsNull() {
      assertThrows(NullPointerException.class, () -> propertyReader.readProperty(null));
    }

    @Test
    public void shouldThrowExceptionWhenKeyIsEmptyOrOnlyWhitespace() {
      assertThrows(IllegalArgumentException.class, () -> propertyReader.readProperty(""));
      assertThrows(IllegalArgumentException.class, () -> propertyReader.readProperty("    "));
    }

    @Test
    public void shouldReturnValueWhenKeyIsPresent() {
      assertThat(propertyReader.readProperty(PRESENT_PROPERTY_KEY), is(PRESENT_PROPERTY_VALUE));
    }

    @Test
    public void shouldReturnTrimmedValueWhenKeyIsPresentAndValueHasLeadingAndTrailingWhitespace()
        throws Exception {
      final PropertyReader propertyReader =
          newSingletonPropertyReader("  " + PRESENT_PROPERTY_VALUE + "  ");

      assertThat(propertyReader.readProperty(PRESENT_PROPERTY_KEY), is(PRESENT_PROPERTY_VALUE));
    }

    @Test
    public void shouldReturnEmptyWhenKeyIsAbsent() {
      assertThat(propertyReader.readProperty(ABSENT_PROPERTY_KEY), is(emptyString()));
    }
  }

  /** Test cases for {@link PropertyReader#readPropertyOrDefault(String, String)}. */
  @Nested
  public final class ReadPropertyOrDefaultTest {
    @Test
    public void shouldReturnValueWhenKeyIsPresent() throws Exception {
      final String value = "value";
      final PropertyReader propertyReader = newSingletonPropertyReader(value);

      assertThat(
          propertyReader.readPropertyOrDefault(PRESENT_PROPERTY_KEY, "defaultValue"), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyIsAbsent() throws Exception {
      final String defaultValue = "defaultValue";
      final PropertyReader propertyReader = newEmptyPropertyReader();

      assertThat(
          propertyReader.readPropertyOrDefault(ABSENT_PROPERTY_KEY, defaultValue),
          is(defaultValue));
    }
  }

  /** Test cases for {@link PropertyReader#readBooleanPropertyOrDefault(String, boolean)}. */
  @Nested
  public final class ReadBooleanPropertyOrDefaultTest {
    @Test
    public void shouldReturnValueWhenKeyIsPresent() throws Exception {
      final boolean value = true;
      final PropertyReader propertyReader = newSingletonPropertyReader(String.valueOf(value));

      assertThat(
          propertyReader.readBooleanPropertyOrDefault(PRESENT_PROPERTY_KEY, false), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyIsAbsent() throws Exception {
      final boolean defaultValue = true;
      final PropertyReader propertyReader = newEmptyPropertyReader();

      assertThat(
          propertyReader.readBooleanPropertyOrDefault(ABSENT_PROPERTY_KEY, defaultValue),
          is(defaultValue));
    }
  }

  /** Test cases for {@link PropertyReader#readIntegerPropertyOrDefault(String, int)}. */
  @Nested
  public final class ReadIntegerPropertyOrDefaultTest {
    @Test
    public void shouldReturnValueWhenKeyIsPresent() throws Exception {
      final int value = 42;
      final PropertyReader propertyReader = newSingletonPropertyReader(String.valueOf(value));

      assertThat(propertyReader.readIntegerPropertyOrDefault(PRESENT_PROPERTY_KEY, -1), is(value));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyIsPresentAndValueIsNotAnInteger() throws Exception {
      final int defaultValue = 777;
      final PropertyReader propertyReader = newSingletonPropertyReader("other");

      assertThat(
          propertyReader.readIntegerPropertyOrDefault(PRESENT_PROPERTY_KEY, defaultValue),
          is(defaultValue));
    }

    @Test
    public void shouldReturnDefaultValueWhenKeyIsAbsent() throws Exception {
      final int defaultValue = 777;
      final PropertyReader propertyReader = newEmptyPropertyReader();

      assertThat(
          propertyReader.readIntegerPropertyOrDefault(ABSENT_PROPERTY_KEY, defaultValue),
          is(defaultValue));
    }
  }
}
