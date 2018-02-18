package org.triplea.client.launch.screens;

import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.JComponent;

/**
 * Enumeration of the various setup screen configurations leading up to the 'game staging screen'.
 */
public enum LaunchScreen {
  INITIAL(InitialLaunchScreen::build),

  NETWORK_GAME_TYPE_SELECT(INITIAL, NetworkGameTypeSelectionScreen::build),

  JOIN_NETWORK_GAME_OPTIONS(NETWORK_GAME_TYPE_SELECT, JoinNetworkGameSetup::build),

  HOST_NETWORK_GAME_OPTIONS(NETWORK_GAME_TYPE_SELECT, HostNetworkGameSetup::build),

  PLAY_BY_FORUM_OPTIONS(NETWORK_GAME_TYPE_SELECT, PlayByForumSetup::build),

  PLAY_BY_EMAIL_OPTIONS(NETWORK_GAME_TYPE_SELECT, PlayByEmailSetup::build);


  public final LaunchScreen previousScreen;

  private final Supplier<JComponent> windowBuilder;

  LaunchScreen(final Supplier<JComponent> windowBuilder) {
    this(null, windowBuilder);
  }

  LaunchScreen(final LaunchScreen previousScreen, final Supplier<JComponent> windowBuilder) {
    this.previousScreen = previousScreen;
    this.windowBuilder = windowBuilder;
  }

  public Optional<LaunchScreen> getPreviousScreen() {
    return Optional.ofNullable(previousScreen);
  }

  public LaunchScreen getPreviousScreenNotOptional() {
    return getPreviousScreen()
        .orElseThrow(() -> new IllegalStateException("Previous screen is null for enum value: " + name()));
  }

  public JComponent buildScreen() {
    return windowBuilder.get();
  }
}
