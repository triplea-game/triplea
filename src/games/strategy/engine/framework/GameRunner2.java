package games.strategy.engine.framework;

import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import games.strategy.debug.ErrorConsole;
import games.strategy.engine.lobby.server.LobbyServer;
import games.strategy.triplea.ui.menubar.TripleAMenuBar;
import org.apache.commons.httpclient.HostConfiguration;

import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.startup.ui.MainFrame;
import games.strategy.performance.Perf;
import games.strategy.performance.PerfTimer;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.Version;

public class GameRunner2 {

  // not arguments:
  public static final int PORT = 3300;
  public static final String LOOK_AND_FEEL_PREF = "LookAndFeel";
  public static final String DELAYED_PARSING = "DelayedParsing";
  public static final String CASUALTY_SELECTION_SLOW = "CasualtySelectionSlow";
  public static final String PROXY_CHOICE = "proxy.choice";
  public static final String HTTP_PROXYHOST = "http.proxyHost";
  public static final String HTTP_PROXYPORT = "http.proxyPort";
  // do not include this in the getProperties list. they are only for loading an old savegame.
  public static final String OLD_EXTENSION = ".old";
  // argument options below:
  public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
  public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
  public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
  public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
  public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
  public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
  public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
  public static final String TRIPLEA_STARTED = "triplea.started";
  public static final String LOBBY_HOST = "triplea.lobby.host";
  public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
  public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
  public static final String LOBBY_GAME_SUPPORT_EMAIL = "triplea.lobby.game.supportEmail";
  public static final String LOBBY_GAME_SUPPORT_PASSWORD = "triplea.lobby.game.supportPassword";
  public static final String LOBBY_GAME_RECONNECTION = "triplea.lobby.game.reconnection";
  // what is the default version of triplea (the one in the "bin" folder)
  public static final String TRIPLEA_ENGINE_VERSION_BIN = "triplea.engine.version.bin";
  // proxy stuff
  public static final String PROXY_HOST = "proxy.host";
  public static final String PROXY_PORT = "proxy.port";
  // other stuff
  public static final String TRIPLEA_DO_NOT_CHECK_FOR_UPDATES = "triplea.doNotCheckForUpdates";
  // has the memory been manually set or not?
  public static final String TRIPLEA_MEMORY_SET = "triplea.memory.set";
  public static final String TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME = "triplea.server.startGameSyncWaitTime";
  public static final String TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME = "triplea.server.observerJoinWaitTime";
  // non-commandline-argument-properties (for preferences)
  // first time we've run this version of triplea?
  private static final String TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY =
      "triplea.firstTimeThisVersion" + ClientContext.engineVersion();
  private static final String TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE = "triplea.lastCheckForEngineUpdate";
  // only for Online?
  public static final String TRIPLEA_MEMORY_ONLINE_ONLY = "triplea.memory.onlineOnly";
  // what should our xmx be approximately?
  public static final String TRIPLEA_MEMORY_XMX = "triplea.memory.Xmx";
  public static final String TRIPLEA_MEMORY_USE_DEFAULT = "triplea.memory.useDefault";
  public static final String SYSTEM_INI = "system.ini";
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;
  public static final int DEFAULT_CLIENT_GAMEDATA_LOAD_GRACE_TIME =
      Math.max(MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME, 25);
  // need time for network transmission of a large game data
  public static final int MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME = MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME + 10;
  public static final int DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME =
      Math.max(DEFAULT_CLIENT_GAMEDATA_LOAD_GRACE_TIME + 10, 35);
  public static final int ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME = 10;
  public static final int MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME =
      MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME + ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME + 110;
  public static final int DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME =
      Math.max(Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, 900),
          DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME + ADDITIONAL_SERVER_ERROR_DISCONNECTION_WAIT_TIME + 110);

  public static enum ProxyChoice {
    NONE, USE_SYSTEM_SETTINGS, USE_USER_PREFERENCES
  }

  public static String[] getProperties() {
    return new String[] {TRIPLEA_GAME_PROPERTY, TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY, TRIPLEA_HOST_PROPERTY,
        TRIPLEA_PORT_PROPERTY, TRIPLEA_NAME_PROPERTY, TRIPLEA_SERVER_PASSWORD_PROPERTY, TRIPLEA_STARTED, LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY,
        LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY, TRIPLEA_ENGINE_VERSION_BIN, PROXY_HOST, PROXY_PORT,
        TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, TRIPLEA_MEMORY_SET};
  }

  private static void usage() {
    System.out.println("Arguments\n" + "   " + TRIPLEA_GAME_PROPERTY + "=<FILE_NAME>\n" + "   "
        + TRIPLEA_SERVER_PROPERTY + "=true\n" + "   " + TRIPLEA_CLIENT_PROPERTY + "=true\n" + "   "
        + TRIPLEA_HOST_PROPERTY + "=<HOST_IP>\n" + "   " + TRIPLEA_PORT_PROPERTY + "=<PORT>\n" + "   "
        + TRIPLEA_NAME_PROPERTY + "=<PLAYER_NAME>\n" + "   " + LobbyServer.TRIPLEA_LOBBY_PORT_PROPERTY + "=<LOBBY_PORT>\n" + "   " + LOBBY_HOST
        + "=<LOBBY_HOST>\n" + "   " + LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n" + "   " + LOBBY_GAME_HOSTED_BY
        + "=<LOBBY_GAME_HOSTED_BY>\n" + "   " + PROXY_HOST + "=<Proxy_Host>\n" + "   " + PROXY_PORT + "=<Proxy_Port>\n"
        + "   " + TRIPLEA_MEMORY_SET + "=true/false <did you set the xmx manually?>\n" + "\n"
        + "if there is only one argument, and it does not start with triplea.game, the argument will be \n"
        + "taken as the name of the file to load.\n" + "\n" + "Example\n" + "   to start a game using the given file:\n"
        + "\n" + "   triplea /home/sgb/games/test.xml\n" + "\n" + "   or\n" + "\n"
        + "   triplea triplea.game=/home/sgb/games/test.xml\n" + "\n" + "   to connect to a remote host:\n" + "\n"
        + "   triplea triplea.client=true triplea.host=127.0.0.0 triplea.port=3300 triplea.name=Paul\n" + "\n"
        + "   to start a server with the given game\n" + "\n"
        + "   triplea triplea.game=/home/sgb/games/test.xml triplea.server=true triplea.port=3300 triplea.name=Allan"
        + "\n"
        + "   to start a server, you can optionally password protect the game using triplea.server.password=foo");
  }

  public static void main(final String[] args) {
    ErrorConsole.getConsole();

    try (PerfTimer timer = Perf.startTimer("Show main window")) {
      // do after we handle command line args
      checkForMemoryXMX();
      SwingUtilities.invokeLater(() -> setupLookAndFeel());
      showMainFrame();
    }
    (new Thread(() -> setupLogging())).start();

    setupProxies();
    (new Thread(() -> checkForUpdates())).start();
    handleCommandLineArgs(args);
  }

  private static void showMainFrame() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final MainFrame frame = new MainFrame();
        frame.requestFocus();
        frame.toFront();
        frame.setVisible(true);
      }
    });
  }

  /**
   * Move command line arguments to System.properties
   */
  private static void handleCommandLineArgs(final String[] args) {
    final String[] properties = getProperties();
    // if only 1 arg, it might be the game path, find it (like if we are double clicking a savegame)
    // optionally, it may not start with the property name
    if (args.length == 1) {
      boolean startsWithPropertyKey = false;
      for (final String prop : properties) {
        if (args[0].startsWith(prop)) {
          startsWithPropertyKey = true;
          break;
        }
      }
      if (!startsWithPropertyKey) {
        // change it to start with the key
        args[0] = TRIPLEA_GAME_PROPERTY + "=" + args[0];
      }
    }
    boolean usagePrinted = false;
    for (final String arg1 : args) {
      boolean found = false;
      String arg = arg1;
      final int indexOf = arg.indexOf('=');
      if (indexOf > 0) {
        arg = arg.substring(0, indexOf);
        for (final String property : properties) {
          if (arg.equals(property)) {
            final String value = getValue(arg1);
            System.getProperties().setProperty(property, value);
            System.out.println(property + ":" + value);
            found = true;
            break;
          }
        }
      }
      if (!found) {
        System.out.println("Unrecogized:" + arg1);
        if (!usagePrinted) {
          usagePrinted = true;
          usage();
        }
      }
    }
    final String version = System.getProperty(TRIPLEA_ENGINE_VERSION_BIN);
    if (version != null && version.length() > 0) {
      final Version testVersion;
      try {
        testVersion = new Version(version);
        // if successful we don't do anything
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + version);
        if (!ClientContext.engineVersion().getVersion().equals(testVersion, false)) {
          System.out.println("Current Engine version in use: " + ClientContext.engineVersion());
        }
      } catch (final Exception e) {
        System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, ClientContext.engineVersion().toString());
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + ClientContext.engineVersion());
      }
    } else {
      System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, ClientContext.engineVersion().toString());
      System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + ClientContext.engineVersion());
    }
  }

  private static String getValue(final String arg) {
    final int index = arg.indexOf('=');
    if (index == -1) {
      return "";
    }
    return arg.substring(index + 1);
  }

  public static void setupLookAndFeel() {
    SwingAction.invokeAndWait(() -> {
      try {
        UIManager.setLookAndFeel(getDefaultLookAndFeel());
        // FYI if you are getting a null pointer exception in Substance, like this:
        // org.pushingpixels.substance.internal.utils.SubstanceColorUtilities
        // .getDefaultBackgroundColor(SubstanceColorUtilities.java:758)
        // Then it is because you included the swingx substance library without including swingx.
        // You can solve by including both swingx libraries or removing both,
        // or by setting the look and feel twice in a row.
      } catch (final Throwable t) {
        if (!GameRunner.isMac()) {
          try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          } catch (final Exception e) {
          }
        }
      }
    });
  }

  public static void setupLogging() {
    // setup logging to read our logging.properties
    try {
      LogManager.getLogManager().readConfiguration(ClassLoader.getSystemResourceAsStream("logging.properties"));
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static String getDefaultLookAndFeel() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    // substance 7.x
    String defaultLookAndFeel = "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel";
    // substance 5.x
    // String defaultLookAndFeel = "org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel";
    // macs are already beautiful
    if (GameRunner.isMac()) {
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    }
    final String userDefault = pref.get(LOOK_AND_FEEL_PREF, defaultLookAndFeel);
    final List<String> availableSkins = TripleAMenuBar.getLookAndFeelAvailableList();
    if (!availableSkins.contains(userDefault)) {
      if (!availableSkins.contains(defaultLookAndFeel)) {
        return UIManager.getSystemLookAndFeelClassName();
      }
      setDefaultLookAndFeel(defaultLookAndFeel);
      return defaultLookAndFeel;
    }
    return userDefault;
  }

  public static void setDefaultLookAndFeel(final String lookAndFeelClassName) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    pref.put(LOOK_AND_FEEL_PREF, lookAndFeelClassName);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static void checkForMemoryXMX() {
    final String memSetString = System.getProperty(TRIPLEA_MEMORY_SET, "false");
    final boolean memSet = Boolean.parseBoolean(memSetString);
    // if we have already set the memory, then return.
    // (example: we used process runner to create a new triplea with a specific memory)
    if (memSet) {
      return;
    }
    final Properties systemIni = getSystemIni();
    if (useDefaultMaxMemory(systemIni)) {
      return;
    }
    if (getUseMaxMemorySettingOnlyForOnlineJoinOrHost(systemIni)) {
      return;
    }
    long xmx = getMaxMemoryFromSystemIniFileInMB(systemIni);
    // if xmx less than zero, return (because it means we do not want to change it)
    if (xmx <= 0) {
      return;
    }
    final int mb = 1024 * 1024;
    xmx = xmx * mb;
    final long currentMaxMemory = Runtime.getRuntime().maxMemory();
    System.out.println("Current max memory: " + currentMaxMemory + ";  and new xmx should be: " + xmx);
    final long diff = Math.abs(currentMaxMemory - xmx);
    // Runtime.maxMemory is never accurate, and is usually off by 5% to 15%,
    // so if our difference is less than 22% we should just ignore the difference
    if (diff <= xmx * 0.22) {
      return;
    }
    // the difference is significant enough that we should re-run triplea with a larger number
    TripleAProcessRunner.startNewTripleA(xmx);
    // must exit now
    System.exit(0);
  }

  public static boolean useDefaultMaxMemory(final Properties systemIni) {
    final String useDefaultMaxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    final boolean useDefaultMaxMemory = Boolean.parseBoolean(useDefaultMaxMemoryString);
    return useDefaultMaxMemory;
  }

  public static long getMaxMemoryInBytes() {
    final Properties systemIni = getSystemIni();
    final String useDefaultMaxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    final boolean useDefaultMaxMemory = Boolean.parseBoolean(useDefaultMaxMemoryString);
    final String maxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_XMX, "").trim();
    // for whatever reason, .maxMemory() returns a value about 12% smaller than the real Xmx value.
    // Just something to be aware of.
    long max = Runtime.getRuntime().maxMemory();
    if (!useDefaultMaxMemory && maxMemoryString.length() > 0) {
      try {
        final int maxMemorySet = Integer.parseInt(maxMemoryString);
        // it is in MB
        max = 1024 * 1024 * ((long) maxMemorySet);
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return max;
  }

  public static int getMaxMemoryFromSystemIniFileInMB(final Properties systemIni) {
    final String maxMemoryString = systemIni.getProperty(TRIPLEA_MEMORY_XMX, "").trim();
    int maxMemorySet = -1;
    if (maxMemoryString.length() > 0) {
      try {
        maxMemorySet = Integer.parseInt(maxMemoryString);
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return maxMemorySet;
  }

  public static Properties setMaxMemoryInMB(final int maxMemoryInMB) {
    System.out.println("Setting max memory for TripleA to: " + maxMemoryInMB + "m");
    final Properties prop = new Properties();
    prop.put(TRIPLEA_MEMORY_USE_DEFAULT, "false");
    prop.put(TRIPLEA_MEMORY_XMX, "" + maxMemoryInMB);
    return prop;
  }

  public static void clearMaxMemory() {
    final Properties prop = new Properties();
    prop.put(TRIPLEA_MEMORY_USE_DEFAULT, "true");
    prop.put(TRIPLEA_MEMORY_ONLINE_ONLY, "true");
    prop.put(TRIPLEA_MEMORY_XMX, "");
    writeSystemIni(prop, false);
  }

  public static void setUseMaxMemorySettingOnlyForOnlineJoinOrHost(final boolean useForOnlineOnly,
      final Properties prop) {
    prop.put(TRIPLEA_MEMORY_ONLINE_ONLY, "" + useForOnlineOnly);
  }

  public static boolean getUseMaxMemorySettingOnlyForOnlineJoinOrHost(final Properties systemIni) {
    final String forOnlineOnlyString = systemIni.getProperty(TRIPLEA_MEMORY_ONLINE_ONLY, "true");
    final boolean forOnlineOnly = Boolean.parseBoolean(forOnlineOnlyString);
    return forOnlineOnly;
  }

  public static Properties getSystemIni() {
    final Properties rVal = new Properties();
    final File systemIni = new File(ClientFileSystemHelper.getRootFolder(), SYSTEM_INI);
    if (systemIni != null && systemIni.exists()) {
      try (FileInputStream fis = new FileInputStream(systemIni)) {
        rVal.load(fis);
      } catch (final IOException e) {
        ClientLogger.logQuietly(e);
      }
    }
    return rVal;
  }

  public static void writeSystemIni(final Properties properties, final boolean clearOldAndOverwrite) {
    final Properties toWrite;
    if (clearOldAndOverwrite) {
      toWrite = properties;
    } else {
      toWrite = getSystemIni();
      for (final Entry<Object, Object> entry : properties.entrySet()) {
        toWrite.put(entry.getKey(), entry.getValue());
      }
    }

    final File systemIni = new File(ClientFileSystemHelper.getRootFolder(), SYSTEM_INI);

    try (FileOutputStream fos = new FileOutputStream(systemIni)) {
      toWrite.store(fos, SYSTEM_INI);
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static void setupProxies() {
    // System properties, not user pref
    String proxyHostArgument = System.getProperty(PROXY_HOST);
    String proxyPortArgument = System.getProperty(PROXY_PORT);
    if (proxyHostArgument == null) {
      // in case it was set by -D we also check this
      proxyHostArgument = System.getProperty(HTTP_PROXYHOST);
    }
    if (proxyPortArgument == null) {
      proxyPortArgument = System.getProperty(HTTP_PROXYPORT);
    }
    // arguments should override and set user preferences
    String proxyHost = null;
    if (proxyHostArgument != null && proxyHostArgument.trim().length() > 0) {
      proxyHost = proxyHostArgument;
    }
    String proxyPort = null;
    if (proxyPortArgument != null && proxyPortArgument.trim().length() > 0) {
      try {
        Integer.parseInt(proxyPortArgument);
        proxyPort = proxyPortArgument;
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    if (proxyHost != null || proxyPort != null) {
      setProxy(proxyHost, proxyPort, ProxyChoice.USE_USER_PREFERENCES);
    }
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    final ProxyChoice choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
    if (choice == ProxyChoice.USE_SYSTEM_SETTINGS) {
      setToUseSystemProxies();
    } else if (choice == ProxyChoice.USE_USER_PREFERENCES) {
      final String host = pref.get(GameRunner2.PROXY_HOST, "");
      final String port = pref.get(GameRunner2.PROXY_PORT, "");
      if (host.trim().length() > 0) {
        System.setProperty(HTTP_PROXYHOST, host);
      }
      if (port.trim().length() > 0) {
        System.setProperty(HTTP_PROXYPORT, port);
      }
    }
  }

  public static void setProxy(final String proxyHost, final String proxyPort, final ProxyChoice proxyChoice) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    final ProxyChoice choice;
    if (proxyChoice != null) {
      choice = proxyChoice;
      pref.put(PROXY_CHOICE, proxyChoice.toString());
    } else {
      choice = ProxyChoice.valueOf(pref.get(PROXY_CHOICE, ProxyChoice.NONE.toString()));
    }
    if (proxyHost != null && proxyHost.trim().length() > 0) {
      // user pref, not system properties
      pref.put(PROXY_HOST, proxyHost);
      if (choice == ProxyChoice.USE_USER_PREFERENCES) {
        System.setProperty(HTTP_PROXYHOST, proxyHost);
      }
    }
    if (proxyPort != null && proxyPort.trim().length() > 0) {
      try {
        Integer.parseInt(proxyPort);
        // user pref, not system properties
        pref.put(PROXY_PORT, proxyPort);
        if (choice == ProxyChoice.USE_USER_PREFERENCES) {
          System.setProperty(HTTP_PROXYPORT, proxyPort);
        }
      } catch (final NumberFormatException e) {
        ClientLogger.logQuietly(e);
      }
    }
    if (choice == ProxyChoice.NONE) {
      System.clearProperty(HTTP_PROXYHOST);
      System.clearProperty(HTTP_PROXYPORT);
    } else if (choice == ProxyChoice.USE_SYSTEM_SETTINGS) {
      setToUseSystemProxies();
    }
    if (proxyHost != null || proxyPort != null || proxyChoice != null) {
      try {
        pref.flush();
        pref.sync();
      } catch (final BackingStoreException e) {
        ClientLogger.logQuietly(e);
      }
    }
  }

  private static void setToUseSystemProxies() {
    final String JAVA_NET_USESYSTEMPROXIES = "java.net.useSystemProxies";
    System.setProperty(JAVA_NET_USESYSTEMPROXIES, "true");
    List<Proxy> proxyList = null;
    try {
      final ProxySelector def = ProxySelector.getDefault();
      if (def != null) {
        proxyList = def.select(new URI("http://sourceforge.net/"));
        ProxySelector.setDefault(null);
        if (proxyList != null && !proxyList.isEmpty()) {
          final Proxy proxy = proxyList.get(0);
          final InetSocketAddress address = (InetSocketAddress) proxy.address();
          if (address != null) {
            final String host = address.getHostName();
            final int port = address.getPort();
            System.setProperty(HTTP_PROXYHOST, host);
            System.setProperty(HTTP_PROXYPORT, Integer.toString(port));
            System.setProperty(PROXY_HOST, host);
            System.setProperty(PROXY_PORT, Integer.toString(port));
          } else {
            System.clearProperty(HTTP_PROXYHOST);
            System.clearProperty(HTTP_PROXYPORT);
            System.clearProperty(PROXY_HOST);
            System.clearProperty(PROXY_PORT);
          }
        }
      } else {
        final String host = System.getProperty(PROXY_HOST);
        final String port = System.getProperty(PROXY_PORT);
        if (host == null) {
          System.clearProperty(HTTP_PROXYHOST);
        } else {
          System.setProperty(HTTP_PROXYHOST, host);
        }
        if (port == null) {
          System.clearProperty(HTTP_PROXYPORT);
        } else {
          try {
            Integer.parseInt(port);
            System.setProperty(HTTP_PROXYPORT, port);
          } catch (final NumberFormatException nfe) {
            // nothing
          }
        }
      }
    } catch (final Exception e) {
      ClientLogger.logQuietly(e);
    } finally {
      System.setProperty(JAVA_NET_USESYSTEMPROXIES, "false");
    }
  }

  public static void addProxy(final HostConfiguration config) {
    final String host = System.getProperty(HTTP_PROXYHOST);
    final String port = System.getProperty(HTTP_PROXYPORT, "-1");
    if (host != null && host.trim().length() > 0) {
      config.setProxy(host, Integer.valueOf(port));
    }
  }

  public static boolean getDelayedParsing() {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    return pref.getBoolean(DELAYED_PARSING, true);
  }

  public static void setDelayedParsing(final boolean delayedParsing) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    pref.putBoolean(DELAYED_PARSING, delayedParsing);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  // TODO: delete all this when we figure out the new casualty selection algorithm
  public static boolean getCasualtySelectionSlow() {
    if (s_checkedCasualtySelectionSlowPreference) {
      return s_casualtySelectionSlow;
    }
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    s_casualtySelectionSlow = pref.getBoolean(CASUALTY_SELECTION_SLOW, false);
    s_checkedCasualtySelectionSlowPreference = true;
    return s_casualtySelectionSlow;
  }

  private static boolean s_casualtySelectionSlow = false;
  private static boolean s_checkedCasualtySelectionSlowPreference = false;

  public static void setCasualtySelectionSlow(final boolean casualtySelectionBeta) {
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    pref.putBoolean(CASUALTY_SELECTION_SLOW, casualtySelectionBeta);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  public static int getServerStartGameSyncWaitTime() {
    return Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, Preferences.userNodeForPackage(GameRunner2.class)
        .getInt(TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME));
  }

  public static void resetServerStartGameSyncWaitTime() {
    setServerStartGameSyncWaitTime(DEFAULT_SERVER_START_GAME_SYNC_WAIT_TIME);
  }

  public static void setServerStartGameSyncWaitTime(final int seconds) {
    final int wait = Math.max(MINIMUM_SERVER_START_GAME_SYNC_WAIT_TIME, seconds);
    if (wait == getServerStartGameSyncWaitTime()) {
      return;
    }
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    pref.putInt(TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME, wait);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  public static int getServerObserverJoinWaitTime() {
    return Math.max(MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME, Preferences.userNodeForPackage(GameRunner2.class)
        .getInt(TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME, DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME));
  }

  public static void resetServerObserverJoinWaitTime() {
    setServerObserverJoinWaitTime(DEFAULT_SERVER_OBSERVER_JOIN_WAIT_TIME);
  }

  public static void setServerObserverJoinWaitTime(final int seconds) {
    final int wait = Math.max(MINIMUM_SERVER_OBSERVER_JOIN_WAIT_TIME, seconds);
    if (wait == getServerObserverJoinWaitTime()) {
      return;
    }
    final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
    pref.putInt(TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME, wait);
    try {
      pref.sync();
    } catch (final BackingStoreException e) {
      ClientLogger.logQuietly(e);
    }
  }

  private static void checkForUpdates() {
    final Thread t = new Thread(new Runnable() {
      @Override
      public void run() {
        // do not check if we are the old extra jar. (a jar kept for backwards compatibility only)
        if (ClientFileSystemHelper.areWeOldExtraJar()) {
          return;
        }
        if (System.getProperty(GameRunner2.TRIPLEA_SERVER_PROPERTY, "false").equalsIgnoreCase("true")) {
          return;
        }
        if (System.getProperty(GameRunner2.TRIPLEA_CLIENT_PROPERTY, "false").equalsIgnoreCase("true")) {
          return;
        }
        if (System.getProperty(GameRunner2.TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
          return;
        }

        // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
        final String fileName = System.getProperty(GameRunner2.TRIPLEA_GAME_PROPERTY, "");
        if (fileName.trim().length() > 0) {
          return;
        }

        boolean busy = false;
        busy = checkForLatestEngineVersionOut();
        if (!busy) {
          busy = checkForUpdatedMaps();
        }
      }
    }, "Checking Latest TripleA Engine Version");
    t.start();
  }

  /**
   * @return true if we are out of date or this is the first time this triplea has ever been run
   */
  private static boolean checkForLatestEngineVersionOut() {
    try {
      final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
      final boolean firstTimeThisVersion = pref.getBoolean(TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY, true);
      // check at most once per 2 days (but still allow a 'first run message' for a new version of triplea)
      final Calendar calendar = Calendar.getInstance();
      final int year = calendar.get(Calendar.YEAR);
      final int day = calendar.get(Calendar.DAY_OF_YEAR);
      // format year:day
      final String lastCheckTime = pref.get(TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE, "");
      if (!firstTimeThisVersion && lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearDay = lastCheckTime.split(":");
        if (Integer.parseInt(yearDay[0]) >= year && Integer.parseInt(yearDay[1]) + 1 >= day) {
          return false;
        }
      }
      pref.put(TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE, year + ":" + day);
      try {
        pref.sync();
      } catch (final BackingStoreException e) {
      }
      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return false;
      }
      if (ClientContext.engineVersion().getVersion().isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(false),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE, false, new CountDownLatchHandler(true));
          }
        });
        return true;
      } else {
        // if this is the first time we are running THIS version of TripleA, then show what is new.
        if (firstTimeThisVersion
            && latestEngineOut.getReleaseNotes().containsKey(ClientContext.engineVersion().getVersion())) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getCurrentFeaturesComponent(),
                  "What is New?", JOptionPane.INFORMATION_MESSAGE, false, new CountDownLatchHandler(true));
            }
          });
          pref.putBoolean(TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY, false);
          try {
            pref.flush();
          } catch (final BackingStoreException ex) {
          }
          return true;
        }
      }
    } catch (final Exception e) {
      System.out.println("Error while checking for engine updates: " + e.getMessage());
    }
    return false;
  }

  /**
   * @return true if we have any out of date maps
   */
  private static boolean checkForUpdatedMaps() {
    MapDownloadController downloadController = ClientContext.mapDownloadController();
    return downloadController.checkDownloadedMapsAreLatest();
  }


  public static Image getGameIcon(final Window frame) {
    Image img = null;
    try {
      img = frame.getToolkit().getImage(GameRunner2.class.getResource("ta_icon.png"));
    } catch (final Exception ex) {
      ClientLogger.logError("ta_icon.png not loaded", ex);
    }
    final MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(img, 0);
    try {
      tracker.waitForAll();
    } catch (final InterruptedException ex) {
      ClientLogger.logQuietly(ex);
    }
    return img;
  }

}
