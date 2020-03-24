package org.triplea.modules.game;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Instantiates controller with dependencies. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ConnectivityControllerFactory {
  public static ConnectivityController buildController() {
    return ConnectivityController.builder().connectivityCheck(new ConnectivityCheck()).build();
  }
}
