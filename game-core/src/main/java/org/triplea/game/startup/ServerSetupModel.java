package org.triplea.game.startup;

import games.strategy.engine.framework.startup.mc.ServerModel;
import javax.annotation.Nullable;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;

/** Interface to abstract common functionality on server game creation into a unified interface. */
public interface ServerSetupModel {
  void showSelectType();

  void onServerMessengerCreated(
      ServerModel serverModel, @Nullable GameHostingResponse gameHostingResponse);
}
