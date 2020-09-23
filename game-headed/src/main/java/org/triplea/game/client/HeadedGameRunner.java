package org.triplea.game.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.CliProperties;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.file.system.loader.AvailableGamesFileSystemReader;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import java.awt.GraphicsEnvironment;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.java.Log;
import org.triplea.debug.ErrorMessage;
import org.triplea.debug.LoggerManager;
import org.triplea.debug.console.window.ConsoleConfiguration;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

/** Runs a headed game client. */
@Log
public final class HeadedGameRunner {
  private HeadedGameRunner() {}

  public static void initializeClientSettingAndLogging() {
    Thread.setDefaultUncaughtExceptionHandler(
        (t, e) -> log.log(Level.SEVERE, e.getLocalizedMessage(), e));

    ClientSetting.initialize();

    LoggerManager.setLogLevel(
        ClientSetting.loggingVerbosity.getValue().map(Level::parse).orElse(Level.INFO));
  }

  public static void initializeLookAndFeel() {
    Interruptibles.await(() -> SwingAction.invokeAndWait(LookAndFeel::initialize));
  }

  public static void initializeDesktopIntegrations(final String[] args) {
    ArgParser.handleCommandLineArgs(args);

    if (SystemProperties.isMac()) {
      MacOsIntegration.setOpenUriHandler(
          uri -> {
            final String mapName =
                URLDecoder.decode(
                    uri.toString().substring(ArgParser.TRIPLEA_PROTOCOL.length()),
                    StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(
                () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));
          });
      MacOsIntegration.setOpenFileHandler(
          file -> {
            SwingUtilities.invokeLater(
                () ->
                    JOptionPane.showMessageDialog(
                        null,
                        "Unfortunately opening save-games via the OS"
                            + " is currently not supported on macOS.",
                        "Unsupported feature",
                        JOptionPane.INFORMATION_MESSAGE));
            System.setProperty(CliProperties.TRIPLEA_GAME, file.getAbsolutePath());
            GameRunner.showMainFrame();
          });
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }
  }

  /** Entry point for running a new headed game client. */
  public static void main(final String[] args) {
    checkNotNull(args);
    checkState(
        !GraphicsEnvironment.isHeadless(),
        "UI client launcher invoked from headless environment. This is currently "
            + "prohibited by design to avoid UI rendering errors in the headless environment.");
    initializeClientSettingAndLogging();
    initializeLookAndFeel();

    initializeDesktopIntegrations(args);
    SwingUtilities.invokeLater(ConsoleConfiguration::initialize);
    SwingUtilities.invokeLater(ErrorMessage::initialize);
    GameRunner.start();
    AvailableGamesFileSystemReader.refreshMapFileCache();
  }
}
