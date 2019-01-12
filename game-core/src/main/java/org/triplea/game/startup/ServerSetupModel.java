package org.triplea.game.startup;

import games.strategy.engine.framework.startup.mc.ServerModel;

public interface ServerSetupModel {
  void showSelectType();

  void onServerMessengerCreated(final ServerModel serverSetupModel);
}
