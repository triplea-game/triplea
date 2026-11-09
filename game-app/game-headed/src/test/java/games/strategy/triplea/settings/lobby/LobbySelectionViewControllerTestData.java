package games.strategy.triplea.settings.lobby;

import lombok.experimental.UtilityClass;

@UtilityClass
class LobbySelectionViewControllerTestData {
  static final LobbySelectionViewData lobbySelectionViewData =
      LobbySelectionViewData.builder()
          .selectedRadioButton(LobbySelectionViewData.LobbyChoice.LOCAL)
          .uriFieldEnabled(true)
          .uriFieldValue("")
          .build();
}
