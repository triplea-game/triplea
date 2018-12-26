package games.strategy.triplea.ui;

import static games.strategy.triplea.ui.MacOsIntegration.isJavaVersionAtLeast9;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class MacOsIntegrationTest {
  @Nested
  final class IsJavaVersionAtLeast9Test {
    @Test
    void shouldReturnTrueWhenJavaVersionIs9OrLater() {
      Arrays.asList("9", "10", "11", "12")
          .forEach(specificationVersion -> assertThat(isJavaVersionAtLeast9(specificationVersion), is(true)));
    }

    @Test
    void shouldReturnFalseWhenJavaVersionIsMalformed() {
      assertThat(isJavaVersionAtLeast9(""), is(false));
    }

    @Test
    void shouldReturnFalseWhenJavaVersionIs8() {
      assertThat(isJavaVersionAtLeast9("1.8"), is(false));
    }
  }
}
