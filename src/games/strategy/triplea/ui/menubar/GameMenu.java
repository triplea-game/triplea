package games.strategy.triplea.ui.menubar;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
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

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.IGame;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.sound.SoundOptions;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.settings.SettingsWindow;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.PoliticalStateOverview;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.VerifiedRandomNumbersDialog;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;

public class GameMenu {

  private final TripleAFrame frame;
  private final IUIContext iuiContext;
  private final GameData gameData;
  private final IGame game;

  public GameMenu(final TripleAMenuBar menuBar, final TripleAFrame frame) {
    this.frame = frame;
    game = frame.getGame();
    gameData = frame.getGame().getData();
    iuiContext = frame.getUIContext();

    menuBar.add(createGameMenu());
  }

  private JMenu createGameMenu() {
    final JMenu menuGame = SwingComponents.newJMenu("Game", SwingComponents.KeyboardCode.G);
    addEditMode(menuGame);

    menuGame.addSeparator();
    menuGame.add(SwingAction.of("Engine Settings", e -> SettingsWindow.showWindow()));
    SoundOptions.addGlobalSoundSwitchMenu(menuGame);
    SoundOptions.addToMenu(menuGame);
    menuGame.addSeparator();
    menuGame.add(frame.getShowGameAction()).setMnemonic(KeyEvent.VK_G);
    menuGame.add(frame.getShowHistoryAction()).setMnemonic(KeyEvent.VK_H);
    menuGame.add(frame.getShowMapOnlyAction()).setMnemonic(KeyEvent.VK_M);

    menuGame.addSeparator();
    addGameOptionsMenu(menuGame);
    addShowVerifiedDice(menuGame);
    addPoliticsMenu(menuGame);
    addNotificationSettings(menuGame);
    addShowDiceStats(menuGame);
    addRollDice(menuGame);
    addBattleCalculatorMenu(menuGame);
    return menuGame;
  }

  private void addEditMode(final JMenu parentMenu) {
    final JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
    editMode.setModel(frame.getEditModeButtonModel());
    parentMenu.add(editMode).setMnemonic(KeyEvent.VK_E);
  }



  private void addShowVerifiedDice(final JMenu parentMenu) {
    final Action showVerifiedDice = SwingAction.of("Show Verified Dice..",
        e -> new VerifiedRandomNumbersDialog(frame.getRootPane()).setVisible(true));
    if (game instanceof ClientGame) {
      parentMenu.add(showVerifiedDice).setMnemonic(KeyEvent.VK_V);
    }
  }

