package org.triplea.client.launch.screens.staging;

/**
 * Marker interface for listeners of the 'game started' event. Notably used by network hosts to send the
 * 'game start' event to network clients.
 */
@FunctionalInterface
public interface GameStartedCallback {
  void gameIsStarted();
}
