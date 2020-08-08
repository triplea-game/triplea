package games.strategy.engine.lobby.client.ui.action.player.info;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.moderator.PlayerSummary;

class PlayerInfoSummaryTextAreaTest {

  @Test
  void buildPlayerSummaryWithoutModeratorOnlyData() {
    final PlayerSummary playerSummary = PlayerSummary.builder().build();

    final String result = PlayerInfoSummaryTextArea.buildPlayerInfoText(playerSummary);

    assertThat(result, is("Not registered"));
  }

  @Test
  void buildPlayerSummaryWithFullData() {
    final PlayerSummary playerSummary =
        PlayerSummary.builder()
            .ip("3.3.3.3")
            .systemId("system-id")
            .registrationDateEpochMillis(600L)
            .build();

    final String result = PlayerInfoSummaryTextArea.buildPlayerInfoText(playerSummary);

    assertThat(result, containsString("IP: 3.3.3.3"));
    assertThat(result, containsString("System ID: system-id"));
    assertThat(result, containsString("Registered on:"));
  }
}
