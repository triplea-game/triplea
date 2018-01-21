package games.strategy.engine.framework.startup.launcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.local.PlayerCountrySelection;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.random.PlainRandomSource;

public class LauncherFactory {

  public static ILauncher getLocalLaunchers(
      final GameSelectorModel gameSelectorModel,
      final List<? extends PlayerCountrySelection> playerRows) {

    final Map<String, String> playerTypes = playerRows.stream()
        .collect(Collectors.toMap(PlayerCountrySelection::getPlayerName, PlayerCountrySelection::getPlayerType));

    final Map<String, Boolean> playersEnabled = playerRows.stream()
        .collect(Collectors.toMap(PlayerCountrySelection::getPlayerName, PlayerCountrySelection::isPlayerEnabled));

    // we don't need the playerToNode list, the disable-able players, or the alliances
    // list, for a local game
    final PlayerListing pl =
        new PlayerListing(null, playersEnabled, playerTypes, gameSelectorModel.getGameData().getGameVersion(),
            gameSelectorModel.getGameName(), gameSelectorModel.getGameRound(), null, null);
    return new LocalLauncher(gameSelectorModel, new PlainRandomSource(), pl);
  }
}
