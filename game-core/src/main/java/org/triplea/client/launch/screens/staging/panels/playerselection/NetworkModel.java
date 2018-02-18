package org.triplea.client.launch.screens.staging.panels.playerselection;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.ServerModel;

class NetworkModel {

  NetworkModel(final ServerModel serverModel) {}

  NetworkModel(final ClientModel clientModel) {
    // TODO implement
  }

  void releaseCountry(final PlayerID country) {
    // TODO implement
    System.out.println("release " + country);
    throw new UnsupportedOperationException("TODO");
  }

  void takeCountry(final PlayerID country, final String currentPlayerName) {
    // TODO implement
    System.out.println("take " + country + " , " + currentPlayerName);
    throw new UnsupportedOperationException("TODO");
  }

  void updateAiPlayer(final PlayerID country, final String selection) {
    // TODO implement
    throw new UnsupportedOperationException("TODO");
  }
}
