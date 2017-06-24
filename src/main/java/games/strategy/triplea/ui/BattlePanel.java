package games.strategy.triplea.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.ErrorConsole;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.gamePlayer.IGamePlayer;
import games.strategy.net.GUID;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.dataObjects.FightBattleDetails;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.ui.Util;
import games.strategy.ui.Util.Task;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.ThreadUtil;

/**
 * UI for fighting battles.
 */
public class BattlePanel extends ActionPanel {
  private static final long serialVersionUID = 5304208569738042592L;
  private final JLabel actionLabel = new JLabel();
  private FightBattleDetails fightBattleMessage;
  private volatile BattleDisplay battleDisplay;
  // if we are showing a battle, then this will be set to the currently
  // displayed battle. This will only be set after the display
  // is shown on the screen
  private volatile GUID currentBattleDisplayed;
  // there is a bug in linux jdk1.5.0_6 where frames are not
  // being garbage collected
  // reuse one frame
  private final JFrame battleFrame;
  Map<BattleType, Collection<Territory>> battles;

  /** Creates new BattlePanel. */
  public BattlePanel(final GameData data, final MapPanel map) {
    super(data, map);
    battleFrame = new JFrame() {
      private static final long serialVersionUID = -947813247703330615L;

      @Override
      public void dispose() {
        games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(null);
        super.dispose();
      }
    };
    battleFrame.setIconImage(GameRunner.getGameIcon(battleFrame));
    getMap().getUIContext().addShutdownWindow(battleFrame);
    battleFrame.addWindowListener(new WindowListener() {
      @Override
      public void windowActivated(final WindowEvent e) {
        SwingUtilities.invokeLater(() -> {
          if (battleDisplay != null) {
            battleDisplay.takeFocus();
          }
        });
      }

      @Override
      public void windowClosed(final WindowEvent e) {}

      @Override
      public void windowClosing(final WindowEvent e) {}

      @Override
      public void windowDeactivated(final WindowEvent e) {}

      @Override
      public void windowDeiconified(final WindowEvent e) {}

      @Override
      public void windowIconified(final WindowEvent e) {}

      @Override
      public void windowOpened(final WindowEvent e) {}
    });
  }

  public void setBattlesAndBombing(final Map<BattleType, Collection<Territory>> battles) {
    this.battles = battles;
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeAll();
        actionLabel.setText(id.getName() + " battle");
        setLayout(new BorderLayout());
        final JPanel panel = SwingComponents.gridPanel(0, 1);
        panel.add(actionLabel);
        for (final Entry<BattleType, Collection<Territory>> entry : battles.entrySet()) {
          for (final Territory t : entry.getValue()) {
            addBattleActions(panel, t, entry.getKey().isBombingRun(), entry.getKey());
          }
        }
        add(panel, BorderLayout.NORTH);
        SwingUtilities.invokeLater(REFRESH);
      }

