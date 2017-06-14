package games.strategy.engine.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

public class MaxMemorySettingTest {
  @Test
  public void contractTests() {
    assertThat(MaxMemorySetting.of("1").isSet, is(true));
    assertThat(MaxMemorySetting.NOT_SET.isSet, is(false));
  }

  @Test
  public void decimalValuesTruncatedToIntegerValues() {
    assertThat(MaxMemorySetting.of("123.3").value, is(123L));
  }

  @Test
  public void verifyInputParsing() {
    Arrays.asList(
        "",
        "NaN",
        "-",
        "-0.1",
        "-1"
    ).forEach(invalidValue -> {
      try {
        MaxMemorySetting.of(invalidValue);
        fail(invalidValue + ", was expected to trigger an illegal arg exception");
      } catch (final IllegalArgumentException expected) {
        // expected
      }
    });
  }
}
