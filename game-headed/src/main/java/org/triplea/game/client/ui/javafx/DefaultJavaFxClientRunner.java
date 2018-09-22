package org.triplea.game.client.ui.javafx;

import javafx.application.Application;

/**
 * Default implementation of the {@link JavaFxClientRunner} service.
 */
public final class DefaultJavaFxClientRunner implements JavaFxClientRunner {
  @Override
  public void start(final String[] args) {
    Application.launch(TripleA.class, args);
  }
}
