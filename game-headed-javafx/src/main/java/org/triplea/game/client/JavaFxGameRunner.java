package org.triplea.game.client;

import org.triplea.debug.ErrorMessage;
import org.triplea.debug.console.window.ConsoleConfiguration;
import org.triplea.game.client.ui.javafx.TripleA;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

import javafx.application.Application;
import lombok.extern.java.Log;

@Log
public class JavaFxGameRunner {

  private JavaFxGameRunner() {}

  public static void main(final String[] args) {
    HeadedGameRunner.initializeClientSettingAndLogging();
    HeadedGameRunner.initializeLookAndFeel();
    Interruptibles.await(
        () ->
            SwingAction.invokeAndWait(
                () -> {
                  ConsoleConfiguration.initialize();
                  ErrorMessage.initialize();
                }));
    HeadedGameRunner.initializeDesktopIntegrations(args);

    Application.launch(TripleA.class, args);
  }
}
