package games.strategy.triplea.ui.menubar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;

import games.strategy.triplea.ui.AbstractUIContext;
import games.strategy.triplea.ui.BattleDisplay;
import games.strategy.triplea.ui.ExtendedStats;
import games.strategy.triplea.ui.IUIContext;
import games.strategy.triplea.ui.PoliticalStateOverview;
import games.strategy.triplea.ui.PurchasePanel;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.VerifiedRandomNumbersDialog;
import games.strategy.triplea.ui.menubar.menu.view.ViewMenu;
import games.strategy.ui.SwingAction;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.ColorProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.data.properties.PropertiesUI;
import games.strategy.engine.framework.ClientGame;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.history.Step;
import games.strategy.engine.random.IRandomStats;
import games.strategy.engine.random.RandomStatsDetails;
import games.strategy.engine.stats.IStat;
import games.strategy.sound.SoundOptions;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.EndRoundDelegate;
import games.strategy.triplea.image.MapImage;
import games.strategy.triplea.image.TileImageFactory;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.printgenerator.SetupFrame;
import games.strategy.triplea.ui.screen.IDrawable.OptionalExtraBorderLevel;
import games.strategy.triplea.ui.screen.UnitsDrawer;
import games.strategy.triplea.util.PlayerOrderComparator;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingComponents;
import games.strategy.util.IllegalCharacterRemover;
import games.strategy.util.LocalizeHTML;

/**
 * Main menu for the triplea frame.
 */
public class TripleAMenu extends BasicGameMenuBar {
  private static final long serialVersionUID = 987243661147503593L;

  public TripleAMenu(final TripleAFrame frame) {
    super(frame);
  }

  private IUIContext getUIContext() {
    return frame.getUIContext();
  }

  @Override
  protected void addGameSpecificHelpMenus(final JMenu helpMenu) {
    addMoveHelpMenu(helpMenu);
    addUnitHelpMenu(helpMenu);
  }

