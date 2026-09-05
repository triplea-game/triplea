package games.strategy.triplea.delegate;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

final class DieTest {
  @Nested
  final class CompressionTest {
    @Test
    void shouldBeAbleToRoundTripCompressedValue() {
      for (int i = 0; i < 254; i++) {
        for (int j = 0; j < 254; j++) {
          final Die hit = new Die(i, j, Die.DieType.MISS);
          assertThat(hit).isEqualTo(Die.getFromWriteValue(hit.getCompressedValue()));
          final Die notHit = new Die(i, j, Die.DieType.HIT);
          assertThat(notHit).isEqualTo(Die.getFromWriteValue(notHit.getCompressedValue()));
          final Die ignored = new Die(i, j, Die.DieType.IGNORED);
          assertThat(ignored).isEqualTo(Die.getFromWriteValue(ignored.getCompressedValue()));
        }
      }
    }
  }
}
