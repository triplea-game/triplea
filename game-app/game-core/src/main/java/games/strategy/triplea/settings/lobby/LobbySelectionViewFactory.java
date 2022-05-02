package games.strategy.triplea.settings.lobby;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LobbySelectionViewFactory {

  /**
   * @see LobbySelectionView
   */
  public static LobbySelectionView build() {
    final LobbySelectionSwingEventQueue lobbySelectionEventQueue =
        new LobbySelectionSwingEventQueue();

    final LobbySelectionView lobbySelectionView = new LobbySelectionView(lobbySelectionEventQueue);
    lobbySelectionEventQueue.addView(lobbySelectionView);

    final LobbySelectionViewController lobbySelectionViewController =
        new LobbySelectionViewController(lobbySelectionEventQueue);
    lobbySelectionEventQueue.addController(lobbySelectionViewController);

    return lobbySelectionView;
  }
}
