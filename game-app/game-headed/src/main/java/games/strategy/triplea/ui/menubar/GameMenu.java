package games.strategy.triplea.ui.menubar;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.triplea.odds.calculator.BattleCalculatorDialog;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.PoliticalStateOverview;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.ui.VerifiedRandomNumbersDialog;
import games.strategy.triplea.ui.statistics.StatisticsDialog;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.triplea.sound.SoundOptions;
import org.triplea.swing.IntTextField;
import org.triplea.swing.JMenuBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;
import org.triplea.swing.key.binding.KeyCode;

final class GameMenu extends JMenu {
  private static final long serialVersionUID = -6273782490069588052L;

  private final TripleAFrame frame;
  private final UiContext uiContext;
  private final GameData gameData;
  private final IGame game;

  GameMenu(final TripleAFrame frame) {
    super("Game");

    this.frame = frame;
    game = frame.getGame();
    gameData = frame.getGame().getData();
    uiContext = frame.getUiContext();

    setMnemonic(KeyEvent.VK_G);

    addEditMode();
    addSeparator();
    add(SwingAction.of("Engine Settings", e -> ClientSetting.showSettingsWindow(frame)));
    add(SoundOptions.buildGlobalSoundSwitchMenuItem());
    add(SoundOptions.buildSoundOptionsMenuItem());
    addSeparator();
    addMenuItemWithHotkey(frame.getShowGameAction(), KeyEvent.VK_G);
    addMenuItemWithHotkey(
        frame.getShowHistoryAction(),
        // 'H' is a reserved hotkey in Mac, used to minimize apps use 'Y' instead for MacOs.
        SystemProperties.isMac() ? KeyEvent.VK_Y : KeyEvent.VK_H);
    addSeparator();
    addGameOptionsMenu();
    addShowVerifiedDice();
    addPoliticsMenu();
    addNotificationSettings();
    addShowDiceStats();
    addRollDice();
    if (ClientSetting.showBetaFeatures.getValueOrThrow()) {
      addStatistics();
    }
    addMenuItemWithHotkey(
        SwingAction.of(
            "Battle Calculator", e -> BattleCalculatorDialog.show(frame, null, gameData)),
        KeyEvent.VK_B);
  }

