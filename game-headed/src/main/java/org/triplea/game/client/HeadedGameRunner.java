package org.triplea.game.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.awt.GraphicsEnvironment;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import org.triplea.common.util.Services;
import org.triplea.game.client.ui.javafx.JavaFxClientRunner;

import games.strategy.debug.Console;
import games.strategy.debug.ConsoleHandler;
import games.strategy.debug.ErrorMessage;
import games.strategy.debug.ErrorMessageHandler;
import games.strategy.engine.framework.ArgParser;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;

/**
 * Runs a headed game client.
 */
public final class HeadedGameRunner {
  private HeadedGameRunner() {}

  /**
   * Entry point for running a new headed game client.
   */
  public static void main(final String[] args) {
    checkNotNull(args);
    checkState(!GraphicsEnvironment.isHeadless(),
        "UI client launcher invoked from headless environment. This is currently prohibited by design to "
            + "avoid UI rendering errors in the headless environment.");
    ClientSetting.initialize();
    if (!ClientSetting.useExperimentalJavaFxUi.getValueOrThrow()) {
      Interruptibles.await(() -> SwingAction.invokeAndWait(() -> {
        LookAndFeel.initialize();
        initializeLogManager(Console.newInstance());
        ErrorMessage.enable();
      }));
    }
    ArgParser.handleCommandLineArgs(args);

    if (SystemProperties.isMac()) {
      MacOsIntegration.addOpenUriHandler(uri -> {
        final String encoding = StandardCharsets.UTF_8.displayName();
        try {
          final String mapName = URLDecoder.decode(
              uri.toString().substring(ArgParser.TRIPLEA_PROTOCOL.length()), encoding);
          SwingUtilities.invokeLater(() -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));
        } catch (final UnsupportedEncodingException e) {
          throw new AssertionError(encoding + " is not a supported encoding!", e);
        }
      });
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }

    if (ClientSetting.useExperimentalJavaFxUi.getValueOrThrow()) {
      Services.loadAny(JavaFxClientRunner.class).start(args);
    } else {
      GameRunner.start();
    }
  }

  private static void initializeLogManager(final Console console) {
    final Logger defaultLogger = LogManager.getLogManager().getLogger("");
    defaultLogger.addHandler(new ErrorMessageHandler());
    defaultLogger.addHandler(new ConsoleHandler(console));
  }

}
