package games.strategy.engine.framework.startup.ui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.pbem.AxisAndAlliesForumPoster;
import games.strategy.engine.pbem.IEmailSender;
import games.strategy.engine.pbem.IForumPoster;
import games.strategy.engine.pbem.PbemMessagePoster;
import games.strategy.engine.pbem.TripleAForumPoster;
import games.strategy.engine.random.IRemoteDiceServer;
import games.strategy.engine.random.InternalDiceServer;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.engine.random.PropertiesDiceRoller;
import games.strategy.ui.SwingAction;
import lombok.extern.java.Log;
import swinglib.JPanelBuilder;

/**
 * A panel for setting up Play by Email/Forum.
 * This panel listens to the GameSelectionModel so it can refresh when a new game is selected or save game loaded
 * The MainPanel also listens to this panel, and we notify it through the notifyObservers()
 */
@Log
public class PbemSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 9006941131918034674L;
  private static final String DICE_ROLLER = "games.strategy.engine.random.IRemoteDiceServer";
  private final GameSelectorModel gameSelectorModel;
  private final JPanel diceServerEditor;
  private final JPanel forumPosterEditor;
  private final JPanel emailSenderEditor;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();
  private final JPanel localPlayerPanel = new JPanel();
  private final JButton localPlayerSelection = new JButton("Select Local Players and AI's");

  /**
   * Creates a new instance.
   *
   * @param model the GameSelectionModel, though which changes are obtained when new games are chosen, or save games
   *        loaded
   */
  public PbemSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    diceServerEditor = JPanelBuilder.builder().add(new JLabel("Dice Server")).build();
    forumPosterEditor = JPanelBuilder.builder().add(new JLabel("Post to Forum")).build();
    emailSenderEditor = JPanelBuilder.builder().add(new JLabel("Provider")).build();
    createComponents();
    layoutComponents();
    setupListeners();
    loadAll();
  }

  private void createComponents() {
    final JScrollPane scrollPane = new JScrollPane(localPlayerPanel);
    localPlayerPanel.addHierarchyListener(e -> {
      final Window window = SwingUtilities.getWindowAncestor(localPlayerPanel);
      if (window instanceof Dialog) {
        final Dialog dialog = (Dialog) window;
        if (!dialog.isResizable()) {
          dialog.setResizable(true);
          dialog.setMinimumSize(new Dimension(700, 700));
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

    emailPanel.add(emailSenderEditor, new GridBagConstraints(0, 0, 1, 1, 1.0d, 0d,
        GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 2, 0), 0, 0));

    // add selection of local players
    add(localPlayerSelection, new GridBagConstraints(0, row, 1, 1, 1.0d, 0d, GridBagConstraints.NORTHEAST,
        GridBagConstraints.NONE, new Insets(10, 0, 10, 0), 0, 0));
    layoutPlayerComponents(localPlayerPanel, playerTypes, gameSelectorModel.getGameData());
  }

  @Override
  public boolean showCancelButton() {
    return true;
  }

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
   */
  private void loadDiceServer(final GameData data) {
    final List<IRemoteDiceServer> diceRollers = new ArrayList<>(PropertiesDiceRoller.loadFromFile());
    diceRollers.add(new InternalDiceServer());
    if (gameSelectorModel.isSavedGame()) {
      // get the dice roller from the save game, if any
      // FIXME fill fields from savegame
      // final IRemoteDiceServer roller = (IRemoteDiceServer) data.getProperties().get(DICE_ROLLER);
    }
  }

  /**
   * Load the Forum poster that are stored in the GameData, and select it in the list.
   * Sensitive information such as passwords are not stored in save games, so the are loaded from the LocalBeanCache
   */
  private void loadForumPosters(final GameData data) {
    // get the forum posters,
    final List<IForumPoster> forumPosters = new ArrayList<>();
    //forumPosters.add(new AxisAndAlliesForumPoster());
    //forumPosters.add(new TripleAForumPoster());
    // now get the poster stored in the save game
    final IForumPoster forumPoster = (IForumPoster) data.getProperties().get(PbemMessagePoster.FORUM_POSTER_PROP_NAME);
    if (forumPoster != null) {
      // if we have a cached version, use the credentials from this, as each player has different forum login
      // FIXME load from savegame
    }
  }

  /**
   * Configures the list of Email senders. If the game was saved we use this email sender.
   * Since passwords are not stored in save games, the LocalBeanCache is checked
   *
   * @param data the game data
   */
  private void loadEmailSender(final GameData data) {
    // The list of email, either loaded from cache or created
    final List<IEmailSender> emailSenders = new ArrayList<>();
    // emailSenders.add(new GenericEmailSender());
    // now get the sender from the save game, update it with credentials from the cache, and set it
    final IEmailSender sender = (IEmailSender) data.getProperties().get(PbemMessagePoster.EMAIL_SENDER_PROP_NAME);
    if (sender != null) {
      // FIXME load from savegame
    }
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
    // final boolean diceServerValid = diceServerEditor.isBeanValid();
    // final boolean summaryValid = forumPosterEditor.isBeanValid();
    // final boolean emailValid = emailSenderEditor.isBeanValid();
    final boolean pbemReady = true; // diceServerValid && summaryValid && emailValid && gameSelectorModel.getGameData() != null;
    if (!pbemReady) {
      return false;
    }
    // make sure at least 1 player is enabled
    return playerTypes.stream().anyMatch(PlayerSelectorRow::isPlayerEnabled);
  }

  @Override
  public void postStartGame() {
    // store the dice server
    final GameData data = gameSelectorModel.getGameData();
    // data.getProperties().set(DICE_ROLLER, diceServerEditor.getBean());
    // store the Turn Summary Poster
    // final IForumPoster poster = (IForumPoster) forumPosterEditor.getBean();
    /*if (poster != null) {
      // clone the poster, the remove sensitive info, and put the clone into the game data
      // this was the sensitive info is not stored in the save game, but the user cache still has the password
      final IForumPoster summaryPoster = poster.doClone();
      summaryPoster.clearSensitiveInfo();
      data.getProperties().set(PbemMessagePoster.FORUM_POSTER_PROP_NAME, summaryPoster);
    }*/
    // store the email poster
    /*IEmailSender sender = (IEmailSender) emailSenderEditor.getBean();
    if (sender != null) {
      // create a clone, delete the sensitive information in the clone, and use it in the game
      // the locally cached version still has the password so the user doesn't have to enter it every time
      data.getProperties().set(PbemMessagePoster.EMAIL_SENDER_PROP_NAME, sender);
    }
    // store whether we are a pbem game or not, whether we are capable of posting a game save
    if (poster != null || sender != null) {
      data.getProperties().set(PbemMessagePoster.PBEM_GAME_PROP_NAME, true);
    }*/
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
    // create local launcher
    final PbemDiceRoller randomSource = new PbemDiceRoller(null /* (IRemoteDiceServer) diceServerEditor.getBean()*/);
    final Map<String, PlayerType> playerTypes = new HashMap<>();
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

  @Override
  public JComponent getDrawable() {
    return this;
  }
}
