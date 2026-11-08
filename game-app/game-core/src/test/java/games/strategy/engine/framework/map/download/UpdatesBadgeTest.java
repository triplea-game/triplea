package games.strategy.engine.framework.map.download;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

final class UpdatesBadgeTest {
  @DisplayName("The badge counts the maps with updates")
  @Test
  void countsUpdates() {
    final UpdatesBadge badge = new UpdatesBadge();

    badge.showUpdates(3);

    assertThat(List.of(badge.getText(), badge.isVisible())).containsExactly("3 updates", true);
  }

  @DisplayName("One update is said in the singular")
  @Test
  void oneUpdate() {
    final UpdatesBadge badge = new UpdatesBadge();

    badge.showUpdates(1);

    assertThat(badge.getText()).isEqualTo("1 update");
  }

  @DisplayName("Without updates the badge is hidden")
  @Test
  void hiddenWithoutUpdates() {
    final UpdatesBadge badge = new UpdatesBadge();
    badge.showUpdates(2);

    badge.showUpdates(0);

    assertThat(badge.isVisible()).as("eg after the last update was installed").isFalse();
  }
}
