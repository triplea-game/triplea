package org.triplea.client.launch.screens.staging.panels.playerselection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

import games.strategy.engine.data.DefaultNamed;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.ServerModel;

/**
 * View-model for the player selection UI.
 * Takes care of sending networking events, whether that is done
 * or not is transparent to the UI.
 */
public class PlayerSelectionModel {

  private final GameData gameData;
  private final String currentPlayerName;

  private final NetworkModel networkModel;

  private final Map<PlayerID, String> countryToHumanOrAiSelectionMap = new HashMap<>();
  private final Map<PlayerID, String> countryToPlayerNameMap = new HashMap<>();


  private final List<Consumer<PlayerSelectionModel>> guiListeners = new ArrayList<>();

  public PlayerSelectionModel(
      final GameData gameData,
      final String currentPlayerName,
      @Nullable final ServerModel serverModel,
      @Nullable final ClientModel clientModel) {
    this.gameData = gameData;
    this.currentPlayerName = currentPlayerName;
    this.networkModel =
        ((serverModel == null) && (clientModel == null)) ? null : buildNetworkModel(serverModel, clientModel);
  }

  private static NetworkModel buildNetworkModel(final ServerModel serverModel, final ClientModel clientModel) {
    if (serverModel != null) {
      return new NetworkModel(serverModel);
    }
    Preconditions.checkNotNull(clientModel);
    return new NetworkModel(clientModel);
  }


  List<String> getAlliances() {
    final List<String> list = new ArrayList<>(gameData.getAllianceTracker().getAlliances());
    list.sort(String::compareTo);
    return list;
  }

  List<PlayerID> getPlayersInAlliance(final String alliance) {
    final List<PlayerID> list = new ArrayList<>(gameData.getAllianceTracker().getPlayersInAlliance(alliance));
    list.sort(Comparator.comparing(DefaultNamed::getName));
    return list;
  }

  String buildSelectionIdString(final PlayerID country) {
    return gameData.getGameName() + country.getName();
  }

  String[] getPlayerSelectionTypes() {
    return new String[] {"Human", "AI"};
  }

  void addGuiListener(final Consumer<PlayerSelectionModel> listener) {
    guiListeners.add(listener);
  }

  // TODO: use enum for second arg!!!
  void updatePlayerSelection(final PlayerID country, final String selection) {
    this.countryToHumanOrAiSelectionMap.put(country, selection);

    if (networkModel != null) {
      networkModel.updateAiPlayer(country, selection);
    }
    notifyGuiListeners();
  }

  private void notifyGuiListeners() {
    System.out.println("DEBUG - notifying gui listeners");
    guiListeners.forEach(guiListener -> guiListener.accept(this));
  }

  boolean isCountryTakenByCurrentPlayer(final PlayerID country) {
    return countryToPlayerNameMap.getOrDefault(country, "").equals(getCurrentPlayerName());
  }

  String getCurrentPlayerName() {
    return currentPlayerName;
  }

  void updateCountryToEmptyPlayerSelection(final PlayerID country) {
    countryToPlayerNameMap.remove(country);
    if (networkModel != null) {
      networkModel.releaseCountry(country);
    }
  }

  boolean isCountryTakenByAnyPlayer(final PlayerID country) {
    return countryToPlayerNameMap.containsKey(country);
  }

  String getPlayerNameByCountry(final PlayerID country) {
    return countryToPlayerNameMap.getOrDefault(country, "");
  }

  void updateCountryToCurrentPlayer(final PlayerID country) {
    countryToPlayerNameMap.put(country, getCurrentPlayerName());
    if (networkModel != null) {
      networkModel.takeCountry(country, getCurrentPlayerName());
    }
  }

}
