package games.strategy.triplea.settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SelectionComponentUiUtilsTest {
  @Nested
  final class ToStringOfOptionalPathTest {
    @Test
    void shouldReturnAbsolutePathWhenPresent() {
      final Path path = Paths.get(".");

      assertThat(
          SelectionComponentUiUtils.toString(Optional.of(path)),
          is(path.toAbsolutePath().toString()));
    }

    @Test
    void shouldReturnEmptyStringWhenAbsent() {
      assertThat(SelectionComponentUiUtils.toString(Optional.empty()), is(emptyString()));
    }
  }
}
