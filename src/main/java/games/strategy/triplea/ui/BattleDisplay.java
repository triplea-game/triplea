package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.system.SystemProperties;
import games.strategy.net.GUID;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitOwner;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.SwingAction;
import games.strategy.ui.Util;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

/**
 * Displays a running battle.
 */
public class BattleDisplay extends JPanel {
  private static final long serialVersionUID = -7939993104972562765L;
  private static final String DICE_KEY = "D";
  private static final String CASUALTIES_KEY = "C";
  private static final String MESSAGE_KEY = "M";
  private final GUID battleId;
  private final PlayerID defender;
  private final PlayerID attacker;
  private final Territory location;
  private final GameData gameData;
  private final JButton actionButton = new JButton("");
  private final BattleModel defenderModel;
  private final BattleModel attackerModel;
  private BattleStepsPanel steps;
  private DicePanel dicePanel;
  private final CasualtyNotificationPanel casualties;
  private JPanel actionPanel;
  private final CardLayout actionLayout = new CardLayout();
  private final JPanel messagePanel = new JPanel();
  private final MapPanel mapPanel;
  private final JPanel casualtiesInstantPanelDefender = new JPanel();
  private final JPanel casualtiesInstantPanelAttacker = new JPanel();
  private final JLabel labelNoneAttacker = new JLabel("None");
  private final JLabel labelNoneDefender = new JLabel("None");
  private final IUIContext uiContext;
  private final JLabel messageLabel = new JLabel();
  private final Action nullAction = SwingAction.of(" ", e -> {
  });

  BattleDisplay(final GameData data, final Territory territory, final PlayerID attacker, final PlayerID defender,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits, final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie, final GUID battleId,
      final MapPanel mapPanel, final boolean isAmphibious, final BattleType battleType,
      final Collection<Unit> amphibiousLandAttackers) {
    this.battleId = battleId;
    this.defender = defender;
    this.attacker = attacker;
    location = territory;
    this.mapPanel = mapPanel;
    gameData = data;
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(territory);
    defenderModel = new BattleModel(defendingUnits, false, battleType, gameData, location, territoryEffects,
        isAmphibious, Collections.emptySet(), this.mapPanel.getUIContext());
    attackerModel = new BattleModel(attackingUnits, true, battleType, gameData, location, territoryEffects,
        isAmphibious, amphibiousLandAttackers, this.mapPanel.getUIContext());
    defenderModel.setEnemyBattleModel(attackerModel);
    attackerModel.setEnemyBattleModel(defenderModel);
    defenderModel.refresh();
    attackerModel.refresh();
    uiContext = mapPanel.getUIContext();
    casualties = new CasualtyNotificationPanel(data, this.mapPanel.getUIContext());
    if (killedUnits != null && attackingWaitingToDie != null && defendingWaitingToDie != null) {
      final Collection<Unit> attackerUnitsKilled = Match.getMatches(killedUnits, Matches.unitIsOwnedBy(attacker));
      attackerUnitsKilled.addAll(attackingWaitingToDie);
      if (!attackerUnitsKilled.isEmpty()) {
        updateKilledUnits(attackerUnitsKilled, attacker);
      }
      final Collection<Unit> defenderUnitsKilled = Match.getMatches(killedUnits, Matches.unitIsOwnedBy(defender));
      defenderUnitsKilled.addAll(defendingWaitingToDie);
      if (!defenderUnitsKilled.isEmpty()) {
        updateKilledUnits(defenderUnitsKilled, defender);
      }
    }
    initLayout();
  }

  void cleanUp() {
    actionButton.setAction(nullAction);
    steps.deactivate();
    mapPanel.getUIContext().removeActive(steps);
    steps = null;
  }

  void takeFocus() {
    // we want a component on this frame to take focus
    // so that pressing space will work (since it requires in focused
    // window). Only seems to be an issue on windows
    actionButton.requestFocus();
  }

  public Territory getBattleLocation() {
    return location;
  }

  public GUID getBattleID() {
    return battleId;
  }

  public void bombingResults(final List<Die> dice, final int cost) {
    dicePanel.setDiceRollForBombing(dice, cost);
    actionLayout.show(actionPanel, DICE_KEY);
  }

  public static boolean getShowEnemyCasualtyNotification() {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    return prefs.getBoolean(Constants.SHOW_ENEMY_CASUALTIES_USER_PREF, true);
  }

