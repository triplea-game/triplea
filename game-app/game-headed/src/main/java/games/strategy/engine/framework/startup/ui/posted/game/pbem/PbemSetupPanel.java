package games.strategy.engine.framework.startup.ui.posted.game.pbem;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.GameState;
import games.strategy.engine.framework.I18nEngineFramework;
import games.strategy.engine.framework.message.PlayerListing;
import games.strategy.engine.framework.startup.launcher.ILauncher;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.mc.HeadedLaunchAction;
import games.strategy.engine.framework.startup.mc.HeadedPlayerTypes;
import games.strategy.engine.framework.startup.ui.PlayerSelectorRow;
import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.engine.framework.startup.ui.SetupPanel;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.posted.game.DiceServerEditor;
import games.strategy.engine.random.PbemDiceRoller;
import games.strategy.triplea.settings.ClientSetting;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
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
import javax.swing.SwingUtilities;
import org.triplea.swing.SectionHeader;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import org.triplea.util.ExitStatus;

/**
 * A panel for setting up Play by Email/Forum. This panel listens to the GameSelectionModel so it
 * can refresh when a new game is selected or save game loaded The MainPanel also listens to this
 * panel, and we notify it through the notifyObservers()
 */
public class PbemSetupPanel extends SetupPanel implements Observer {
  private static final long serialVersionUID = -4027051961383144244L;
  private final GameSelectorModel gameSelectorModel;
  private final DiceServerEditor diceServerEditor;
  private final EmailSenderEditor emailSenderEditor;
  private final List<PlayerSelectorRow> playerTypes = new ArrayList<>();
  private final JPanel localPlayerPanel = new JPanel();
  private final JButton localPlayerSelection =
      new JButton(I18nEngineFramework.get().getText("startup.SetupPanel.btn.PlayerSelection.Lbl"));

  /**
   * Creates a new instance.
   *
   * @param model the GameSelectionModel, through which changes are obtained when new games are
   *     chosen, or save games loaded
   */
  public PbemSetupPanel(final GameSelectorModel model) {
    gameSelectorModel = model;
    diceServerEditor = new DiceServerEditor(this::fireListener);
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
                I18nEngineFramework.get()
                    .getText("startup.SetupPanel.PlayerSelection.Dialog.Title"),
                JOptionPane.PLAIN_MESSAGE));
  }

  private void layoutComponents() {
    removeAll();
    setLayout(new GridBagLayout());
    // Empty border works as margin
    int row = 0;
    add(
        diceServerEditor,
        new GridBagConstraintsBuilder(0, row++)
            .gridWidth(1)
            .gridHeight(1)
            .weightX(1.0)
            .weightY(0.0)
            .anchor(GridBagConstraintsAnchor.NORTHWEST)
            .fill(GridBagConstraintsFill.HORIZONTAL)
            .insets(0, 0, 16, 0)
            .build());

    final JPanel emailSection = new JPanel(new BorderLayout());
    emailSection.add(SectionHeader.create("Email"), BorderLayout.NORTH);
    emailSection.add(emailSenderEditor.build(), BorderLayout.WEST);
    add(
        emailSection,
        new GridBagConstraintsBuilder(0, row++)
            .gridWidth(1)
            .gridHeight(1)
            .weightX(1.0)
            .weightY(0.0)
            .anchor(GridBagConstraintsAnchor.NORTHWEST)
            .fill(GridBagConstraintsFill.HORIZONTAL)
            .insets(0, 0, 16, 0)
            .build());

    add(
        SectionHeader.create(
            I18nEngineFramework.get().getText("startup.SetupPanel.players.Lbl"),
            localPlayerSelection),
        new GridBagConstraintsBuilder(0, row++)
            .weightX(1.0)
            .anchor(GridBagConstraintsAnchor.NORTHWEST)
            .fill(GridBagConstraintsFill.HORIZONTAL)
            .insets(0, 0, 0, 0)
            .build());
    // pushes the sections to the top
    add(
        new JPanel(),
        new GridBagConstraintsBuilder(0, row)
            .weightY(1.0)
            .fill(GridBagConstraintsFill.BOTH)
            .build());
    layoutPlayerComponents(localPlayerPanel, playerTypes, gameSelectorModel.getGameData());
  }

  @Override
  public List<Action> getUserActions() {
    return List.of();
  }

  private void setupListeners() {
    // register, so we get notified when the game model (GameData) changes
    // (e.g if the user load a save game or selects another game)
    gameSelectorModel.addObserver(this);
  }

  private void loadAll() {
    Optional.ofNullable(gameSelectorModel.getGameData())
        .map(GameState::getProperties)
        .ifPresent(
            properties -> {
              diceServerEditor.populateFromGameProperties(properties);
              emailSenderEditor.populateFromGameProperties(properties);
            });
  }

  /** Called when the current game changes. */
  @Override
  public void cancel() {
    gameSelectorModel.deleteObserver(this);
  }

  /** Called when the observers detect change, to see if the game is in a start-able state. */
  @Override
  public boolean canGameStart() {
    final boolean diceServerValid = diceServerEditor.areFieldsValid();
    final boolean emailFieldsValid = emailSenderEditor.areFieldsValid();
    final boolean gameSelected = emailFieldsValid && gameSelectorModel.getGameData() != null;
    final boolean atLeastOnePlayerIsEnabled =
        playerTypes.stream().anyMatch(PlayerSelectorRow::isPlayerEnabled);
    return diceServerValid && emailFieldsValid && gameSelected && atLeastOnePlayerIsEnabled;
  }

  @Override
  public Optional<String> getStartBlockedReason() {
    if (gameSelectorModel.getGameData() == null) {
      return Optional.of("Select a game to play");
    }
    if (!diceServerEditor.areFieldsValid()) {
      return Optional.of("Enter valid To and Cc email addresses for the dice server");
    }
    if (!emailSenderEditor.areFieldsValid()) {
      return Optional.of("Complete the email settings");
    }
    if (playerTypes.stream().noneMatch(PlayerSelectorRow::isPlayerEnabled)) {
      return Optional.of("Enable at least one player");
    }
    return Optional.empty();
  }

  @Override
  public void postStartGame() {
    final GameState data = gameSelectorModel.getGameData();

    Preconditions.checkNotNull(
        data,
        "Game Data must not be null when starting a game, "
            + "this error indicates a programming bug that allowed for the start game button to be "
            + "enabled without first valid game data being loaded. ");
    if (diceServerEditor.areFieldsValid()) {
      diceServerEditor.applyToGameProperties(data.getProperties());
    }
    emailSenderEditor.applyToGameProperties(data.getProperties());
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
    Preconditions.checkNotNull(
        gameSelectorModel.getGameData(),
        "Game Data must not be null when launching a game, "
            + "this error indicates a programming bug that allowed for the start game button to be "
            + "enabled without first valid game data being loaded. ");

    if (emailSenderEditor.isForgetPasswordOnShutdown()) {
      ExitStatus.addExitAction(
          () -> {
            ClientSetting.emailPassword.resetValue();
            ClientSetting.flush();
          });
    }

    final PbemDiceRoller randomSource = new PbemDiceRoller(diceServerEditor.newDiceServer());
    final Map<String, PlayerTypes.Type> playerTypes = new HashMap<>();
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
            gameSelectorModel.getGameName(),
            gameSelectorModel.getGameRound(),
            null,
            null);
    return Optional.of(
        new LocalLauncher(
            gameSelectorModel,
            randomSource,
            pl,
            this,
            new HeadedLaunchAction(this),
            new PlayerTypes(HeadedPlayerTypes.getPlayerTypes())));
  }
}
