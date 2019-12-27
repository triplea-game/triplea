package games.strategy.engine.framework.startup.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.HeadedLaunchAction;
import games.strategy.engine.framework.startup.ui.editors.DiceServerEditor;
import games.strategy.engine.framework.startup.ui.editors.EmailSenderEditor;
import games.strategy.engine.framework.startup.ui.editors.ForumPosterEditor;
import games.strategy.engine.random.PbemDiceRoller;
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
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.triplea.swing.SwingAction;

/**
 * A panel for setting up Play by Email/Forum. This panel listens to the GameSelectionModel so it
 * can refresh when a new game is selected or save game loaded The MainPanel also listens to this
 * panel, and we notify it through the notifyObservers()
 */
public class PbemSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = 9006941131918034674L;
  private final GameSelectorModel gameSelectorModel;
  private final DiceServerEditor diceServerEditor;
  private final ForumPosterEditor forumPosterEditor;
  private final EmailSenderEditor emailSenderEditor;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();
  private final JPanel localPlayerPanel = new JPanel();
  private final JButton localPlayerSelection = new JButton("Select Local Players and AI's");

  /**
   * Creates a new instance.
   *
   * @param model the GameSelectionModel, though which changes are obtained when new games are
   *     chosen, or save games loaded
   */
  public PbemSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    diceServerEditor = new DiceServerEditor(this::fireListener);
    forumPosterEditor = new ForumPosterEditor(this::fireListener);
    emailSenderEditor = new EmailSenderEditor(this::fireListener);
    createComponents();
    layoutComponents();
    setupListeners();
    loadAll();
  }

  private void createComponents() {
    final JScrollPane scrollPane = new JScrollPane(localPlayerPanel);
    localPlayerPanel.addHierarchyListener(
        e -> {
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
        e ->
            JOptionPane.showMessageDialog(
                PbemSetupPanel.this,
                scrollPane,
                "Select Local Players and AI's",
                JOptionPane.PLAIN_MESSAGE));
  }

  private void layoutComponents() {
    removeAll();
    setLayout(new GridBagLayout());
    // Empty border works as margin
    setBorder(new EmptyBorder(10, 10, 10, 10));
    int row = 0;
    add(
        diceServerEditor,
        new GridBagConstraints(
            0,
            row++,
            1,
            1,
            1.0d,
            0d,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(10, 0, 20, 0),
            0,
            0));

    final JTabbedPane tabbedPane = new JTabbedPane();
    add(
        tabbedPane,
        new GridBagConstraints(
            0,
            row++,
            1,
            1,
            1.0d,
            0d,
            GridBagConstraints.NORTHWEST,
            GridBagConstraints.HORIZONTAL,
            new Insets(10, 0, 20, 0),
            0,
            0));
    tabbedPane.addTab("Play By Forum", forumPosterEditor);
    tabbedPane.addTab("Play By Email", emailSenderEditor);

    // add selection of local players
    add(
        localPlayerSelection,
        new GridBagConstraints(
            0,
            row,
            1,
            1,
            1.0d,
            0d,
            GridBagConstraints.NORTHEAST,
            GridBagConstraints.NONE,
            new Insets(10, 0, 10, 0),
            0,
            0));
    layoutPlayerComponents(localPlayerPanel, playerTypes, gameSelectorModel.getGameData());
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  @Override
  public boolean isCancelButtonVisible() {
    return true;
  }

  private void setupListeners() {
    // register, so we get notified when the game model (GameData) changes
    // (e.g if the user load a save game or selects another game)
    gameSelectorModel.addObserver(this);
  }

  private void loadAll() {
    Optional.ofNullable(gameSelectorModel.getGameData())
        .map(GameData::getProperties)
        .ifPresent(
            properties -> {
              diceServerEditor.populateFromGameProperties(properties);
              forumPosterEditor.populateFromGameProperties(properties);
              emailSenderEditor.populateFromGameProperties(properties);
            });
  }

  /** Called when the current game changes. */
  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  /** Called when the observers detect change, to see if the game is in a startable state. */
  @Override
  public boolean canGameStart() {
    final boolean diceServerValid = diceServerEditor.areFieldsValid();
    final boolean forumValid = forumPosterEditor.areFieldsValid();
    final boolean emailValid = emailSenderEditor.areFieldsValid();
    final boolean ready =
        diceServerValid && (forumValid || emailValid) && gameSelectorModel.getGameData() != null;
    // make sure at least 1 player is enabled
    return ready && playerTypes.stream().anyMatch(PlayerSelectorRow::isPlayerEnabled);
  }

  @Override
  public void postStartGame() {
    final GameData data = gameSelectorModel.getGameData();
    if (diceServerEditor.areFieldsValid()) {
      diceServerEditor.applyToGameProperties(data.getProperties());
    }
    if (forumPosterEditor.areFieldsValid()) {
      forumPosterEditor.applyToGameProperties(data.getProperties());
    }
    if (emailSenderEditor.areFieldsValid()) {
      emailSenderEditor.applyToGameProperties(data.getProperties());
    }
  }

  /**
   * Is called in response to the GameSelectionModel being updated. It means the we have to reload
   * the form
   *
   * @param o always null
   * @param arg always null
   */
  @Override
  public void update(final Observable o, final Object arg) {
    SwingAction.invokeNowOrLater(
        () -> {
          loadAll();
          layoutComponents();
        });
  }

  /** Called when the user hits play. */
  @Override
  public Optional<ILauncher> getLauncher() {
    final PbemDiceRoller randomSource = new PbemDiceRoller(diceServerEditor.newDiceServer());
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
        new PlayerListing(
            null,
            playersEnabled,
            playerTypes,
            gameSelectorModel.getGameData().getGameVersion(),
            gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound(),
            null,
            null);
    return Optional.of(
        new LocalLauncher(gameSelectorModel, randomSource, pl, this, new HeadedLaunchAction(this)));
  }
}
