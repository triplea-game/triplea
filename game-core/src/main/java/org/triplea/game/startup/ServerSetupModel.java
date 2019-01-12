package org.triplea.game.startup;

import games.strategy.engine.framework.startup.mc.ServerModel;

public interface ServerSetupModel {
  void showSelectType();

  boolean isHeadless();

  void onServerMessengerCreated(final ServerModel serverSetupModel);
}
