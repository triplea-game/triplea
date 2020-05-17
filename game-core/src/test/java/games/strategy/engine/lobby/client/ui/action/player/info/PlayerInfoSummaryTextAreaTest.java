package games.strategy.engine.lobby.client.ui.action.player.info;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

class PlayerInfoSummaryTextAreaTest {

  @Test
  void buildPlayerSummaryWithoutModeratorOnlyData() {
    final PlayerSummary playerSummary = PlayerSummary.builder().name("player-name").build();

    final String result = PlayerInfoSummaryTextArea.buildPlayerInfoText(playerSummary);

    assertThat(result, is("player-name"));
  }

  @Test
  void buildPlayerSummaryWithFullData() {
    final PlayerSummary playerSummary =
        PlayerSummary.builder().name("player-name").ip("3.3.3.3").systemId("system-id").build();

    final String result = PlayerInfoSummaryTextArea.buildPlayerInfoText(playerSummary);

    assertThat(result, containsString("player-name"));
    assertThat(result, containsString("IP: 3.3.3.3"));
    assertThat(result, containsString("System ID: system-id"));
  }
}