  protected void addGameOptionsMenu(final JMenu menuGame) {
    if (!gameData.getProperties().getEditableProperties().isEmpty()) {
      final AbstractAction optionsAction = SwingAction.of("Game Options...", e -> {
        final PropertiesUI ui = new PropertiesUI(gameData.getProperties().getEditableProperties(), false);
        JOptionPane.showMessageDialog(frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
      });
      menuGame.add(optionsAction).setMnemonic(KeyEvent.VK_O);
    }
  }

  /**
   * Add a Politics Panel button to the game menu, this panel will show the
   * current political landscape as a reference, no actions on this panel.
   *
   * @param menuGame
   */
  private void addPoliticsMenu(final JMenu menuGame) {
    final AbstractAction politicsAction = SwingAction.of("Show Politics Panel", e -> {
      final PoliticalStateOverview ui = new PoliticalStateOverview(gameData, iuiContext, false);
      final JScrollPane scroll = new JScrollPane(ui);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      // not only do we have a start bar, but we also have the message dialog to account for
      final int availHeight = screenResolution.height - 120;
      // just the scroll bars plus the window sides
      final int availWidth = screenResolution.width - 40;

      scroll.setPreferredSize(
          new Dimension((scroll.getPreferredSize().width > availWidth ? availWidth : scroll.getPreferredSize().width),
              (scroll.getPreferredSize().height > availHeight ? availHeight : scroll.getPreferredSize().height)));

      JOptionPane.showMessageDialog(frame, scroll, "Politics Panel", JOptionPane.PLAIN_MESSAGE);

    });
    menuGame.add(politicsAction).setMnemonic(KeyEvent.VK_P);
  }

  private void addNotificationSettings(final JMenu parentMenu) {
    final JMenu notificationMenu = new JMenu();
    notificationMenu.setMnemonic(KeyEvent.VK_U);
    notificationMenu.setText("User Notifications...");
    final JCheckBoxMenuItem showEndOfTurnReport = new JCheckBoxMenuItem("Show End of Turn Report");
    showEndOfTurnReport.setMnemonic(KeyEvent.VK_R);
    final JCheckBoxMenuItem showTriggeredNotifications = new JCheckBoxMenuItem("Show Triggered Notifications");
    showTriggeredNotifications.setMnemonic(KeyEvent.VK_T);
    final JCheckBoxMenuItem showTriggerChanceSuccessful =
        new JCheckBoxMenuItem("Show Trigger/Condition Chance Roll Successful");
    showTriggerChanceSuccessful.setMnemonic(KeyEvent.VK_S);
    final JCheckBoxMenuItem showTriggerChanceFailure =
        new JCheckBoxMenuItem("Show Trigger/Condition Chance Roll Failure");
    showTriggerChanceFailure.setMnemonic(KeyEvent.VK_F);
    notificationMenu.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(final MenuEvent e) {
        showEndOfTurnReport.setSelected(iuiContext.getShowEndOfTurnReport());
        showTriggeredNotifications.setSelected(iuiContext.getShowTriggeredNotifications());
        showTriggerChanceSuccessful.setSelected(iuiContext.getShowTriggerChanceSuccessful());
        showTriggerChanceFailure.setSelected(iuiContext.getShowTriggerChanceFailure());
      }

      @Override
      public void menuDeselected(final MenuEvent e) {}

      @Override
      public void menuCanceled(final MenuEvent e) {}
    });
    showEndOfTurnReport.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        iuiContext.setShowEndOfTurnReport(showEndOfTurnReport.isSelected());
      }
    });
    showTriggeredNotifications.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        iuiContext.setShowTriggeredNotifications(showTriggeredNotifications.isSelected());
      }
    });
    showTriggerChanceSuccessful.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        iuiContext.setShowTriggerChanceSuccessful(showTriggerChanceSuccessful.isSelected());
      }
    });
    showTriggerChanceFailure.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        iuiContext.setShowTriggerChanceFailure(showTriggerChanceFailure.isSelected());
      }
    });
    notificationMenu.add(showEndOfTurnReport);
    notificationMenu.add(showTriggeredNotifications);
    notificationMenu.add(showTriggerChanceSuccessful);
    notificationMenu.add(showTriggerChanceFailure);
    parentMenu.add(notificationMenu);
  }

  private void addShowDiceStats(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Show Dice Stats...", e -> {
      final IRandomStats randomStats =
          (IRandomStats) game.getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);
      final RandomStatsDetails stats = randomStats.getRandomStats(gameData.getDiceSides());
      JOptionPane.showMessageDialog(frame, new JScrollPane(stats.getAllStats()), "Random Stats",
          JOptionPane.INFORMATION_MESSAGE);
    });
    parentMenu.add(showDiceStats).setMnemonic(KeyEvent.VK_D);
  }

  private void addRollDice(final JMenu parentMenu) {
    final JMenuItem RollDiceBox = new JMenuItem("Roll Dice...");
    RollDiceBox.setMnemonic(KeyEvent.VK_R);
    RollDiceBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final IntTextField numberOfText = new IntTextField(0, 100);
        final IntTextField diceSidesText = new IntTextField(1, 200);
        numberOfText.setText(String.valueOf(0));
        diceSidesText.setText(String.valueOf(gameData.getDiceSides()));
        final JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.add(new JLabel("Number of Dice to Roll: "), new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
        panel.add(new JLabel("Sides on the Dice: "), new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
        panel.add(numberOfText, new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.BOTH, new Insets(0, 0, 0, 20), 0, 0));
        panel.add(diceSidesText, new GridBagConstraints(2, 1, 1, 1, 0, 0, GridBagConstraints.WEST,
            GridBagConstraints.BOTH, new Insets(0, 20, 0, 10), 0, 0));
        JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(parentMenu), panel, "Roll Dice",
            JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"}, "OK");
        try {
          final int numberOfDice = Integer.parseInt(numberOfText.getText());
          if (numberOfDice > 0) {
            final int diceSides = Integer.parseInt(diceSidesText.getText());
            final int[] dice =
                game.getRandomSource().getRandom(diceSides, numberOfDice, "Rolling Dice, no effect on game.");
            final JPanel panelDice = new JPanel();
            final BoxLayout layout = new BoxLayout(panelDice, BoxLayout.Y_AXIS);
            panelDice.setLayout(layout);
            final JLabel label = new JLabel("Rolls (no effect on game): ");
            panelDice.add(label);
            String diceString = "";
            for (int i = 0; i < dice.length; i++) {
              diceString += String.valueOf(dice[i] + 1) + ((i == dice.length - 1) ? "" : ", ");
            }
            final JTextField diceList = new JTextField(diceString);
            diceList.setEditable(false);
            panelDice.add(diceList);
            JOptionPane.showMessageDialog(frame, panelDice, "Dice Rolled", JOptionPane.INFORMATION_MESSAGE);
          }
        } catch (final Exception ex) {
        }
      }
    });
    parentMenu.add(RollDiceBox);
  }

  private void addBattleCalculatorMenu(final JMenu menuGame) {
    final Action showBattleMenu = SwingAction.of("Battle Calculator...", e -> OddsCalculatorDialog.show(frame, null));
    final JMenuItem showBattleMenuItem = menuGame.add(showBattleMenu);
    showBattleMenuItem.setMnemonic(KeyEvent.VK_B);
    showBattleMenuItem.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_B, java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
  }
}
