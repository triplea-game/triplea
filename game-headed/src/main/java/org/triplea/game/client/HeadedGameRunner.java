package org.triplea.game.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import games.strategy.debug.ErrorMessage;
import games.strategy.debug.LoggerManager;
import games.strategy.debug.console.window.ConsoleConfiguration;
import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.CliProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import java.awt.GraphicsEnvironment;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javafx.application.Application;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.game.client.ui.javafx.TripleA;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

/** Runs a headed game client. */
@Log
public final class HeadedGameRunner {
  private HeadedGameRunner() {}

  /** Entry point for running a new headed game client. */
  public static void main(final String[] args) {
    checkNotNull(args);
    checkState(
        !GraphicsEnvironment.isHeadless(),
        "UI client launcher invoked from headless environment. This is currently "
            + "prohibited by design to avoid UI rendering errors in the headless environment.");
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> log.log(Level.SEVERE, e.getLocalizedMessage(), e));

    ClientSetting.initialize();

    LoggerManager.setLogLevel(
        ClientSetting.loggingVerbosity.getValue().map(Level::parse).orElse(Level.INFO));
    Interruptibles.await(() -> SwingAction.invokeAndWait(LookAndFeel::initialize));
    if (!ClientSetting.useExperimentalJavaFxUi.getValueOrThrow()) {
      Interruptibles.await(
          () ->
              SwingAction.invokeAndWait(
                  () -> {
                    ConsoleConfiguration.initialize();
                    ErrorMessage.initialize();
                  }));
    }
    ArgParser.handleCommandLineArgs(args);

    if (SystemProperties.isMac()) {
      MacOsIntegration.addOpenUriHandler(
          uri -> {
            final String mapName =
                URLDecoder.decode(
                    uri.toString().substring(ArgParser.TRIPLEA_PROTOCOL.length()),
                    StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(
                () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));
          });
      MacOsIntegration.addOpenFilesHandler(
          list ->
              list.stream()
                  .findAny()
                  .ifPresent(
                      file -> {
                        System.setProperty(CliProperties.TRIPLEA_GAME, file.getAbsolutePath());
                        GameRunner.showMainFrame();
                      }));
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }

    if (ClientSetting.useExperimentalJavaFxUi.getValueOrThrow()) {
      Application.launch(TripleA.class, args);
    } else {
      GameRunner.start();
    }
  }
}
