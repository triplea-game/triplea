package org.triplea.game.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_MAP_DOWNLOAD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_MAP_DOWNLOAD_PREFIX;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_START;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_START_LOBBY;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_START_LOCAL;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_START_PBEM;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_START_PBF;
import static games.strategy.triplea.Constants.PROPERTY_FALSE;
import static games.strategy.triplea.Constants.PROPERTY_TRUE;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.auto.update.UpdateChecks;
import games.strategy.engine.framework.GameDataFileUtils;
import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.engine.framework.I18nResourceBundle;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.file.system.loader.ZippedMapsExtractor;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.engine.framework.startup.ui.panels.main.HeadedServerSetupModel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.framework.ui.MainFrame;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.MacOsIntegration;
import games.strategy.ui.Util;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.debug.ErrorMessage;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.LobbyHttpClientConfig;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;
import org.triplea.map.description.file.MapDescriptionYamlGeneratorRunner;
import org.triplea.swing.SwingAction;
import org.triplea.util.ExitStatus;

/** Runs a headed game client. */
@Slf4j
public final class HeadedGameRunner {
  private static final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private static HeadedServerSetupModel headedServerSetupModel;

  private HeadedGameRunner() {}

  public static void initializeClientSettingAndLogging() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error(e.getLocalizedMessage(), e));
    final Locale defaultLocale = Locale.getDefault();
    if (!I18nResourceBundle.getMapSupportedLocales().contains(defaultLocale)) {
      Locale.setDefault(Locale.US);
    }
    ClientSetting.initialize();
  }

  public static void initializeLookAndFeel() {
    Interruptibles.await(() -> SwingAction.invokeAndWait(LookAndFeel::initialize));
  }

  public static void initializeDesktopIntegrations(final String[] args) {
    if (args.length == 1) { // simple case with one argument only
      if (args[0].startsWith(TRIPLEA_MAP_DOWNLOAD_PREFIX)) {
        final String value =
            URLDecoder.decode(
                args[0].substring(TRIPLEA_MAP_DOWNLOAD_PREFIX.length()), StandardCharsets.UTF_8);
        System.setProperty(TRIPLEA_MAP_DOWNLOAD, value);
      } else if (!args[0].contains("=")) {
        System.setProperty(TRIPLEA_GAME, args[0]);
      } else {
        setPropertiesFromArgs(args);
      }
    } else {
      setPropertiesFromArgs(args);
    }

    if (SystemProperties.isMac()) {
      MacOsIntegration.setOpenUriHandler(
          uri -> {
            final String mapName =
                URLDecoder.decode(
                    uri.toString().substring(TRIPLEA_MAP_DOWNLOAD_PREFIX.length()),
                    StandardCharsets.UTF_8);
            SwingUtilities.invokeLater(
                () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(mapName));
          });
      MacOsIntegration.setOpenFileHandler(MainFrame::loadSaveFile);
    }

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }
  }

  private static void setPropertiesFromArgs(String[] args) {
    for (String arg : args) {
      String[] nameValuePair = arg.split("=");
      if (nameValuePair.length != 2) {
        log.warn("Property '{}' not matching pattern '<name>=<value>'.", arg);
        continue;
      }
      switch (nameValuePair[0]) {
        case TRIPLEA_GAME:
        case TRIPLEA_SERVER:
        case TRIPLEA_CLIENT:
        case TRIPLEA_START:
          System.setProperty(nameValuePair[0], nameValuePair[1]);
          break;
        default:
          log.warn("Property '{}' not unknown.", nameValuePair[0]);
      }
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

    LobbyHttpClientConfig.setConfig(
        LobbyHttpClientConfig.builder()
            .clientVersion(ProductVersionReader.getCurrentVersion().toMajorMinorString())
            .systemId(SystemIdLoader.load().getValue())
            .build());

    initializeDesktopIntegrations(args);
    SwingUtilities.invokeLater(ErrorMessage::initialize);

    ZippedMapsExtractor.builder()
        .downloadedMapsFolder(ClientFileSystemHelper.getUserMapsFolder())
        .progressIndicator(
            unzipTask -> BackgroundTaskRunner.runInBackground("Unzipping map files", unzipTask))
        .build()
        .unzipMapFiles();

    MapDescriptionYamlGeneratorRunner.builder()
        .downloadedMapsFolder(ClientFileSystemHelper.getUserMapsFolder())
        .progressIndicator(
            unzipTask ->
                BackgroundTaskRunner.runInBackground("Generating map descriptor files", unzipTask))
        .build()
        .generateYamlFiles();

    log.info("Launching game, version: {} ", ProductVersionReader.getCurrentVersion());
    start();
  }

  /**
   * Starts a new UI-enabled game client. This method will return before the game client UI exits.
   * The game client UI will continue to run until it is shut down by the user.
   *
   * <p>No command-line arguments will launch a client; additional arguments can be supplied to
   * specify additional behavior.
   *
   * @throws IllegalStateException If called from a headless environment.
   */
  private static void start() {
    SwingUtilities.invokeLater(
        () -> {
          headedServerSetupModel = new HeadedServerSetupModel(gameSelectorModel);
          MainFrame.buildMainFrame(headedServerSetupModel, gameSelectorModel);
          headedServerSetupModel.showSelectType();
          ThreadRunner.runInNewThread(
              () -> {
                showMainFrame();
                gameSelectorModel.setReadyForSaveLoad();
              });
        });

    UpdateChecks.launch();
  }

  /**
   * Sets the 'main frame' to visible. In this context the main frame is the initial welcome (launch
   * lobby/single player game etc.) screen presented to GUI enabled clients.
   */
  public static void showMainFrame() {
    GameShutdownRegistry.runShutdownActions();
    final String startProperty = System.getProperty(TRIPLEA_START, "").toLowerCase(Locale.ROOT);
    if (startProperty.equals(TRIPLEA_START_LOBBY)) {
      SwingUtilities.invokeLater(() -> headedServerSetupModel.login());
    } else if (System.getProperty(TRIPLEA_SERVER, PROPERTY_FALSE).equals(PROPERTY_TRUE)) {
      MainFrame.show();
      gameSelectorModel.loadDefaultGameSameThread();
      final ServerModel serverModel = headedServerSetupModel.showServer();
      MainFrame.addQuitAction(serverModel::cancel);
      System.clearProperty(TRIPLEA_SERVER);
    } else if (System.getProperty(TRIPLEA_CLIENT, PROPERTY_FALSE).equals(PROPERTY_TRUE)) {
      MainFrame.show();
      headedServerSetupModel.showClient();
      System.clearProperty(TRIPLEA_CLIENT);
    } else {
      final String saveGameFileName = System.getProperty(TRIPLEA_GAME, "");
      if (!saveGameFileName.isEmpty()) {
        startGameDirectly(saveGameFileName, startProperty);
      }
      MainFrame.show();
      gameSelectorModel.loadDefaultGameSameThread();
      openMapDownloadWindowIfDownloadScheduled();
    }
  }

  private static void startGameDirectly(String saveGameFileName, String startProperty) {
    final boolean isSaveFile = saveGameFileName.endsWith(GameDataFileUtils.getExtension());
    if (!isSaveFile && !saveGameFileName.endsWith(".xml")) {
      log.warn("File '{}' neither save nor game map file", saveGameFileName);
      return;
    }
    Path gameFilePath = Path.of(saveGameFileName);
    if (!Files.exists(gameFilePath)) {
      gameFilePath =
          Path.of(ClientSetting.saveGamesFolderPath.getValueOrThrow().toString(), saveGameFileName);
      if (!Files.exists(gameFilePath)) {
        log.warn("Save game file '{}' not found", saveGameFileName);
        return;
      }
    }
    final Path fileToLoadPath = gameFilePath;
    SwingUtilities.invokeLater(
        () -> {
          final boolean isFileLoaded;
          if (isSaveFile) isFileLoaded = gameSelectorModel.loadSave(fileToLoadPath);
          else isFileLoaded = gameSelectorModel.loadMap(fileToLoadPath);
          if (isFileLoaded) {
            switch (startProperty) {
              case TRIPLEA_START_PBF:
                headedServerSetupModel.showPbf();
                break;
              case TRIPLEA_START_PBEM:
                headedServerSetupModel.showPbem();
                break;
              case TRIPLEA_START_LOCAL:
              default:
                headedServerSetupModel.showLocal();
                MainFrame.startGameDirectly(headedServerSetupModel);
            }
          }
        });
  }

  private static void openMapDownloadWindowIfDownloadScheduled() {
    final String downloadableMap = System.getProperty(TRIPLEA_MAP_DOWNLOAD, "");
    if (!downloadableMap.isEmpty()) {
      SwingUtilities.invokeLater(
          () -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(downloadableMap));
    }
  }

  public static void exitGameIfNoWindowsVisible() {
    // Invoke later to add this check to the end of the event dispatcher queue
    // and allow any potential in-flight 'setVisible' invocations to execute first.
    SwingUtilities.invokeLater(
        () -> {
          final boolean allFramesClosed =
              Arrays.stream(Frame.getFrames()).noneMatch(Component::isVisible);
          if (allFramesClosed) {
            ExitStatus.SUCCESS.exit();
          }
        });
  }

  /** After the game has been left, call this. */
  public static void clientLeftGame() {
    Util.ensureNotOnEventDispatchThread();
    Interruptibles.await(() -> SwingAction.invokeAndWait(headedServerSetupModel::showSelectType));
    showMainFrame();
  }
}
