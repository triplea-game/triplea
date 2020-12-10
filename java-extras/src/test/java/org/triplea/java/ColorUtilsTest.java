package org.triplea.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Color;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ColorUtilsTest {

  @Test
  void verifyColorConstruction() {
    assertThat(ColorUtils.fromHexString("000000"), is(Color.BLACK));
    assertThat(ColorUtils.fromHexString("FFFFFF"), is(Color.WHITE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "12345", "1234567", "ZZ00ZZ"})
  void illegalArgsWillThrow(final String illegalValue) {
    assertThrows(IllegalArgumentException.class, () -> ColorUtils.fromHexString(illegalValue));
  }
}
