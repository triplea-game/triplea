package org.triplea.game.startup;

import games.strategy.engine.framework.startup.mc.ServerModel;

/** Interface to abstract common functionality on server game creation into a unified interface. */
public interface ServerSetupModel {
  void showSelectType();

  void onServerMessengerCreated(ServerModel serverModel);
}
