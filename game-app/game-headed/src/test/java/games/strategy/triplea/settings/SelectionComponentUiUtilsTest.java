package games.strategy.triplea.settings;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class SelectionComponentUiUtilsTest {
  @Nested
  final class ToStringOfOptionalPathTest {
    @Test
    void shouldReturnAbsolutePathWhenPresent() {
      final Path path = Path.of(".");

      assertThat(SelectionComponentUiUtils.toString(Optional.of(path)))
          .isEqualTo(path.toAbsolutePath().toString());
    }

    @Test
    void shouldReturnEmptyStringWhenAbsent() {
      assertThat(SelectionComponentUiUtils.toString(Optional.empty())).isEmpty();
    }
  }
}
