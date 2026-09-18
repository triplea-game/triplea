package games.strategy.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class IServerMessengerTest {
  @Nested
  final class GetRealNameTest {
    @Test
    void shouldReturnNameUnchangedWhenSuffixAbsent() {
      assertThat(IServerMessenger.getRealName("name")).isEqualTo("name");
    }

    @Test
    void shouldReturnNameWithoutSuffixWhenSuffixPresent() {
      assertThat(IServerMessenger.getRealName("name (1)")).isEqualTo("name");
    }
  }
}