  public static void setShowEnemyCasualtyNotification(final boolean showEnemyCasualtyNotification) {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    prefs.putBoolean(Constants.SHOW_ENEMY_CASUALTIES_USER_PREF, showEnemyCasualtyNotification);
  }

  public static boolean getFocusOnOwnCasualtiesNotification() {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    return prefs.getBoolean(Constants.FOCUS_ON_OWN_CASUALTIES_USER_PREF, false);
  }

  public static void setFocusOnOwnCasualtiesNotification(final boolean focusOnOwnCasualtiesNotification) {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    prefs.putBoolean(Constants.FOCUS_ON_OWN_CASUALTIES_USER_PREF, focusOnOwnCasualtiesNotification);
  }

  public static void setConfirmDefensiveRolls(final boolean confirmDefensiveRolls) {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    prefs.putBoolean(Constants.CONFIRM_DEFENSIVE_ROLLS, confirmDefensiveRolls);
  }

  public static boolean getConfirmDefensiveRolls() {
    final Preferences prefs = Preferences.userNodeForPackage(BattleDisplay.class);
    return prefs.getBoolean(Constants.CONFIRM_DEFENSIVE_ROLLS, false);
  }


  /**
   * updates the panel content according to killed units for the player.
   *
   * @param killedUnits
   *        list of units killed
   * @param playerId
   *        player kills belongs to
   */
  private Collection<Unit> updateKilledUnits(final Collection<Unit> killedUnits, final PlayerID playerId) {
    final JPanel lCausalityPanel;
    if (playerId.equals(defender)) {
      lCausalityPanel = casualtiesInstantPanelDefender;
    } else {
      lCausalityPanel = casualtiesInstantPanelAttacker;
    }
    Map<Unit, Collection<Unit>> dependentsMap;
    gameData.acquireReadLock();
    try {
      dependentsMap = BattleCalculator.getDependents(killedUnits);
    } finally {
      gameData.releaseReadLock();
    }
    final Collection<Unit> dependentUnitsReturned = new ArrayList<>();
    final Iterator<Collection<Unit>> dependentUnitsCollections = dependentsMap.values().iterator();
    while (dependentUnitsCollections.hasNext()) {
      final Collection<Unit> dependentCollection = dependentUnitsCollections.next();
      dependentUnitsReturned.addAll(dependentCollection);
    }
    for (final UnitCategory category : UnitSeperator.categorize(killedUnits, dependentsMap, false, false)) {
      final JPanel panel = new JPanel();
      JLabel unit = uiContext.createUnitImageJLabel(category.getType(), category.getOwner(), gameData);
      panel.add(unit);
      panel.add(new JLabel("x " + category.getUnits().size()));
      for (final UnitOwner owner : category.getDependents()) {
        unit = uiContext.createUnitImageJLabel(owner.getType(), owner.getOwner(), gameData);
        panel.add(unit);
        // TODO this size is of the transport collection size, not the transportED collection size.
        panel.add(new JLabel("x " + category.getUnits().size()));
      }
      lCausalityPanel.add(panel);
    }
    return dependentUnitsReturned;
  }

  void casualtyNotification(final String step, final DiceRoll dice, final PlayerID player,
      final Collection<Unit> killed, final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents) {
    setStep(step);
    casualties.setNotification(dice, killed, damaged, dependents);
    actionLayout.show(actionPanel, CASUALTIES_KEY);
    killed.addAll(updateKilledUnits(killed, player));
    if (player.equals(defender)) {
      defenderModel.removeCasualties(killed);
    } else {
      attackerModel.removeCasualties(killed);
    }
  }

  void deadUnitNotification(final PlayerID player, final Collection<Unit> killed,
      final Map<Unit, Collection<Unit>> dependents) {
    casualties.setNotificationShort(killed, dependents);
    actionLayout.show(actionPanel, CASUALTIES_KEY);
    killed.addAll(updateKilledUnits(killed, player));
    if (player.equals(defender)) {
      defenderModel.removeCasualties(killed);
    } else {
      attackerModel.removeCasualties(killed);
    }
  }

  void changedUnitsNotification(final PlayerID player, final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits, final Map<Unit, Collection<Unit>> dependents) {
    if (player.equals(defender)) {
      if (removedUnits != null) {
        defenderModel.removeCasualties(removedUnits);
      }
      if (addedUnits != null) {
        defenderModel.addUnits(addedUnits);
      }
    } else {
      if (removedUnits != null) {
        attackerModel.removeCasualties(removedUnits);
      }
      if (addedUnits != null) {
        attackerModel.addUnits(addedUnits);
      }
    }
  }

