package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SelectionComponentUiUtilsTest {
  @Nested
  final class ToStringOfOptionalFileTest {
    @Test
    void shouldReturnAbsolutePathOfFileWhenPresent() {
      final File file = new File(".");

      assertThat(SelectionComponentUiUtils.toString(Optional.of(file)), is(file.getAbsolutePath()));
    }

    @Test
    void shouldReturnEmptyStringWhenAbsent() {
      assertThat(SelectionComponentUiUtils.toString(Optional.empty()), is(emptyString()));
    }
  }
}