      private void addBattleActions(final JPanel panel, final Territory territory, final boolean bomb,
          final BattleType battleType) {
        final JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout());
        innerPanel.add(new JButton(new FightBattleAction(territory, bomb, battleType)), BorderLayout.CENTER);
        innerPanel.add(new JButton(new CenterBattleAction(territory)), BorderLayout.EAST);
        panel.add(innerPanel);
      }
    });
  }

  public void notifyRetreat(final String messageShort, final String messageLong, final String step,
      final PlayerID retreatingPlayer) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.battleInfo(messageLong, step);
      }
    });
  }

  public void showDice(final DiceRoll dice, final String step) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.battleInfo(dice, step);
      }
    });
  }

  public void battleEndMessage(final String message) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.endBattle(message, battleFrame);
      }
    });
  }

  private void cleanUpBattleWindow() {
    if (battleDisplay != null) {
      currentBattleDisplayed = null;
      battleDisplay.cleanUp();
      battleFrame.getContentPane().removeAll();
      battleDisplay = null;
      games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(battleFrame);
    }
  }

  private boolean ensureBattleIsDisplayed(final GUID battleId) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong threads");
    }
    GUID displayed = currentBattleDisplayed;
    int count = 0;
    while (displayed == null || !battleId.equals(displayed)) {
      count++;
      ThreadUtil.sleep(count);
      // something is wrong, we shouldnt have to wait this long
      if (count > 200) {
        ErrorConsole.getConsole().dumpStacks();
        new IllegalStateException(
            "battle not displayed, looking for:" + battleId + " showing:" + currentBattleDisplayed).printStackTrace();
        return false;
      }
      displayed = currentBattleDisplayed;
    }
    return true;
  }

  protected JFrame getBattleFrame() {
    return battleFrame;
  }

  public void listBattle(final GUID battleId, final List<String> steps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      final Runnable r = () -> {
        // recursive call
        listBattle(battleId, steps);
      };
      try {
        SwingUtilities.invokeLater(r);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
      }
      return;
    }
    removeAll();
    if (battleDisplay != null) {
      getMap().centerOn(battleDisplay.getBattleLocation());
      battleDisplay.listBattle(steps);
    }
  }

  public void showBattle(final GUID battleId, final Territory location,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits, final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie,
      final PlayerID attacker, final PlayerID defender,
      final boolean isAmphibious, final BattleType battleType, final Collection<Unit> amphibiousLandAttackers) {
    SwingAction.invokeAndWait(() -> {
      if (battleDisplay != null) {
        cleanUpBattleWindow();
        currentBattleDisplayed = null;
      }
      if (!getMap().getUIContext().getShowMapOnly()) {
        battleDisplay = new BattleDisplay(getData(), location, attacker, defender, attackingUnits, defendingUnits,
            killedUnits, attackingWaitingToDie, defendingWaitingToDie, battleId, BattlePanel.this.getMap(),
            isAmphibious, battleType, amphibiousLandAttackers);
        battleFrame.setTitle(attacker.getName() + " attacks " + defender.getName() + " in " + location.getName());
        battleFrame.getContentPane().removeAll();
        battleFrame.getContentPane().add(battleDisplay);
        battleFrame.setSize(800, 600);
        battleFrame.setLocationRelativeTo(JOptionPane.getFrameForComponent(BattlePanel.this));
        games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(battleFrame);
        boolean foundHumanInBattle = false;
        for (final IGamePlayer gamePlayer : getMap().getUIContext().getLocalPlayers().getLocalPlayers()) {
          if ((gamePlayer.getPlayerID().equals(attacker) && gamePlayer instanceof TripleAPlayer)
              || (gamePlayer.getPlayerID().equals(defender) && gamePlayer instanceof TripleAPlayer)) {
            foundHumanInBattle = true;
            break;
          }
        }
        if (getMap().getUIContext().getShowBattlesBetweenAIs() || foundHumanInBattle) {
          battleFrame.setVisible(true);
          battleFrame.validate();
          battleFrame.invalidate();
          battleFrame.repaint();
        } else {
          battleFrame.setVisible(false);
        }
        battleFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        currentBattleDisplayed = battleId;
        SwingUtilities.invokeLater(() -> battleFrame.toFront());
      }
    });
  }

  FightBattleDetails waitForBattleSelection() {
    waitForRelease();
    if (fightBattleMessage != null) {
      getMap().centerOn(fightBattleMessage.getWhere());
    }
    return fightBattleMessage;
  }

  /**
   * Ask user which territory to bombard with a given unit.
   */
  public Territory getBombardment(final Unit unit, final Territory unitTerritory,
      final Collection<Territory> territories, final boolean noneAvailable) {
    final BombardComponent comp = Util.runInSwingEventThread(
        () -> new BombardComponent(unit, unitTerritory, territories, noneAvailable));
    int option = JOptionPane.NO_OPTION;
    while (option != JOptionPane.OK_OPTION) {
      option = EventThreadJOptionPane.showConfirmDialog(this, comp, "Bombardment Territory Selection",
          JOptionPane.OK_OPTION, getMap().getUIContext().getCountDownLatchHandler());
    }
    return comp.getSelection();
  }

  public boolean getAttackSubs(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(null, "Attack submarines in " + terr.toString() + "?", "Attack",
        JOptionPane.YES_NO_OPTION, getMap().getUIContext().getCountDownLatchHandler()) == 0;
  }

  public boolean getAttackTransports(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(null, "Attack transports in " + terr.toString() + "?", "Attack",
        JOptionPane.YES_NO_OPTION, getMap().getUIContext().getCountDownLatchHandler()) == 0;
  }

  public boolean getAttackUnits(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(null, "Attack units in " + terr.toString() + "?", "Attack",
        JOptionPane.YES_NO_OPTION, getMap().getUIContext().getCountDownLatchHandler()) == 0;
  }

  public boolean getShoreBombard(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(null, "Conduct naval bombard in " + terr.toString() + "?",
        "Bombard", JOptionPane.YES_NO_OPTION, getMap().getUIContext().getCountDownLatchHandler()) == 0;
  }

  public void casualtyNotification(final String step, final DiceRoll dice, final PlayerID player,
      final Collection<Unit> killed, final Collection<Unit> damaged, final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.casualtyNotification(step, dice, player, killed, damaged, dependents);
      }
    });
  }

  public void deadUnitNotification(final PlayerID player, final Collection<Unit> killed,
      final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.deadUnitNotification(player, killed, dependents);
      }
    });
  }

  public void changedUnitsNotification(final PlayerID player, final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits, final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.changedUnitsNotification(player, removedUnits, addedUnits, dependents);
      }
    });
  }

  public void confirmCasualties(final GUID battleId, final String message) {
    // something is wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return;
    }
    battleDisplay.waitForConfirmation(message);
  }

  public CasualtyDetails getCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents,
      final int count, final String message, final DiceRoll dice, final PlayerID hit,
      final CasualtyList defaultCasualties, final GUID battleId, final boolean allowMultipleHitsPerUnit) {
    // if the battle display is null, then this is an aa fire during move
    if (battleId == null) {
      return getCasualtiesAA(selectFrom, dependents, count, message, dice, hit, defaultCasualties,
          allowMultipleHitsPerUnit);
    } else {
      // something is wong
      if (!ensureBattleIsDisplayed(battleId)) {
        System.out.println("Battle Not Displayed?? " + message);
        return new CasualtyDetails(defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
      }
      return battleDisplay.getCasualties(selectFrom, dependents, count, message, dice, hit, defaultCasualties,
          allowMultipleHitsPerUnit);
    }
  }

  private CasualtyDetails getCasualtiesAA(final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
      final PlayerID hit, final CasualtyList defaultCasualties, final boolean allowMultipleHitsPerUnit) {
    final Task<CasualtyDetails> task = () -> {
      final boolean isEditMode = (dice == null);
      final UnitChooser chooser = new UnitChooser(selectFrom, defaultCasualties, dependents, getData(),
          allowMultipleHitsPerUnit, getMap().getUIContext());
      chooser.setTitle(message);
      if (isEditMode) {
        chooser.setMax(selectFrom.size());
      } else {
        chooser.setMax(count);
      }
      final DicePanel dicePanel = new DicePanel(getMap().getUIContext(), getData());
      if (!isEditMode) {
        dicePanel.setDiceRoll(dice);
      }
      final JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout());
      panel.add(chooser, BorderLayout.CENTER);
      dicePanel.setMaximumSize(new Dimension(450, 600));
      dicePanel.setPreferredSize(new Dimension(300, (int) dicePanel.getPreferredSize().getHeight()));
      panel.add(dicePanel, BorderLayout.SOUTH);
      final String[] options = {"OK"};
      EventThreadJOptionPane.showOptionDialog(getRootPane(), panel, hit.getName() + " select casualties",
          JOptionPane.OK_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, null,
          getMap().getUIContext().getCountDownLatchHandler());
      final List<Unit> killed = chooser.getSelected(false);
      final CasualtyDetails response =
          new CasualtyDetails(killed, chooser.getSelectedDamagedMultipleHitPointUnits(), false);
      return response;
    };
    return Util.runInSwingEventThread(task);
  }

  public Territory getRetreat(final GUID battleId, final String message, final Collection<Territory> possible,
      final boolean submerge) {
    // something is really wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return null;
    }
    return battleDisplay.getRetreat(message, possible, submerge);
  }

  public void gotoStep(final GUID battleId, final String step) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.setStep(step);
      }
    });
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.notifyRetreat(retreating);
      }
    });
  }

  public void bombingResults(final GUID battleId, final List<Die> dice, final int cost) {
    SwingUtilities.invokeLater(() -> {
      if (battleDisplay != null) {
        battleDisplay.bombingResults(dice, cost);
      }
    });
  }

  Territory oldCenteredTerritory = null;
  Timer centerBattleActionTimer = null;

  class CenterBattleAction extends AbstractAction {
    private static final long serialVersionUID = -5071133874755970334L;
    Territory m_territory;

    CenterBattleAction(final Territory battleSite) {
      super("Center");
      m_territory = battleSite;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (centerBattleActionTimer != null) {
        centerBattleActionTimer.cancel();
      }
      if (oldCenteredTerritory != null) {
        getMap().clearTerritoryOverlay(oldCenteredTerritory);
      }
      getMap().centerOn(m_territory);
      centerBattleActionTimer = new Timer();
      centerBattleActionTimer.scheduleAtFixedRate(new MyTimerTask(m_territory, centerBattleActionTimer), 150, 150);
      oldCenteredTerritory = m_territory;
    }

    class MyTimerTask extends TimerTask {
      private final Territory territory;
      private final Timer m_stopTimer;
      private int m_count = 0;

      MyTimerTask(final Territory battleSite, final Timer stopTimer) {
        territory = battleSite;
        m_stopTimer = stopTimer;
      }

      @Override
      public void run() {
        if (m_count == 5) {
          m_stopTimer.cancel();
        }
        if ((m_count % 3) == 0) {
          getMap().setTerritoryOverlayForBorder(territory, Color.white);
          getMap().paintImmediately(getMap().getBounds());
          // TODO: getUIContext().getMapData().getBoundingRect(m_territory)); what kind of additional transformation
          // needed here?
          // TODO: setTerritoryOverlayForBorder is causing invalid ordered lock acquire atempt, why?
        } else {
          getMap().clearTerritoryOverlay(territory);
          getMap().paintImmediately(getMap().getBounds());
          // TODO: getUIContext().getMapData().getBoundingRect(m_territory)); what kind of additional transformation
          // needed here?
          // TODO: setTerritoryOverlayForBorder is causing invalid ordered lock acquire atempt, why?
        }
        m_count++;
      }
    }
  }

  class FightBattleAction extends AbstractAction {
    private static final long serialVersionUID = 5510976406003707776L;
    Territory m_territory;
    boolean m_bomb;
    BattleType m_type;

    FightBattleAction(final Territory battleSite, final boolean bomb, final BattleType battleType) {
      super(battleType.toString() + " in " + battleSite.getName() + "...");
      m_territory = battleSite;
      m_bomb = bomb;
      m_type = battleType;
    }

    @Override
    public void actionPerformed(final ActionEvent actionEvent) {
      if (oldCenteredTerritory != null) {
        getMap().clearTerritoryOverlay(oldCenteredTerritory);
      }
      fightBattleMessage = new FightBattleDetails(m_territory, m_bomb, m_type);
      release();
    }
  }

  @Override
  public String toString() {
    return "BattlePanel";
  }

  private class BombardComponent extends JPanel {
    private static final long serialVersionUID = -2388895995673156507L;
    private final JList<Object> list;

    BombardComponent(final Unit unit, final Territory unitTerritory, final Collection<Territory> territories,
        final boolean noneAvailable) {
      this.setLayout(new BorderLayout());
      final String unitName = unit.getUnitType().getName() + " in " + unitTerritory;
      final JLabel label = new JLabel("Which territory should " + unitName + " bombard?");
      this.add(label, BorderLayout.NORTH);
      final Vector<Object> listElements = new Vector<>(territories);
      if (noneAvailable) {
        listElements.add(0, "None");
      }
      list = new JList<>(listElements);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (listElements.size() >= 1) {
        list.setSelectedIndex(0);
      }
      final JScrollPane scroll = new JScrollPane(list);
      this.add(scroll, BorderLayout.CENTER);
    }

    public Territory getSelection() {
      final Object selected = list.getSelectedValue();
      if (selected instanceof Territory) {
        return (Territory) selected;
      }
      // User selected "None" option
      return null;
    }
  }
}