  protected void waitForConfirmation(final String message) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("This cannot be in dispatch thread");
    }

    final CountDownLatch continueLatch = new CountDownLatch(1);
    final AbstractAction buttonAction = SwingAction.of(message, e -> continueLatch.countDown());
    SwingUtilities.invokeLater(() -> actionButton.setAction(buttonAction));
    mapPanel.getUIContext().addShutdownLatch(continueLatch);

    // Set a auto-wait expiration if the option is set.
    if (!getConfirmDefensiveRolls()) {
      final int maxWaitTime = 1500;
      final Timer t = new Timer();
      t.schedule(new TimerTask() {
        @Override
        public void run() {
          continueLatch.countDown();
          if (continueLatch.getCount() > 0) {
            SwingUtilities.invokeLater(() -> actionButton.setAction(nullAction));
          }
        }
      }, maxWaitTime);
    }

    try {
      // wait for the button to be pressed.
      continueLatch.await();
    } catch (final InterruptedException ie) {
      Thread.currentThread().interrupt();
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(continueLatch);
    }
    SwingUtilities.invokeLater(() -> actionButton.setAction(nullAction));
  }


  void endBattle(final String message, final Window enclosingFrame) {
    steps.walkToLastStep();
    final Action close = SwingAction.of(message + " : (Press Space to Close)", e -> enclosingFrame.setVisible(false));
    SwingUtilities.invokeLater(() -> actionButton.setAction(close));
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    defenderModel.notifyRetreat(retreating);
    attackerModel.notifyRetreat(retreating);
  }

  Territory getRetreat(final String message, final Collection<Territory> possible, final boolean submerge) {
    if (!submerge || possible.size() > 1) {
      return getRetreatInternal(message, possible);
    } else {
      return getSubmerge(message);
    }
  }

  private Territory getSubmerge(final String message) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final Territory[] retreatTo = new Territory[1];
    final CountDownLatch latch = new CountDownLatch(1);
    final Action action = SwingAction.of("Submerge Subs?", e -> {
      final String ok = "Submerge";
      final String cancel = "Remain";
      final String wait = "Ask Me Later";
      final String[] options = {ok, cancel, wait};
      final int choice = JOptionPane.showOptionDialog(BattleDisplay.this, message, "Submerge Subs?",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, cancel);
      // dialog dismissed
      if (choice == -1) {
        return;
      }
      // wait
      if (choice == 2) {
        return;
      }
      // remain
      if (choice == 1) {
        latch.countDown();
        return;
      }
      // submerge
      retreatTo[0] = location;
      latch.countDown();
    });
    SwingUtilities.invokeLater(() -> actionButton.setAction(action));
    SwingUtilities.invokeLater(() -> action.actionPerformed(null));
    mapPanel.getUIContext().addShutdownLatch(latch);
    try {
      latch.await();
    } catch (final InterruptedException e1) {
      Thread.currentThread().interrupt();
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(latch);
    }
    SwingUtilities.invokeLater(() -> actionButton.setAction(nullAction));
    return retreatTo[0];
  }

  private Territory getRetreatInternal(final String message, final Collection<Territory> possible) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Should not be called from dispatch thread");
    }
    final Territory[] retreatTo = new Territory[1];
    final CountDownLatch latch = new CountDownLatch(1);
    final Action action = SwingAction.of("Retreat?", e -> {
      final String yes = possible.size() == 1 ? "Retreat to " + possible.iterator().next().getName() : "Retreat";
      final String no = "Remain";
      final String cancel = "Ask Me Later";
      final String[] options = {yes, no, cancel};
      final int choice = JOptionPane.showOptionDialog(BattleDisplay.this, message, "Retreat?",
          JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, no);
      // dialog dismissed
      if (choice == -1) {
        return;
      }
      // wait
      if (choice == JOptionPane.CANCEL_OPTION) {
        return;
      }
      // remain
      if (choice == JOptionPane.NO_OPTION) {
        latch.countDown();
        return;
      }
      // if you have eliminated the impossible, whatever remains, no matter
      // how improbable, must be the truth
      // retreat
      if (possible.size() == 1) {
        retreatTo[0] = possible.iterator().next();
        latch.countDown();
      } else {
        final RetreatComponent comp = new RetreatComponent(possible);
        final int option = JOptionPane.showConfirmDialog(BattleDisplay.this, comp, message,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        if (option == JOptionPane.OK_OPTION) {
          if (comp.getSelection() != null) {
            retreatTo[0] = comp.getSelection();
            latch.countDown();
          }
        }
      }
    });
    SwingUtilities.invokeLater(() -> actionButton.setAction(action));
    SwingUtilities.invokeLater(() -> action.actionPerformed(null));
    mapPanel.getUIContext().addShutdownLatch(latch);
    try {
      latch.await();
    } catch (final InterruptedException e1) {
      e1.printStackTrace();
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(latch);
    }
    SwingUtilities.invokeLater(() -> actionButton.setAction(nullAction));
    return retreatTo[0];
  }

  private class RetreatComponent extends JPanel {
    private static final long serialVersionUID = 3855054934860687832L;
    private final JList<Territory> list;
    private final JLabel retreatTerritory = new JLabel("");

    RetreatComponent(final Collection<Territory> possible) {
      this.setLayout(new BorderLayout());
      final JLabel label = new JLabel("Retreat to...");
      label.setBorder(new EmptyBorder(0, 0, 10, 0));
      this.add(label, BorderLayout.NORTH);
      final JPanel imagePanel = new JPanel();
      imagePanel.setLayout(new FlowLayout(FlowLayout.CENTER));
      imagePanel.add(retreatTerritory);
      imagePanel.setBorder(new EmptyBorder(10, 10, 10, 0));
      this.add(imagePanel, BorderLayout.EAST);
      final Vector<Territory> listElements = new Vector<>(possible);
      list = new JList<>(listElements);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (listElements.size() >= 1) {
        list.setSelectedIndex(0);
      }
      final JScrollPane scroll = new JScrollPane(list);
      this.add(scroll, BorderLayout.CENTER);
      scroll.setBorder(new EmptyBorder(10, 0, 10, 0));
      updateImage();
      list.addListSelectionListener(e -> updateImage());
    }

    private void updateImage() {
      final int width = 250;
      final int height = 250;
      final Image img = mapPanel.getTerritoryImage(list.getSelectedValue(), location);
      final Image finalImage = Util.createImage(width, height, true);
      final Graphics g = finalImage.getGraphics();
      g.drawImage(img, 0, 0, width, height, this);
      g.dispose();
      retreatTerritory.setIcon(new ImageIcon(finalImage));
    }

    public Territory getSelection() {
      return list.getSelectedValue();
    }
  }

  CasualtyDetails getCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents,
      final int count, final String message, final DiceRoll dice, final PlayerID hit,
      final CasualtyList defaultCasualties, final boolean allowMultipleHitsPerUnit) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("This method should not be run in the event dispatch thread");
    }
    final AtomicReference<CasualtyDetails> casualtyDetails = new AtomicReference<>();
    final CountDownLatch continueLatch = new CountDownLatch(1);
    SwingUtilities.invokeLater(() -> {
      final boolean isEditMode = (dice == null);
      if (!isEditMode) {
        actionLayout.show(actionPanel, DICE_KEY);
        dicePanel.setDiceRoll(dice);
      }
      final boolean plural = isEditMode || (count > 1);
      final String countStr = isEditMode ? "" : "" + count;
      final String btnText =
          hit.getName() + ", press space to select " + countStr + (plural ? " casualties" : " casualty");
      actionButton.setAction(new AbstractAction(btnText) {
        private static final long serialVersionUID = -2156028313292233568L;
        private UnitChooser chooser;
        private JScrollPane chooserScrollPane;

        @Override
        public void actionPerformed(final ActionEvent e) {
          final String messageText = message + " " + btnText + ".";
          if (chooser == null || chooserScrollPane == null) {
            chooser = new UnitChooser(selectFrom, defaultCasualties, dependents, gameData, allowMultipleHitsPerUnit,
                mapPanel.getUIContext());
            chooser.setTitle(messageText);
            if (isEditMode) {
              chooser.setMax(selectFrom.size());
            } else {
              chooser.setMax(count);
            }
            chooserScrollPane = new JScrollPane(chooser);
            final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
            int availHeight = screenResolution.height - 80;
            final int availWidth = screenResolution.width - 30;
            availHeight -= 50;
            chooserScrollPane.setPreferredSize(new Dimension(
                (chooserScrollPane.getPreferredSize().width > availWidth ? availWidth
                    : (chooserScrollPane.getPreferredSize().height > availHeight
                        ? chooserScrollPane.getPreferredSize().width + 22
                        : chooserScrollPane.getPreferredSize().width)),
                (chooserScrollPane.getPreferredSize().height > availHeight ? availHeight
                    : chooserScrollPane.getPreferredSize().height)));
            chooserScrollPane.setBorder(new LineBorder(chooserScrollPane.getBackground()));
          }
          final String[] options = {"Ok", "Cancel"};
          final String focus = BattleDisplay.getFocusOnOwnCasualtiesNotification() ? options[0] : null;
          final int option = JOptionPane.showOptionDialog(BattleDisplay.this, chooserScrollPane,
              hit.getName() + " select casualties", JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
              focus);
          if (option != 0) {
            return;
          }
          final List<Unit> killed = chooser.getSelected(false);
          final List<Unit> damaged = chooser.getSelectedDamagedMultipleHitPointUnits();
          if (!isEditMode && (killed.size() + damaged.size() != count)) {
            JOptionPane.showMessageDialog(BattleDisplay.this, "Wrong number of casualties selected",
                hit.getName() + " select casualties", JOptionPane.ERROR_MESSAGE);
          } else {
            final CasualtyDetails response = new CasualtyDetails(killed, damaged, false);
            casualtyDetails.set(response);
            dicePanel.clear();
            actionButton.setEnabled(false);
            actionButton.setAction(nullAction);
            continueLatch.countDown();
          }
        }
      });
    });
    mapPanel.getUIContext().addShutdownLatch(continueLatch);
    try {
      continueLatch.await();
    } catch (final InterruptedException ex) {
      ClientLogger.logQuietly(ex);
    } finally {
      mapPanel.getUIContext().removeShutdownLatch(continueLatch);
    }
    return casualtyDetails.get();
  }

  private void initLayout() {
    final JPanel attackerUnits = new JPanel();
    attackerUnits.setLayout(new BoxLayout(attackerUnits, BoxLayout.Y_AXIS));
    attackerUnits.add(getPlayerComponent(attacker));
    attackerUnits.add(Box.createGlue());
    final JTable attackerTable = new BattleTable(attackerModel);
    attackerUnits.add(attackerTable);
    attackerUnits.add(attackerTable.getTableHeader());
    final JPanel defenderUnits = new JPanel();
    defenderUnits.setLayout(new BoxLayout(defenderUnits, BoxLayout.Y_AXIS));
    defenderUnits.add(getPlayerComponent(defender));
    defenderUnits.add(Box.createGlue());
    final JTable defenderTable = new BattleTable(defenderModel);
    defenderUnits.add(defenderTable);
    defenderUnits.add(defenderTable.getTableHeader());
    final JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.X_AXIS));
    north.add(attackerUnits);
    north.add(getTerritoryComponent());
    north.add(defenderUnits);
    messagePanel.setLayout(new BorderLayout());
    messagePanel.add(messageLabel, BorderLayout.CENTER);
    steps = new BattleStepsPanel();
    mapPanel.getUIContext().addActive(steps);
    steps.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    dicePanel = new DicePanel(mapPanel.getUIContext(), gameData);
    actionPanel = new JPanel();
    actionPanel.setLayout(actionLayout);
    actionPanel.add(dicePanel, DICE_KEY);
    actionPanel.add(casualties, CASUALTIES_KEY);
    actionPanel.add(messagePanel, MESSAGE_KEY);
    final JPanel diceAndSteps = new JPanel();
    diceAndSteps.setLayout(new BorderLayout());
    diceAndSteps.add(steps, BorderLayout.WEST);
    diceAndSteps.add(actionPanel, BorderLayout.CENTER);
    casualtiesInstantPanelAttacker.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
    casualtiesInstantPanelAttacker.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    casualtiesInstantPanelAttacker.add(labelNoneAttacker);
    casualtiesInstantPanelDefender.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
    casualtiesInstantPanelDefender.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    casualtiesInstantPanelDefender.add(labelNoneDefender);
    final JPanel lInstantCasualtiesPanel = new JPanel();
    lInstantCasualtiesPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
    lInstantCasualtiesPanel.setLayout(new GridBagLayout());
    final JLabel lCausalities = new JLabel("Casualties", SwingConstants.CENTER);
    lCausalities.setFont(getPlayerComponent(attacker).getFont().deriveFont(Font.BOLD, 14));
    lInstantCasualtiesPanel.add(lCausalities, new GridBagConstraints(0, 0, 2, 1, 1.0d, 1.0d, GridBagConstraints.CENTER,
        GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    lInstantCasualtiesPanel.add(casualtiesInstantPanelAttacker, new GridBagConstraints(0, 2, 1, 1, 1.0d, 1.0d,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    lInstantCasualtiesPanel.add(casualtiesInstantPanelDefender, new GridBagConstraints(1, 2, 1, 1, 1.0d, 1.0d,
        GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    diceAndSteps.add(lInstantCasualtiesPanel, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(north, BorderLayout.NORTH);
    add(diceAndSteps, BorderLayout.CENTER);
    add(actionButton, BorderLayout.SOUTH);
    actionButton.setEnabled(false);
    if (!SystemProperties.isMac()) {
      actionButton.setBackground(Color.lightGray.darker());
      actionButton.setForeground(Color.white);
    }
    setDefaultWidths(defenderTable);
    setDefaultWidths(attackerTable);
    final Action continueAction = SwingAction.of(e -> {
      final Action a = actionButton.getAction();
      if (a != null) {
        a.actionPerformed(null);
      }
    });
    // press space to continue
    final String key = "battle.display.press.space.to.continue";
    getActionMap().put(key, continueAction);
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), key);
  }

  /**
   * Shorten columns with no units.
   */
  private static void setDefaultWidths(final JTable table) {
    for (int column = 0; column < table.getColumnCount(); column++) {
      boolean hasData = false;
      for (int row = 0; row < table.getRowCount(); row++) {
        hasData |= (table.getValueAt(row, column) != TableData.NULL);
      }
      if (!hasData) {
        table.getColumnModel().getColumn(column).setPreferredWidth(8);
      }
    }
  }

  public void setStep(final String step) {
    steps.setStep(step);
  }

  void battleInfo(final DiceRoll message, final String step) {
    setStep(step);
    dicePanel.setDiceRoll(message);
    actionLayout.show(actionPanel, DICE_KEY);
  }

  void battleInfo(final String message, final String step) {
    messageLabel.setText(message);
    setStep(step);
    actionLayout.show(actionPanel, MESSAGE_KEY);
  }

  public void listBattle(final List<String> steps) {
    this.steps.listBattle(steps);
  }

  private static JComponent getPlayerComponent(final PlayerID id) {
    final JLabel player = new JLabel(id.getName());
    player.setBorder(new javax.swing.border.EmptyBorder(5, 5, 5, 5));
    player.setFont(player.getFont().deriveFont((float) 14));
    return player;
  }

  private static final int MY_WIDTH = 100;
  private static final int MY_HEIGHT = 100;

  private JComponent getTerritoryComponent() {
    final Image finalImage = Util.createImage(MY_WIDTH, MY_HEIGHT, true);
    final Image territory = mapPanel.getTerritoryImage(location);
    final Graphics g = finalImage.getGraphics();
    g.drawImage(territory, 0, 0, MY_WIDTH, MY_HEIGHT, this);
    g.dispose();
    return new JLabel(new ImageIcon(finalImage));
  }
}


class BattleTable extends JTable {
  private static final long serialVersionUID = 6737857639382012817L;

  BattleTable(final BattleModel model) {
    super(model);
    setDefaultRenderer(Object.class, new Renderer());
    setRowHeight(UnitImageFactory.DEFAULT_UNIT_ICON_SIZE + 5);
    setBackground(new JButton().getBackground());
    setShowHorizontalLines(false);
    getTableHeader().setReorderingAllowed(false);
    // getTableHeader().setResizingAllowed(false);
  }
}


class BattleModel extends DefaultTableModel {
  private static final long serialVersionUID = 6913324191512043963L;
  private final IUIContext m_uiContext;
  private final GameData m_data;
  // is the player the aggressor?
  private final boolean m_attack;
  private final Collection<Unit> m_units;
  private final Territory m_location;
  private final BattleType m_battleType;
  private final Collection<TerritoryEffect> m_territoryEffects;
  private final boolean m_isAmphibious;
  private final Collection<Unit> m_amphibiousLandAttackers;
  private BattleModel m_enemyBattleModel = null;

  private static String[] varDiceArray(final GameData data) {
    // TODO Soft set the maximum bonus to-hit plus 1 for 0 based count(+2 total currently)
    final String[] diceColumns = new String[data.getDiceSides() + 1];
    {
      for (int i = 0; i < diceColumns.length; i++) {
        if (i == 0) {
          diceColumns[i] = " ";
        } else {
          diceColumns[i] = Integer.toString(i);
        }
      }
    }
    return diceColumns;
  }

  BattleModel(final Collection<Unit> units, final boolean attack, final BattleType battleType,
      final GameData data, final Territory battleLocation, final Collection<TerritoryEffect> territoryEffects,
      final boolean isAmphibious, final Collection<Unit> amphibiousLandAttackers, final IUIContext uiContext) {
    super(new Object[0][0], varDiceArray(data));
    m_uiContext = uiContext;
    m_data = data;
    m_attack = attack;
    // were going to modify the units
    m_units = new ArrayList<>(units);
    m_location = battleLocation;
    m_battleType = battleType;
    m_territoryEffects = territoryEffects;
    m_isAmphibious = isAmphibious;
    m_amphibiousLandAttackers = amphibiousLandAttackers;
  }

  public void setEnemyBattleModel(final BattleModel enemyBattleModel) {
    m_enemyBattleModel = enemyBattleModel;
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    m_units.removeAll(retreating);
    refresh();
  }

  public void removeCasualties(final Collection<Unit> killed) {
    m_units.removeAll(killed);
    refresh();
  }

  public void addUnits(final Collection<Unit> units) {
    m_units.addAll(units);
    refresh();
  }

  Collection<Unit> getUnits() {
    return m_units;
  }

  /**
   * refresh the model from m_units.
   */
  public void refresh() {
    // TODO Soft set the maximum bonus to-hit plus 1 for 0 based count(+2 total currently)
    // Soft code the # of columns

    final List<List<TableData>> columns = new ArrayList<>(m_data.getDiceSides() + 1);
    for (int i = 0; i <= m_data.getDiceSides(); i++) {
      columns.add(i, new ArrayList<>());
    }
    final List<Unit> units = new ArrayList<>(m_units);
    DiceRoll.sortByStrength(units, !m_attack);
    final Map<Unit, Tuple<Integer, Integer>> unitPowerAndRollsMap;
    m_data.acquireReadLock();
    try {
      if (m_battleType.isAirPreBattleOrPreRaid()) {
        unitPowerAndRollsMap = null;
      } else {
        unitPowerAndRollsMap = DiceRoll.getUnitPowerAndRollsForNormalBattles(units,
            new ArrayList<>(m_enemyBattleModel.getUnits()), !m_attack, false, m_data, m_location,
            m_territoryEffects, m_isAmphibious, m_amphibiousLandAttackers);
      }
    } finally {
      m_data.releaseReadLock();
    }
    final int diceSides = m_data.getDiceSides();
    final Collection<UnitCategory> unitCategories = UnitSeperator.categorize(units, null, false, false, false);
    for (final UnitCategory category : unitCategories) {
      int strength;
      final UnitAttachment attachment = UnitAttachment.get(category.getType());
      final int[] shift = new int[m_data.getDiceSides() + 1];
      for (final Unit current : category.getUnits()) {
        if (m_battleType.isAirPreBattleOrPreRaid()) {
          if (m_attack) {
            strength = attachment.getAirAttack(category.getOwner());
          } else {
            strength = attachment.getAirDefense(category.getOwner());
          }
        } else {
          // normal battle
          strength = unitPowerAndRollsMap.get(current).getFirst();
        }
        strength = Math.min(Math.max(strength, 0), diceSides);
        shift[strength]++;
      }
      for (int i = 0; i <= m_data.getDiceSides(); i++) {
        if (shift[i] > 0) {
          columns.get(i).add(new TableData(category.getOwner(), shift[i], category.getType(), m_data,
              category.hasDamageOrBombingUnitDamage(), category.getDisabled(), m_uiContext));
        }
      }
      // TODO Kev determine if we need to identify if the unit is hit/disabled
    }
    // find the number of rows
    // this will be the size of the largest column
    int rowCount = 1;
    for (final List<TableData> column : columns) {
      rowCount = Math.max(rowCount, column.size());
    }
    setNumRows(rowCount);
    for (int row = 0; row < rowCount; row++) {
      for (int column = 0; column < columns.size(); column++) {
        // if the column has that many items, add to the table, else add null
        if (columns.get(column).size() > row) {
          setValueAt(columns.get(column).get(row), row, column);
        } else {
          setValueAt(TableData.NULL, row, column);
        }
      }
    }
  }

  @Override
  public boolean isCellEditable(final int row, final int column) {
    return false;
  }
}


class Renderer implements TableCellRenderer {
  JLabel m_stamp = new JLabel();

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
      final boolean hasFocus, final int row, final int column) {
    ((TableData) value).updateStamp(m_stamp);
    return m_stamp;
  }
}


class TableData {
  static final TableData NULL = new TableData();
  private int m_count;
  private Optional<ImageIcon> m_icon;

  private TableData() {}

  TableData(final PlayerID player, final int count, final UnitType type, final GameData data, final boolean damaged,
      final boolean disabled, final IUIContext uiContext) {
    m_count = count;
    m_icon = uiContext.getUnitImageFactory().getIcon(type, player, data, damaged, disabled);
  }

  public void updateStamp(final JLabel stamp) {
    if (m_count == 0) {
      stamp.setText("");
      stamp.setIcon(null);
    } else {
      stamp.setText("x" + m_count);
      if (m_icon.isPresent()) {
        stamp.setIcon(m_icon.get());
      }
    }
  }
}


class CasualtyNotificationPanel extends JPanel {
  private static final long serialVersionUID = -8254027929090027450L;
  private final DicePanel dice;
  private final JPanel killed = new JPanel();
  private final JPanel damaged = new JPanel();
  private final GameData data;
  private final IUIContext uiContext;

  public CasualtyNotificationPanel(final GameData data, final IUIContext uiContext) {
    this.data = data;
    this.uiContext = uiContext;
    dice = new DicePanel(uiContext, data);
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    add(dice);
    add(killed);
    add(damaged);
  }

  protected void setNotification(final DiceRoll dice, final Collection<Unit> killed,
      final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents) {
    final boolean isEditMode = (dice == null);
    if (!isEditMode) {
      this.dice.setDiceRoll(dice);
    }
    this.killed.removeAll();
    this.damaged.removeAll();
    if (!killed.isEmpty()) {
      this.killed.add(new JLabel("Killed"));
    }
    final Iterator<UnitCategory> killedIter = UnitSeperator.categorize(killed, dependents, false, false).iterator();
    categorizeUnits(killedIter, false, false);
    damaged.removeAll(killed);
    if (!damaged.isEmpty()) {
      this.damaged.add(new JLabel("Damaged"));
    }
    final Iterator<UnitCategory> damagedIter = UnitSeperator.categorize(damaged, dependents, false, false).iterator();
    categorizeUnits(damagedIter, true, true);
    invalidate();
    validate();
  }

  protected void setNotificationShort(final Collection<Unit> killed, final Map<Unit, Collection<Unit>> dependents) {
    this.killed.removeAll();
    if (!killed.isEmpty()) {
      this.killed.add(new JLabel("Killed"));
    }
    final Iterator<UnitCategory> killedIter = UnitSeperator.categorize(killed, dependents, false, false).iterator();
    categorizeUnits(killedIter, false, false);
    invalidate();
    validate();
  }

  private void categorizeUnits(final Iterator<UnitCategory> categoryIter, final boolean damaged,
      final boolean disabled) {
    while (categoryIter.hasNext()) {
      final UnitCategory category = categoryIter.next();
      final JPanel panel = new JPanel();
      // TODO Kev determine if we need to identify if the unit is hit/disabled
      final Optional<ImageIcon> unitImage =
          uiContext.getUnitImageFactory().getIcon(category.getType(), category.getOwner(), data,
              damaged && category.hasDamageOrBombingUnitDamage(), disabled && category.getDisabled());
      final JLabel unit = unitImage.isPresent() ? new JLabel(unitImage.get()) : new JLabel();
      panel.add(unit);
      for (final UnitOwner owner : category.getDependents()) {
        unit.add(uiContext.createUnitImageJLabel(owner.getType(), owner.getOwner(), data));
      }
      panel.add(new JLabel("x " + category.getUnits().size()));
      if (damaged) {
        this.damaged.add(panel);
      } else {
        killed.add(panel);
      }
    }
  }
}