  private void addMoveHelpMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Movement/Selection help...", e -> {
      // html formatted string
      final String hints = "<b> Selecting Units</b><br>" + "Left click on a unit stack to select 1 unit.<br>"
          + "ALT-Left click on a unit stack to select 10 units of that type in the stack.<br>"
          + "CTRL-Left click on a unit stack to select all units of that type in the stack.<br>"
          + "Shift-Left click on a unit to select all units in the territory.<br>"
          + "Left click on a territory but not on a unit to bring up a selection window for inputing the desired selection.<br>"
          + "<br><b> Deselecting Units</b><br>"
          + "Right click somewhere not on a unit stack to unselect the last selected unit.<br>"
          + "Right click on a unit stack to unselect one unit in the stack.<br>"
          + "ALT-Right click on a unit stack to unselect 10 units of that type in the stack.<br>"
          + "CTRL-Right click on a unit stack to unselect all units of that type in the stack.<br>"
          + "CTRL-Right click somewhere not on a unit stack to unselect all units selected.<br>"
          + "<br><b> Moving Units</b><br>"
          + "After selecting units Left click on a territory to move units there (do not Left click and Drag, instead select units, then move the mouse, then select the territory).<br>"
          + "CTRL-Left click on a territory to select the territory as a way point (this will force the units to move through this territory on their way to the destination).<br>"
          + "<br><b> Moving the Map Screen</b><br>"
          + "Right click and Drag the mouse to move your screen over the map.<br>"
          + "Left click the map (anywhere), use the arrow keys (or WASD keys) to move your map around. Holding down control will move the map faster.<br />"
          + "Left click in the Minimap at the top right of the screen, and Drag the mouse.<br>"
          + "Move the mouse to the edge of the map to scroll in that direction. Moving the mouse even closer to the edge will scroll faster.<br>"
          + "Scrolling the mouse wheel will move the map up and down.<br>" + "<br><b> Zooming Out</b><br>"
          + "Holding ALT while Scrolling the Mouse Wheel will zoom the map in and out.<br>"
          + "Select 'Zoom' from the 'View' menu, and change to the desired level.<br>"
          + "<br><b> Turn off Map Artwork</b><br>"
          + "Deselect 'Map Details' in the 'View' menu, to show a map without the artwork.<br>"
          + "Select a new 'Map Skin' from the 'View' menu to show a different kind of artwork (not all maps have skins).<br>"
          + "<br><b> Other Things</b><br>"
          + "Press 'n' to cycle through units with movement left (move phases only).<br>"
          + "Press 'f' to highlight all units you own that have movement left (move phases only).<br>"
          + "Press 'i' or 'v' to popup info on whatever territory and unit your mouse is currently over.<br>"
          + "Press 'u' while mousing over a unit to undo all moves that unit has made (beta).<br>"
          + "To list specific units from a territory in the Territory panel, drag and drop from the territory on the map to the territory panel.<br>";      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
      editorPane.setText(hints);
      final JScrollPane scroll = new JScrollPane(editorPane);
      JOptionPane.showMessageDialog(frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
    })).setMnemonic(KeyEvent.VK_M);
  }

  private String getUnitImageURL(final UnitType unitType, final PlayerID player) {
    final UnitImageFactory unitImageFactory = getUIContext().getUnitImageFactory();
    if (player == null || unitImageFactory == null) {
      return "no image";
    }


    Optional<URL> imageUrl = unitImageFactory.getBaseImageURL(unitType.getName(), player);
    String imageLocation = imageUrl.isPresent() ? imageUrl.get().toString() : "";

    return "<img src=\"" + imageLocation + "\" border=\"0\"/>";
  }

  public String getUnitStatsTable() {
    // html formatted string
    int i = 0;
    final String color1 = "ABABAB";
    final String color2 = "BDBDBD";
    final String color3 = "FEECE2";
    final StringBuilder hints = new StringBuilder();
    hints.append("<html>");
    final GameData data = getData();
    try {
      data.acquireReadLock();
      final Map<PlayerID, Map<UnitType, ResourceCollection>> costs =
          BattleCalculator.getResourceCostsForTUV(data, true);
      final Map<PlayerID, List<UnitType>> playerUnitTypes =
          UnitType.getAllPlayerUnitsWithImages(data, getUIContext(), true);
      for (final Entry<PlayerID, List<UnitType>> entry : playerUnitTypes.entrySet()) {
        final PlayerID player = entry.getKey();
        hints.append("<p><table border=\"1\" bgcolor=\"" + color1 + "\">");
        hints.append("<tr><th style=\"font-size:120%;000000\" bgcolor=\"" + color3 + "\" colspan=\"4\">")
            .append(player == null ? "NULL" : player.getName()).append(" Units</th></tr>");
        hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append("><td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr>");
        for (final UnitType ut : entry.getValue()) {
          i++;
          hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
              .append(">").append("<td>").append(getUnitImageURL(ut, player)).append("</td>").append("<td>")
              .append(ut.getName()).append("</td>").append("<td>").append(costs.get(player).get(ut).toStringForHTML())
              .append("</td>").append("<td>").append(ut.getTooltip(player)).append("</td></tr>");
        }
        i++;
        hints.append("<tr").append(((i & 1) == 0) ? " bgcolor=\"" + color1 + "\"" : " bgcolor=\"" + color2 + "\"")
            .append(">").append("<td>Unit</td><td>Name</td><td>Cost</td><td>Tool Tip</td></tr></table></p><br />");
      }
    } finally {
      data.releaseReadLock();
    }
    hints.append("</html>");
    return hints.toString();
  }

  private void addUnitHelpMenu(final JMenu parentMenu) {
    parentMenu.add(SwingAction.of("Unit help...", e -> {
      final JEditorPane editorPane = new JEditorPane();
      editorPane.setEditable(false);
      editorPane.setContentType("text/html");
      editorPane.setText(getUnitStatsTable());
      editorPane.setCaretPosition(0);
      final JScrollPane scroll = new JScrollPane(editorPane);
      scroll.setBorder(BorderFactory.createEmptyBorder());
      final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
      // not only do we have a start bar, but we also have the message dialog to account for just the scroll bars plus
      // the window sides
      final int availHeight = screenResolution.height - 120;
      final int availWidth = screenResolution.width - 40;
      scroll.setPreferredSize(new Dimension(
          (scroll.getPreferredSize().width > availWidth ? availWidth
              : (scroll.getPreferredSize().height > availHeight
                  ? Math.min(availWidth, scroll.getPreferredSize().width + 22) : scroll.getPreferredSize().width)),
          (scroll.getPreferredSize().height > availHeight ? availHeight
              : (scroll.getPreferredSize().width > availWidth
                  ? Math.min(availHeight, scroll.getPreferredSize().height + 22)
                  : scroll.getPreferredSize().height))));
      final JDialog dialog = new JDialog(frame);
      dialog.setModal(false);
      // needs java 1.6 at least...
      // dialog.setModalityType(ModalityType.MODELESS);
      dialog.setAlwaysOnTop(true);
      dialog.add(scroll, BorderLayout.CENTER);
      final JPanel buttons = new JPanel();
      final JButton button = new JButton(SwingAction.of("OK", event -> {
        dialog.setVisible(false);
        dialog.removeAll();
        dialog.dispose();
      }));
      buttons.add(button);
      dialog.getRootPane().setDefaultButton(button);
      dialog.add(buttons, BorderLayout.SOUTH);
      dialog.pack();
      dialog.setLocationRelativeTo(frame);
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(final WindowEvent e) {
          scroll.getVerticalScrollBar().getModel().setValue(0);
          scroll.getHorizontalScrollBar().getModel().setValue(0);
          button.requestFocus();
        }
      });
      dialog.setVisible(true);
      // dialog.dispose();
    })).setMnemonic(KeyEvent.VK_U);
  }

  @Override
  protected void createGameSpecificMenus(final JMenuBar menuBar) {
    new ViewMenu(menuBar, frame, getData());
    menuBar.add(createGameMenu());
    createExportMenu(menuBar);
  }

  private JMenu createGameMenu() {
    final JMenu menuGame = SwingComponents.newJMenu("Game", SwingComponents.KeyboardCode.G);
    addEditMode(menuGame);
    menuGame.add(frame.getShowGameAction()).setMnemonic(KeyEvent.VK_G);
    menuGame.add(frame.getShowHistoryAction()).setMnemonic(KeyEvent.VK_H);
    menuGame.add(frame.getShowMapOnlyAction()).setMnemonic(KeyEvent.VK_M);
    addShowVerifiedDice(menuGame);
    SoundOptions.addGlobalSoundSwitchMenu(menuGame);
    SoundOptions.addToMenu(menuGame, SoundPath.SoundType.TRIPLEA);
    menuGame.addSeparator();
    addGameOptionsMenu(menuGame);
    addPoliticsMenu(menuGame);
    addNotificationSettings(menuGame);
    addFocusOnCasualties(menuGame);
    addConfirmBattlePhases(menuGame);
    addShowEnemyCasualties(menuGame);
    addShowAIBattles(menuGame);
    addAISleepDuration(menuGame);
    addShowDiceStats(menuGame);
    addRollDice(menuGame);
    addBattleCalculatorMenu(menuGame);
    return menuGame;
  }


  private void createExportMenu(final JMenuBar menuBar) {
    final JMenu menuGame = new JMenu("Export");
    menuGame.setMnemonic(KeyEvent.VK_E);
    menuBar.add(menuGame);
    addExportXML(menuGame);
    addExportStats(menuGame);
    addExportStatsFull(menuGame);
    addExportSetupCharts(menuGame);
    addExportUnitStats(menuGame);
    addSaveScreenshot(menuGame);
  }

  private void addEditMode(final JMenu parentMenu) {
    final JCheckBoxMenuItem editMode = new JCheckBoxMenuItem("Enable Edit Mode");
    editMode.setModel(frame.getEditModeButtonModel());
    parentMenu.add(editMode).setMnemonic(KeyEvent.VK_E);
  }



  private void addShowVerifiedDice(final JMenu parentMenu) {
    final Action showVerifiedDice = SwingAction.of("Show Verified Dice..",
        e -> new VerifiedRandomNumbersDialog(frame.getRootPane()).setVisible(true));
    if (getGame() instanceof ClientGame) {
      parentMenu.add(showVerifiedDice).setMnemonic(KeyEvent.VK_V);
    }
  }

  private void addSaveScreenshot(final JMenu parentMenu) {
    parentMenu.add(frame.getSaveScreenshotAction()).setMnemonic(KeyEvent.VK_E);
  }


  private static void addShowEnemyCasualties(final JMenu parentMenu) {
    final JCheckBoxMenuItem showEnemyCasualties = new JCheckBoxMenuItem("Confirm Enemy Casualties");
    showEnemyCasualties.setMnemonic(KeyEvent.VK_E);
    showEnemyCasualties.setSelected(BattleDisplay.getShowEnemyCasualtyNotification());
    showEnemyCasualties.addActionListener(
        SwingAction.of(e -> BattleDisplay.setShowEnemyCasualtyNotification(showEnemyCasualties.isSelected())));
    parentMenu.add(showEnemyCasualties);
  }

  private static void addFocusOnCasualties(final JMenu parentMenu) {
    final JCheckBoxMenuItem focusOnCasualties = new JCheckBoxMenuItem("Focus On Own Casualties");
    focusOnCasualties.setSelected(BattleDisplay.getFocusOnOwnCasualtiesNotification());
    focusOnCasualties.addActionListener(
        SwingAction.of(e -> BattleDisplay.setFocusOnOwnCasualtiesNotification(focusOnCasualties.isSelected())));
    parentMenu.add(focusOnCasualties);
  }

  private static void addConfirmBattlePhases(final JMenu parentMenu) {
    final JCheckBoxMenuItem confirmPhases = new JCheckBoxMenuItem("Confirm Defensive Rolls");
    confirmPhases.setSelected(BattleDisplay.getConfirmDefensiveRolls());
    confirmPhases.addActionListener(
        SwingAction.of(e -> BattleDisplay.setConfirmDefensiveRolls(confirmPhases.isSelected())));
    parentMenu.add(confirmPhases);
  }


  /**
   * Add a Politics Panel button to the game menu, this panel will show the
   * current political landscape as a reference, no actions on this panel.
   *
   * @param menuGame
   */
  private void addPoliticsMenu(final JMenu menuGame) {
    final AbstractAction politicsAction = SwingAction.of("Show Politics Panel", e -> {
      final PoliticalStateOverview ui = new PoliticalStateOverview(getData(), getUIContext(), false);
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
        showEndOfTurnReport.setSelected(getUIContext().getShowEndOfTurnReport());
        showTriggeredNotifications.setSelected(getUIContext().getShowTriggeredNotifications());
        showTriggerChanceSuccessful.setSelected(getUIContext().getShowTriggerChanceSuccessful());
        showTriggerChanceFailure.setSelected(getUIContext().getShowTriggerChanceFailure());
      }

      @Override
      public void menuDeselected(final MenuEvent e) {}

      @Override
      public void menuCanceled(final MenuEvent e) {}
    });
    showEndOfTurnReport.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getUIContext().setShowEndOfTurnReport(showEndOfTurnReport.isSelected());
      }
    });
    showTriggeredNotifications.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getUIContext().setShowTriggeredNotifications(showTriggeredNotifications.isSelected());
      }
    });
    showTriggerChanceSuccessful.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getUIContext().setShowTriggerChanceSuccessful(showTriggerChanceSuccessful.isSelected());
      }
    });
    showTriggerChanceFailure.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getUIContext().setShowTriggerChanceFailure(showTriggerChanceFailure.isSelected());
      }
    });
    notificationMenu.add(showEndOfTurnReport);
    notificationMenu.add(showTriggeredNotifications);
    notificationMenu.add(showTriggerChanceSuccessful);
    notificationMenu.add(showTriggerChanceFailure);
    parentMenu.add(notificationMenu);
  }

  private void addShowAIBattles(final JMenu parentMenu) {
    final JCheckBoxMenuItem showAIBattlesBox = new JCheckBoxMenuItem("Show Battles Between AIs");
    showAIBattlesBox.setMnemonic(KeyEvent.VK_A);
    showAIBattlesBox.setSelected(getUIContext().getShowBattlesBetweenAIs());
    showAIBattlesBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getUIContext().setShowBattlesBetweenAIs(showAIBattlesBox.isSelected());
      }
    });
    parentMenu.add(showAIBattlesBox);
  }

  private void addShowDiceStats(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Show Dice Stats...", e -> {
      final IRandomStats randomStats =
          (IRandomStats) getGame().getRemoteMessenger().getRemote(IRandomStats.RANDOM_STATS_REMOTE_NAME);
      final RandomStatsDetails stats = randomStats.getRandomStats(getData().getDiceSides());
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
        diceSidesText.setText(String.valueOf(getGame().getData().getDiceSides()));
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
        JOptionPane.showOptionDialog(JOptionPane.getFrameForComponent(TripleAMenu.this), panel, "Roll Dice",
            JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[] {"OK"}, "OK");
        try {
          final int numberOfDice = Integer.parseInt(numberOfText.getText());
          if (numberOfDice > 0) {
            final int diceSides = Integer.parseInt(diceSidesText.getText());
            final int[] dice =
                getGame().getRandomSource().getRandom(diceSides, numberOfDice, "Rolling Dice, no effect on game.");
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

  private void addExportStatsFull(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Export Full Game Stats...", e -> createAndSaveStats(true));
    parentMenu.add(showDiceStats).setMnemonic(KeyEvent.VK_F);
  }

  private void addExportStats(final JMenu parentMenu) {
    final Action showDiceStats = SwingAction.of("Export Short Game Stats...", e -> createAndSaveStats(false));
    parentMenu.add(showDiceStats).setMnemonic(KeyEvent.VK_S);
  }

  private void createAndSaveStats(final boolean showPhaseStats) {
    final ExtendedStats statPanel = new ExtendedStats(getData(), getUIContext());
    final JFileChooser chooser = new JFileChooser();
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    final File rootDir = new File(System.getProperties().getProperty("user.dir"));
    final DateFormat formatDate = new SimpleDateFormat("yyyy_MM_dd");
    int currentRound = 0;
    try {
      getData().acquireReadLock();
      currentRound = getData().getSequence().getRound();
    } finally {
      getData().releaseReadLock();
    }
    String defaultFileName = "stats_" + formatDate.format(new Date()) + "_" + getData().getGameName() + "_round_"
        + currentRound + (showPhaseStats ? "_full" : "_short");
    defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
    defaultFileName = defaultFileName + ".csv";
    chooser.setSelectedFile(new File(rootDir, defaultFileName));
    if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
      return;
    }
    final StringBuilder text = new StringBuilder(1000);
    GameData clone;
    try {
      getData().acquireReadLock();
      clone = GameDataUtils.cloneGameData(getData());
      final IStat[] stats = statPanel.getStats();
      // extended stats covers stuff that doesn't show up in the game stats menu bar, like custom resources or tech
      // tokens or # techs, etc.
      final IStat[] statsExtended = statPanel.getStatsExtended(getData());
      final String[] alliances = statPanel.getAlliances().toArray(new String[statPanel.getAlliances().size()]);
      final PlayerID[] players = statPanel.getPlayers().toArray(new PlayerID[statPanel.getPlayers().size()]);
      // its important here to translate the player objects into our game data
      // the players for the stat panel are only relevant with respect to
      // the game data they belong to
      for (int i = 0; i < players.length; i++) {
        players[i] = clone.getPlayerList().getPlayerID(players[i].getName());
      }
      text.append(defaultFileName + ",");
      text.append("\n");
      text.append("TripleA Engine Version: ,");
      text.append(games.strategy.engine.ClientContext.engineVersion() + ",");
      text.append("\n");
      text.append("Game Name: ,");
      text.append(getData().getGameName() + ",");
      text.append("\n");
      text.append("Game Version: ,");
      text.append(getData().getGameVersion() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Current Round: ,");
      text.append(currentRound + ",");
      text.append("\n");
      text.append("Number of Players: ,");
      text.append(statPanel.getPlayers().size() + ",");
      text.append("\n");
      text.append("Number of Alliances: ,");
      text.append(statPanel.getAlliances().size() + ",");
      text.append("\n");
      text.append("\n");
      text.append("Turn Order: ,");
      text.append("\n");
      final List<PlayerID> playerOrderList = new ArrayList<>();
      playerOrderList.addAll(getData().getPlayerList().getPlayers());
      Collections.sort(playerOrderList, new PlayerOrderComparator(getData()));
      final Set<PlayerID> playerOrderSetNoDuplicates = new LinkedHashSet<>(playerOrderList);
      final Iterator<PlayerID> playerOrderIterator = playerOrderSetNoDuplicates.iterator();
      while (playerOrderIterator.hasNext()) {
        final PlayerID currentPlayerID = playerOrderIterator.next();
        text.append(currentPlayerID.getName()).append(",");
        final Iterator<String> allianceName =
            getData().getAllianceTracker().getAlliancesPlayerIsIn(currentPlayerID).iterator();
        while (allianceName.hasNext()) {
          text.append(allianceName.next()).append(",");
        }
        text.append("\n");
      }
      text.append("\n");
      text.append("Winners: ,");
      final EndRoundDelegate delegateEndRound = (EndRoundDelegate) getData().getDelegateList().getDelegate("endRound");
      if (delegateEndRound != null && delegateEndRound.getWinners() != null) {
        for (final PlayerID p : delegateEndRound.getWinners()) {
          text.append(p.getName()).append(",");
        }
      } else {
        text.append("none yet; game not over,");
      }
      text.append("\n");
      text.append("\n");
      text.append("Resource Chart: ,");
      text.append("\n");
      final Iterator<Resource> resourceIterator = getData().getResourceList().getResources().iterator();
      while (resourceIterator.hasNext()) {
        text.append(resourceIterator.next().getName() + ",");
        text.append("\n");
      }
      // if short, we won't both showing production and unit info
      if (showPhaseStats) {
        text.append("\n");
        text.append("Production Rules: ,");
        text.append("\n");
        text.append("Name,Result,Quantity,Cost,Resource,\n");
        final Iterator<ProductionRule> purchaseOptionsIterator =
            getData().getProductionRuleList().getProductionRules().iterator();
        while (purchaseOptionsIterator.hasNext()) {
          final ProductionRule pr = purchaseOptionsIterator.next();
          String costString = pr.toStringCosts().replaceAll("; ", ",");
          costString = costString.replaceAll(" ", ",");
          text.append(pr.getName()).append(",").append(pr.getResults().keySet().iterator().next().getName()).append(",")
              .append(pr.getResults().getInt(pr.getResults().keySet().iterator().next())).append(",").append(costString)
              .append(",");
          text.append("\n");
        }
        text.append("\n");
        text.append("Unit Types: ,");
        text.append("\n");
        text.append("Name,Listed Abilities\n");
        final Iterator<UnitType> allUnitsIterator = getData().getUnitTypeList().iterator();
        while (allUnitsIterator.hasNext()) {
          final UnitAttachment ua = UnitAttachment.get(allUnitsIterator.next());
          if (ua == null) {
            continue;
          }
          String toModify = ua.allUnitStatsForExporter();
          toModify = toModify.replaceFirst("UnitType called ", "").replaceFirst(" with:", "")
              .replaceAll("games.strategy.engine.data.", "").replaceAll("\n", ";").replaceAll(",", ";");
          toModify = toModify.replaceAll("  ", ",");
          toModify = toModify.replaceAll(", ", ",").replaceAll(" ,", ",");
          text.append(toModify);
          text.append("\n");
        }
      }
      text.append("\n");
      text.append((showPhaseStats ? "Full Stats (includes each phase that had activity),"
          : "Short Stats (only shows first phase with activity per player per round),"));
      text.append("\n");
      text.append("Turn Stats: ,");
      text.append("\n");
      text.append("Round,Player Turn,Phase Name,");
      for (final IStat stat : stats) {
        for (final PlayerID player : players) {
          text.append(stat.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(stat.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      for (final IStat element : statsExtended) {
        for (final PlayerID player : players) {
          text.append(element.getName()).append(" ");
          text.append(player.getName());
          text.append(",");
        }
        for (final String alliance : alliances) {
          text.append(element.getName()).append(" ");
          text.append(alliance);
          text.append(",");
        }
      }
      text.append("\n");
      clone.getHistory().gotoNode(clone.getHistory().getLastNode());
      @SuppressWarnings("unchecked")
      final Enumeration<HistoryNode> nodes = ((DefaultMutableTreeNode) clone.getHistory().getRoot()).preorderEnumeration();
      PlayerID currentPlayer = null;
      int round = 0;
      while (nodes.hasMoreElements()) {
        // we want to export on change of turn
        final HistoryNode element = nodes.nextElement();
        if (element instanceof Round) {
          round++;
        }
        if (!(element instanceof Step)) {
          continue;
        }
        final Step step = (Step) element;
        if (step.getPlayerID() == null || step.getPlayerID().isNull()) {
          continue;
        }
        // this is to stop from having multiple entries for each players turn.
        if (!showPhaseStats) {
          if (step.getPlayerID() == currentPlayer) {
            continue;
          }
        }
        currentPlayer = step.getPlayerID();
        clone.getHistory().gotoNode(element);
        final String playerName = step.getPlayerID() == null ? "" : step.getPlayerID().getName() + ": ";
        String stepName = step.getStepName();
        // copied directly from TripleAPlayer, will probably have to be updated in the future if more delegates are made
        if (stepName.endsWith("Bid")) {
          stepName = "Bid";
        } else if (stepName.endsWith("Tech")) {
          stepName = "Tech";
        } else if (stepName.endsWith("TechActivation")) {
          stepName = "TechActivation";
        } else if (stepName.endsWith("Purchase")) {
          stepName = "Purchase";
        } else if (stepName.endsWith("NonCombatMove")) {
          stepName = "NonCombatMove";
        } else if (stepName.endsWith("Move")) {
          stepName = "Move";
        } else if (stepName.endsWith("Battle")) {
          stepName = "Battle";
        } else if (stepName.endsWith("BidPlace")) {
          stepName = "BidPlace";
        } else if (stepName.endsWith("Place")) {
          stepName = "Place";
        } else if (stepName.endsWith("Politics")) {
          stepName = "Politics";
        } else if (stepName.endsWith("EndTurn")) {
          stepName = "EndTurn";
        } else {
          stepName = "";
        }
        text.append(round).append(",").append(playerName).append(",").append(stepName).append(",");
        for (final IStat stat : stats) {
          for (final PlayerID player : players) {
            text.append(stat.getFormatter().format(stat.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(stat.getFormatter().format(stat.getValue(alliance, clone)));
            text.append(",");
          }
        }
        for (final IStat element2 : statsExtended) {
          for (final PlayerID player : players) {
            text.append(element2.getFormatter().format(element2.getValue(player, clone)));
            text.append(",");
          }
          for (final String alliance : alliances) {
            text.append(element2.getFormatter().format(element2.getValue(alliance, clone)));
            text.append(",");
          }
        }
        text.append("\n");
      }
    } finally {
      getData().releaseReadLock();
    }
    try (final FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
      writer.write(text.toString());
    } catch (final IOException e1) {
      ClientLogger.logQuietly(e1);
    }
  }

  private void addExportUnitStats(final JMenu parentMenu) {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Unit Charts...", e -> {
      final JFileChooser chooser = new JFileChooser();
      chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      final File rootDir = new File(System.getProperties().getProperty("user.dir"));
      String defaultFileName = getData().getGameName() + "_unit_stats";
      defaultFileName = IllegalCharacterRemover.removeIllegalCharacter(defaultFileName);
      defaultFileName = defaultFileName + ".html";
      chooser.setSelectedFile(new File(rootDir, defaultFileName));
      if (chooser.showSaveDialog(frame) != JOptionPane.OK_OPTION) {
        return;
      }
      try (final FileWriter writer = new FileWriter(chooser.getSelectedFile())) {
        writer.write(getUnitStatsTable().replaceAll("<p>", "<p>\r\n").replaceAll("</p>", "</p>\r\n")
            .replaceAll("</tr>", "</tr>\r\n").replaceAll(LocalizeHTML.PATTERN_HTML_IMG_TAG, ""));
      } catch (final IOException e1) {
        ClientLogger.logQuietly(e1);
      }

    }));
    menuFileExport.setMnemonic(KeyEvent.VK_U);
    parentMenu.add(menuFileExport);
  }


  private void addExportSetupCharts(final JMenu parentMenu) {
    final JMenuItem menuFileExport = new JMenuItem(SwingAction.of("Export Setup Charts...", e -> {
      final JFrame frame = new JFrame("Export Setup Files");
      frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      final GameData data = this.frame.getGame().getData();
      GameData clonedGameData;
      data.acquireReadLock();
      try {
        clonedGameData = GameDataUtils.cloneGameData(data);
      } finally {
        data.releaseReadLock();
      }
      final JComponent newContentPane = new SetupFrame(clonedGameData);
      // content panes must be opaque
      newContentPane.setOpaque(true);
      frame.setContentPane(newContentPane);
      // Display the window.
      frame.pack();
      frame.setLocationRelativeTo(frame);
      frame.setVisible(true);
      this.frame.getUIContext().addShutdownWindow(frame);

    }));
    menuFileExport.setMnemonic(KeyEvent.VK_C);
    parentMenu.add(menuFileExport);
  }


}
