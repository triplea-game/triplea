package games.strategy.net;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class IServerMessengerTest {
  @Nested
  final class GetRealNameTest {
    @Test
    void shouldReturnNameUnchangedWhenSuffixAbsent() {
      assertThat(IServerMessenger.getRealName("name"), is("name"));
    }

    @Test
    void shouldReturnNameWithoutSuffixWhenSuffixPresent() {
      assertThat(IServerMessenger.getRealName("name (1)"), is("name"));
    }
  }
}
