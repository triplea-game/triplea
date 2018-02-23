package games.strategy.engine.framework.startup.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.editors.IBean;
import games.strategy.engine.framework.startup.ui.editors.SelectAndViewEditor;
import games.strategy.engine.pbem.GenericEmailSender;
import games.strategy.engine.pbem.GmailEmailSender;
import games.strategy.engine.pbem.HotmailEmailSender;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.NullEmailSender;
import games.strategy.engine.pbem.NullForumPoster;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.engine.pbem.TripleAForumPoster;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.InternalDiceServer;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.engine.random.PropertiesDiceRoller;
import games.strategy.triplea.pbem.AxisAndAlliesForumPoster;
import games.strategy.ui.SwingAction;

/**
 * A panel for setting up Play by Email/Forum.
 * This panel listens to the GameSelectionModel so it can refresh when a new game is selected or save game loaded
 * The MainPanel also listens to this panel, and we notify it through the notifyObservers()
 */
public class PbemSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 9006941131918034674L;
  private static final String DICE_ROLLER = "games.strategy.engine.random.IRemoteDiceServer";
  private final GameSelectorModel gameSelectorModel;
  private final SelectAndViewEditor diceServerEditor;
  private final SelectAndViewEditor forumPosterEditor;
  private final SelectAndViewEditor emailSenderEditor;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();
  private final JPanel localPlayerPanel = new JPanel();
  private final JButton localPlayerSelection = new JButton("Select Local Players and AI's");

  /**
   * Creates a new instance.
   *
   * @param model
   *        the GameSelectionModel, though which changes are obtained when new games are chosen, or save games loaded
   */
  public PbemSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    diceServerEditor = new SelectAndViewEditor("Dice Server", "");
    forumPosterEditor = new SelectAndViewEditor("Post to Forum", "forumPosters.html");
    emailSenderEditor = new SelectAndViewEditor("Provider", "emailSenders.html");
    createComponents();
    layoutComponents();
    setupListeners();
    loadAll();
    setWidgetActivation();
  }

  private void createComponents() {
    final JScrollPane scrollPane = new JScrollPane(localPlayerPanel);
    localPlayerPanel.addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(final HierarchyEvent e) {
        final Window window = SwingUtilities.getWindowAncestor(localPlayerPanel);
        if (window instanceof Dialog) {
          final Dialog dialog = (Dialog) window;
          if (!dialog.isResizable()) {
            dialog.setResizable(true);
            dialog.setMinimumSize(new Dimension(700, 700));
          }
        }
      }
    });
    localPlayerSelection.addActionListener(
        e -> JOptionPane.showMessageDialog(PbemSetupPanel.this, scrollPane, "Select Local Players and AI's",
            JOptionPane.PLAIN_MESSAGE));
  }

  private void layoutComponents() {
    removeAll();
    setLayout(new GridBagLayout());
    // Empty border works as margin
    setBorder(new EmptyBorder(10, 10, 10, 10));
    int row = 0;
    add(diceServerEditor, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(10, 0, 20, 0), 0, 0));
    // the play by Forum settings
    forumPosterEditor.setBorder(new TitledBorder("Play By Forum"));
    add(forumPosterEditor, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
    final JPanel emailPanel = new JPanel(new GridBagLayout());
    emailPanel.setBorder(new TitledBorder("Play By Email"));
    add(emailPanel, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 20, 0), 0, 0));
    int panelRow = 0;
    emailPanel.add(emailSenderEditor, new GridBagConstraints(0, panelRow++, 1, 1, 1.0d, 0d,
        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));

    // add selection of local players
    add(localPlayerSelection, new GridBagConstraints(0, row++, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHEAST,
        GridBagConstraints.NONE, new Insets(10, 0, 10, 0), 0, 0));
    layoutPlayerComponents(localPlayerPanel, playerTypes, gameSelectorModel.getGameData());
    setWidgetActivation();
  }

  @Override
  public boolean isMetaSetupPanelInstance() {
    return false;
  }


  @Override
  public void setWidgetActivation() {}

  private void setupListeners() {
    // register, so we get notified when the game model (GameData) changes (e.g if the user load a save game or selects
    // another game)
    gameSelectorModel.addObserver(this);
    // subscribe to editor changes, so we cannotify the MainPanel
    diceServerEditor.addPropertyChangeListener(e -> notifyObservers());
    forumPosterEditor.addPropertyChangeListener(e -> notifyObservers());
    emailSenderEditor.addPropertyChangeListener(e -> notifyObservers());
  }

  private void loadAll() {
    final GameData data = gameSelectorModel.getGameData();
    if (data != null) {
      loadDiceServer(data);
      loadForumPosters(data);
      loadEmailSender(data);
    }
  }

  /**
   * Load the dice rollers from cache, if the game was a save game, the dice roller store is selected.
   *
   * @param data
   *        the game data
   */
  private void loadDiceServer(final GameData data) {
    final List<IRemoteDiceServer> diceRollers = new ArrayList<>(PropertiesDiceRoller.loadFromFile());
    diceRollers.add(new InternalDiceServer());
    for (final IRemoteDiceServer diceRoller : diceRollers) {
      final IRemoteDiceServer cached =
          (IRemoteDiceServer) LocalBeanCache.INSTANCE.getSerializable(diceRoller.getDisplayName());
      if (cached != null) {
        diceRoller.setCcAddress(cached.getCcAddress());
        diceRoller.setToAddress(cached.getToAddress());
        diceRoller.setGameId(cached.getGameId());
      }
    }
    diceServerEditor.setBeans(diceRollers);
    if (gameSelectorModel.isSavedGame()) {
      // get the dice roller from the save game, if any
      final IRemoteDiceServer roller = (IRemoteDiceServer) data.getProperties().get(DICE_ROLLER);
      if (roller != null) {
        diceServerEditor.setSelectedBean(roller);
      }
    }
  }

  /**
   * Load the Forum poster that are stored in the GameData, and select it in the list.
   * Sensitive information such as passwords are not stored in save games, so the are loaded from the LocalBeanCache
   *
   * @param data the game data
   */
  private void loadForumPosters(final GameData data) {
    // get the forum posters,
    final List<IForumPoster> forumPosters = new ArrayList<>();
    forumPosters.add(useCacheIfAvailable(new NullForumPoster()));
    forumPosters.add(useCacheIfAvailable(new AxisAndAlliesForumPoster()));
    forumPosters.add(useCacheIfAvailable(new TripleAForumPoster()));
    forumPosterEditor.setBeans(forumPosters);
    // now get the poster stored in the save game
    final IForumPoster forumPoster = (IForumPoster) data.getProperties().get(PBEMMessagePoster.FORUM_POSTER_PROP_NAME);
    if (forumPoster != null) {
      // if we have a cached version, use the credentials from this, as each player has different forum login
      final IForumPoster cached =
          (IForumPoster) LocalBeanCache.INSTANCE.getSerializable(forumPoster.getClass().getCanonicalName());
      if (cached != null) {
        forumPoster.setUsername(cached.getUsername());
        forumPoster.setPassword(cached.getPassword());
        forumPoster.setCredentialsSaved(cached.areCredentialsSaved());
      }
      forumPosterEditor.setSelectedBean(forumPoster);
    }
  }

  /**
   * Configures the list of Email senders. If the game was saved we use this email sender.
   * Since passwords are not stored in save games, the LocalBeanCache is checked
   *
   * @param data
   *        the game data
   */
  private void loadEmailSender(final GameData data) {
    // The list of email, either loaded from cache or created
    final List<IEmailSender> emailSenders = new ArrayList<>();
    emailSenders.add(useCacheIfAvailable(new NullEmailSender()));
    emailSenders.add(useCacheIfAvailable(new GmailEmailSender()));
    emailSenders.add(useCacheIfAvailable(new HotmailEmailSender()));
    emailSenders.add(useCacheIfAvailable(new GenericEmailSender()));
    emailSenderEditor.setBeans(emailSenders);
    // now get the sender from the save game, update it with credentials from the cache, and set it
    final IEmailSender sender = (IEmailSender) data.getProperties().get(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME);
    if (sender != null) {
      final IEmailSender cached =
          (IEmailSender) LocalBeanCache.INSTANCE.getSerializable(sender.getClass().getCanonicalName());
      if (cached != null) {
        sender.setUserName(cached.getUserName());
        sender.setPassword(cached.getPassword());
        sender.setCredentialsSaved(cached.areCredentialsSaved());
      }
      emailSenderEditor.setSelectedBean(sender);
    }
  }

  /**
   * finds a cached instance of the give type. If a cached version is not available a new one is created
   *
   * @param theClassType
   *        the type of class
   * @return a IBean either loaded from the cache or created
   */
  private static <T extends IBean> T useCacheIfAvailable(final T instance) {
    @SuppressWarnings("unchecked")
    final T cached = (T) LocalBeanCache.INSTANCE.getSerializable(instance.getClass().getCanonicalName());
    return (cached == null) ? instance : cached;
  }

  @Override
  public void shutDown() {
    gameSelectorModel.deleteObserver(this);
  }

  /**
   * Called when the current game changes.
   */
  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  /**
   * Called when the observers detect change, to see if the game is in a startable state.
   */
  @Override
  public boolean canGameStart() {
    if (gameSelectorModel.getGameData() == null) {
      return false;
    }
    final boolean diceServerValid = diceServerEditor.isBeanValid();
    final boolean summaryValid = forumPosterEditor.isBeanValid();
    final boolean emailValid = emailSenderEditor.isBeanValid();
    final boolean pbemReady =
        diceServerValid && summaryValid && emailValid && (gameSelectorModel.getGameData() != null);
    if (!pbemReady) {
      return false;
    }
    // make sure at least 1 player is enabled
    return playerTypes.stream().anyMatch(PlayerSelectorRow::isPlayerEnabled);
  }

  @Override
  public void postStartGame() {
    // // store the dice server
    final GameData data = gameSelectorModel.getGameData();
    data.getProperties().set(DICE_ROLLER, diceServerEditor.getBean());
    // store the Turn Summary Poster
    final IForumPoster poster = (IForumPoster) forumPosterEditor.getBean();
    if (poster != null) {
      // clone the poster, the remove sensitive info, and put the clone into the game data
      // this was the sensitive info is not stored in the save game, but the user cache still has the password
      final IForumPoster summaryPoster = poster.doClone();
      summaryPoster.clearSensitiveInfo();
      data.getProperties().set(PBEMMessagePoster.FORUM_POSTER_PROP_NAME, summaryPoster);
    }
    // store the email poster
    IEmailSender sender = (IEmailSender) emailSenderEditor.getBean();
    if (sender != null) {
      // create a clone, delete the sensitive information in the clone, and use it in the game
      // the locally cached version still has the password so the user doesn't have to enter it every time
      sender = sender.clone();
      sender.clearSensitiveInfo();
      data.getProperties().set(PBEMMessagePoster.EMAIL_SENDER_PROP_NAME, sender);
    }
    // store whether we are a pbem game or not, whether we are capable of posting a game save
    if ((poster != null) || (sender != null)) {
      data.getProperties().set(PBEMMessagePoster.PBEM_GAME_PROP_NAME, true);
    }
  }

  /**
   * Is called in response to the GameSelectionModel being updated. It means the we have to reload the form
   *
   * @param o always null
   * @param arg always null
   */
  @Override
  public void update(final Observable o, final Object arg) {
    SwingAction.invokeNowOrLater(() -> {
      loadAll();
      layoutComponents();
    });
  }

  /**
   * Called when the user hits play.
   */
  @Override
  public Optional<ILauncher> getLauncher() {
    // update local cache and write to disk before game starts
    final IForumPoster poster = (IForumPoster) forumPosterEditor.getBean();
    if (poster != null) {
      LocalBeanCache.INSTANCE.storeSerializable(poster.getClass().getCanonicalName(), poster);
    }
    final IEmailSender sender = (IEmailSender) emailSenderEditor.getBean();
    if (sender != null) {
      LocalBeanCache.INSTANCE.storeSerializable(sender.getClass().getCanonicalName(), sender);
    }
    final IRemoteDiceServer server = (IRemoteDiceServer) diceServerEditor.getBean();
    LocalBeanCache.INSTANCE.storeSerializable(server.getDisplayName(), server);
    LocalBeanCache.INSTANCE.writeToDisk();
    // create local launcher
    final String gameUuid = (String) gameSelectorModel.getGameData().getProperties().get(GameData.GAME_UUID);
    final PbemDiceRoller randomSource = new PbemDiceRoller((IRemoteDiceServer) diceServerEditor.getBean(), gameUuid);
    final Map<String, String> playerTypes = new HashMap<>();
    final Map<String, Boolean> playersEnabled = new HashMap<>();
    for (final PlayerSelectorRow player : this.playerTypes) {
      playerTypes.put(player.getPlayerName(), player.getPlayerType());
      playersEnabled.put(player.getPlayerName(), player.isPlayerEnabled());
    }
    // we don't need the playerToNode list, the
    // disable-able players, or the alliances
    // list, for a local game
    final PlayerListing pl =
        new PlayerListing(null, playersEnabled, playerTypes, gameSelectorModel.getGameData().getGameVersion(),
            gameSelectorModel.getGameName(), gameSelectorModel.getGameRound(), null, null);
    return Optional.of(new LocalLauncher(gameSelectorModel, randomSource, pl));
  }

  /**
   * A cache for serialized beans that should be stored locally.
   * This is used to store settings which are not game related, and should therefore not go into the options cache
   * This is often used by editors to remember previous values
   */
  private enum LocalBeanCache {
    INSTANCE;
    private final File file;
    private final Object mutex = new Object();

    Map<String, IBean> map = new HashMap<>();

    LocalBeanCache() {
      file = new File(ClientFileSystemHelper.getUserRootFolder(), "local.cache");
      map = loadMap();
      // add a shutdown, just in case someone forgets to call writeToDisk
      Runtime.getRuntime().addShutdownHook(new Thread(this::writeToDisk));
    }

    @SuppressWarnings("unchecked")
    private Map<String, IBean> loadMap() {
      if (file.exists()) {
        try (FileInputStream fin = new FileInputStream(file);
            ObjectInput oin = new ObjectInputStream(fin)) {
          final Object o = oin.readObject();
          if (o instanceof Map) {
            final Map<?, ?> m = (Map<?, ?>) o;
            for (final Object o1 : m.keySet()) {
              if (!(o1 instanceof String)) {
                throw new Exception("Map is corrupt");
              }
            }
          } else {
            throw new Exception("File is corrupt");
          }
          // we know that the map has proper type key/value
          return (Map<String, IBean>) o;
        } catch (final Exception e) {
          // on error we delete the cache file, if we can
          file.delete();
          ClientLogger.logQuietly("serialized local bean cache invalid", e);
        }
      }
      return new HashMap<>();
    }

    /**
     * adds a new Serializable to the cache
     *
     * @param key the key the serializable should be stored under. Take care not to override a serializable stored by
     *        other code it is generally a good ide to use fully qualified class names, getClass().getCanonicalName() as
     *        key
     * @param bean the bean
     */
    void storeSerializable(final String key, final IBean bean) {
      map.put(key, bean);
    }

    /**
     * Call to have the cache written to disk.
     */
    void writeToDisk() {
      synchronized (mutex) {
        try (FileOutputStream fout = new FileOutputStream(file, false);
            ObjectOutputStream out = new ObjectOutputStream(fout)) {
          out.writeObject(map);
        } catch (final IOException e) {
          ClientLogger.logQuietly("failed to write local bean cache", e);
        }
      }
    }

    /**
     * Get a serializable from the cache.
     *
     * @param key
     *        the key ot was stored under
     * @return the serializable or null if one doesn't exists under the given key
     */
    IBean getSerializable(final String key) {
      return map.get(key);
    }
  }
}