  private void addMenuItemWithHotkey(final Action action, final int keyCode) {
    final JMenuItem gameMenuItem = add(action);
    gameMenuItem.setMnemonic(keyCode);
    gameMenuItem.setAccelerator(
        KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
  }

  private void addEditMode() {
    final JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
    editMode.setModel(frame.getEditModeButtonModel());
    final JMenuItem editMenuItem = add(editMode);
    editMenuItem.setMnemonic(KeyEvent.VK_E);
    editMenuItem.setAccelerator(
        KeyStroke.getKeyStroke(
            KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
  }

  private void addShowVerifiedDice() {
    final Action showVerifiedDice =
        SwingAction.of(
            "Show Verified Dice",
            e -> new VerifiedRandomNumbersDialog(frame.getRootPane()).setVisible(true));
    if (game instanceof ClientGame) {
      add(showVerifiedDice).setMnemonic(KeyEvent.VK_V);
    }
  }

  private void addGameOptionsMenu() {
    if (!gameData.getProperties().getEditableProperties().isEmpty()) {
      add(SwingAction.of(
              "Game Options",
              e -> {
                final PropertiesUi ui =
                    new PropertiesUi(gameData.getProperties().getEditableProperties(), false);
                JOptionPane.showMessageDialog(frame, ui, "Game Options", JOptionPane.PLAIN_MESSAGE);
              }))
          .setMnemonic(KeyEvent.VK_O);
    }
  }

  /**
   * Add a Politics Panel button to the game menu, this panel will show the current political
   * landscape as a reference, no actions on this panel.
   */
  private void addPoliticsMenu() {
    final JMenuItem politicsMenuItem =
        add(
            SwingAction.of(
                "Show Politics Panel",
                e -> {
                  final PoliticalStateOverview ui =
                      new PoliticalStateOverview(gameData, uiContext, false);
                  final JScrollPane scroll = new JScrollPane(ui);
                  scroll.setBorder(BorderFactory.createEmptyBorder());
                  final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
                  // not only do we have a start bar, but we also have the message dialog to account
                  // for
                  final int availHeight = screenResolution.height - 120;
                  // just the scroll bars plus the window sides
                  final int availWidth = screenResolution.width - 40;

                  scroll.setPreferredSize(
                      new Dimension(
                          Math.min(scroll.getPreferredSize().width, availWidth),
                          Math.min(scroll.getPreferredSize().height, availHeight)));

                  JOptionPane.showMessageDialog(
                      frame, scroll, "Politics Panel", JOptionPane.PLAIN_MESSAGE);
                }));
    politicsMenuItem.setMnemonic(KeyEvent.VK_P);
    // On Mac, Cmd-W is the standard "close window" shortcut, which we use for "Leave Game".
    final int keyCode = (SystemProperties.isMac() ? KeyEvent.VK_O : KeyEvent.VK_W);
    politicsMenuItem.setAccelerator(
        KeyStroke.getKeyStroke(keyCode, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
  }

  private void addNotificationSettings() {
    final JCheckBoxMenuItem showEndOfTurnReport = new JCheckBoxMenuItem("Show End of Turn Report");
    showEndOfTurnReport.setMnemonic(KeyEvent.VK_R);
    final JCheckBoxMenuItem showTriggeredNotifications =
        new JCheckBoxMenuItem("Show Triggered Notifications");
    showTriggeredNotifications.setMnemonic(KeyEvent.VK_T);
    final JCheckBoxMenuItem showTriggerChanceSuccessful =
        new JCheckBoxMenuItem("Show Trigger/Condition Chance Roll Successful");
    showTriggerChanceSuccessful.setMnemonic(KeyEvent.VK_S);
    final JCheckBoxMenuItem showTriggerChanceFailure =
        new JCheckBoxMenuItem("Show Trigger/Condition Chance Roll Failure");
    showTriggerChanceFailure.setMnemonic(KeyEvent.VK_F);
    showEndOfTurnReport.addActionListener(
        e -> uiContext.setShowEndOfTurnReport(showEndOfTurnReport.isSelected()));
    showTriggeredNotifications.addActionListener(
        e -> uiContext.setShowTriggeredNotifications(showTriggeredNotifications.isSelected()));
    showTriggerChanceSuccessful.addActionListener(
        e -> uiContext.setShowTriggerChanceSuccessful(showTriggerChanceSuccessful.isSelected()));
    showTriggerChanceFailure.addActionListener(
        e -> uiContext.setShowTriggerChanceFailure(showTriggerChanceFailure.isSelected()));
    final JMenu notificationMenu =
        new JMenuBuilder("User Notifications", KeyCode.U)
            .addMenuItem(showEndOfTurnReport)
            .addMenuItem(showTriggeredNotifications)
            .addMenuItem(showTriggerChanceSuccessful)
            .addMenuItem(showTriggerChanceFailure)
            .build();
    notificationMenu.addMenuListener(
        new MenuListener() {
          @Override
          public void menuSelected(final MenuEvent e) {
            showEndOfTurnReport.setSelected(uiContext.getShowEndOfTurnReport());
            showTriggeredNotifications.setSelected(uiContext.getShowTriggeredNotifications());
            showTriggerChanceSuccessful.setSelected(uiContext.getShowTriggerChanceSuccessful());
            showTriggerChanceFailure.setSelected(uiContext.getShowTriggerChanceFailure());
          }

          @Override
          public void menuDeselected(final MenuEvent e) {}

          @Override
          public void menuCanceled(final MenuEvent e) {}
        });
    add(notificationMenu);
  }

  private void addShowDiceStats() {
    add(SwingAction.of(
            "Show Dice Stats",
            e -> {
              final IRandomStats randomStats =
                  (IRandomStats)
                      game.getMessengers().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);
              final RandomStatsDetails stats = randomStats.getRandomStats(gameData.getDiceSides());
              JOptionPane.showMessageDialog(
                  frame,
                  new JScrollPane(stats.getAllStats()),
                  "Random Stats",
                  JOptionPane.INFORMATION_MESSAGE);
            }))
        .setMnemonic(KeyEvent.VK_D);
  }

  private void addRollDice() {
    final JMenuItem rollDiceBox = new JMenuItem("Roll Dice");
    rollDiceBox.setMnemonic(KeyEvent.VK_R);
    rollDiceBox.addActionListener(
        e -> {
          final IntTextField numberOfText = new IntTextField(0, 100);
          final IntTextField diceSidesText = new IntTextField(1, 200);
          numberOfText.setText(String.valueOf(0));
          diceSidesText.setText(String.valueOf(gameData.getDiceSides()));
          final JPanel panel = new JPanel();
          panel.setLayout(new GridBagLayout());
          panel.add(
              new JLabel("Number of Dice to Roll: "),
              new GridBagConstraintsBuilder(0, 0)
                  .anchor(GridBagConstraintsAnchor.WEST)
                  .fill(GridBagConstraintsFill.BOTH)
                  .insets(0, 0, 0, 20)
                  .build());
          panel.add(
              new JLabel("Sides on the Dice: "),
              new GridBagConstraintsBuilder(2, 0)
                  .anchor(GridBagConstraintsAnchor.WEST)
                  .fill(GridBagConstraintsFill.BOTH)
                  .insets(0, 20, 0, 10)
                  .build());
          panel.add(
              numberOfText,
              new GridBagConstraintsBuilder(0, 1)
                  .anchor(GridBagConstraintsAnchor.WEST)
                  .fill(GridBagConstraintsFill.BOTH)
                  .insets(0, 0, 0, 20)
                  .build());
          panel.add(
              diceSidesText,
              new GridBagConstraintsBuilder(2, 1)
                  .anchor(GridBagConstraintsAnchor.WEST)
                  .fill(GridBagConstraintsFill.BOTH)
                  .insets(0, 20, 0, 10)
                  .build());
          JOptionPane.showOptionDialog(
              JOptionPane.getFrameForComponent(this),
              panel,
              "Roll Dice",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.INFORMATION_MESSAGE,
              null,
              new String[] {"OK"},
              "OK");
          try {
            final int numberOfDice = Integer.parseInt(numberOfText.getText());
            if (numberOfDice > 0) {
              final int diceSides = Integer.parseInt(diceSidesText.getText());
              final int[] dice =
                  game.getRandomSource()
                      .getRandom(diceSides, numberOfDice, "Rolling Dice, no effect on game.");
              final JPanel panelDice = new JPanel();
              final BoxLayout layout = new BoxLayout(panelDice, BoxLayout.Y_AXIS);
              panelDice.setLayout(layout);
              final JLabel label = new JLabel("Rolls (no effect on game): ");
              panelDice.add(label);
              final StringBuilder diceString = new StringBuilder();
              for (int i = 0; i < dice.length; i++) {
                diceString.append((dice[i] + 1)).append((i == dice.length - 1) ? "" : ", ");
              }
              final JTextField diceList = new JTextField(diceString.toString());
              diceList.setEditable(false);
              panelDice.add(diceList);
              JOptionPane.showMessageDialog(
                  frame, panelDice, "Dice Rolled", JOptionPane.INFORMATION_MESSAGE);
            }
          } catch (final Exception ex) {
            // ignore malformed input
          }
        });
    add(rollDiceBox);
  }

  private void addStatistics() {
    add(
        SwingAction.of(
            "Game Statistics",
            e ->
                JOptionPane.showMessageDialog(
                    frame,
                    new StatisticsDialog(gameData, uiContext.getMapData()),
                    "Game Statistics",
                    JOptionPane.INFORMATION_MESSAGE)));
  }
}
