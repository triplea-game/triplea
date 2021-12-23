package games.strategy.triplea.settings.lobby;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.lobby.LobbySelectionViewData.LobbyChoice;
import lombok.AllArgsConstructor;
import org.triplea.java.exception.UnhandledSwitchCaseException;
import org.triplea.ui.events.queue.ViewClassController;

@AllArgsConstructor
class LobbySelectionViewController
    implements ViewClassController<LobbySelectionView.Events, LobbySelectionViewData> {
  enum Events {
    UPDATE_UI_DATA,
  }

  private final LobbySelectionSwingEventQueue lobbySelectionEventQueue;

  /**
   * In response to any UI event we will update the UI to match the appropriate lobby URI selection.
   */
  @Override
  public void handleEvent(
      final LobbySelectionView.Events viewEvent, final LobbySelectionViewData viewData) {
    lobbySelectionEventQueue.publishControllerEvent(
        Events.UPDATE_UI_DATA,
        ui ->
            determineLobbyChoiceToSelect(viewEvent, viewData) //
                .toLobbySelectionViewData());
  }

  private LobbyChoice determineLobbyChoiceToSelect(
      final LobbySelectionView.Events viewEvent, final LobbySelectionViewData viewData) {
    switch (viewEvent) {
      case RADIO_BUTTON_CHANGED:
        return viewData.getSelectedRadioButton();
      case RESET_TO_DEFAULT:
        return LobbyChoice.fromUri(ClientSetting.lobbyUri.getDefaultValue().orElseThrow());
      case RESET:
        return LobbyChoice.fromUri(ClientSetting.lobbyUri.getValueOrThrow());
      default:
        throw new UnhandledSwitchCaseException(viewEvent);
    }
  }
}
