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
  private final JLabel m_actionLabel = new JLabel();
  private FightBattleDetails m_fightBattleMessage;
  private volatile BattleDisplay m_battleDisplay;
  // if we are showing a battle, then this will be set to the currently
  // displayed battle. This will only be set after the display
  // is shown on the screen
  private volatile GUID m_currentBattleDisplayed;
  // there is a bug in linux jdk1.5.0_6 where frames are not
  // being garbage collected
  // reuse one frame
  private final JFrame m_battleFrame;
  Map<BattleType, Collection<Territory>> m_battles;

  /** Creates new BattlePanel */
  public BattlePanel(final GameData data, final MapPanel map) {
    super(data, map);
    m_battleFrame = new JFrame() {
      private static final long serialVersionUID = -947813247703330615L;

      @Override
      public void dispose() {
        games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(null);
        super.dispose();
      }
    };
    m_battleFrame.setIconImage(GameRunner.getGameIcon(m_battleFrame));
    getMap().getUIContext().addShutdownWindow(m_battleFrame);
    m_battleFrame.addWindowListener(new WindowListener() {
      @Override
      public void windowActivated(final WindowEvent e) {
        SwingUtilities.invokeLater(() -> {
          if (m_battleDisplay != null) {
            m_battleDisplay.takeFocus();
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
    m_battles = battles;
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        removeAll();
        m_actionLabel.setText(id.getName() + " battle");
        setLayout(new BorderLayout());
        final JPanel panel = SwingComponents.gridPanel(0, 1);
        panel.add(m_actionLabel);
        for (final Entry<BattleType, Collection<Territory>> entry : m_battles.entrySet()) {
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
      if (m_battleDisplay != null) {
        m_battleDisplay.battleInfo(messageLong, step);
      }
    });
  }

  public void showDice(final DiceRoll dice, final String step) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.battleInfo(dice, step);
      }
    });
  }

  public void battleEndMessage(final String message) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.endBattle(message, m_battleFrame);
      }
    });
  }

  private void cleanUpBattleWindow() {
    if (m_battleDisplay != null) {
      m_currentBattleDisplayed = null;
      m_battleDisplay.cleanUp();
      m_battleFrame.getContentPane().removeAll();
      m_battleDisplay = null;
      games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(m_battleFrame);
    }
  }

  private boolean ensureBattleIsDisplayed(final GUID battleID) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong threads");
    }
    GUID displayed = m_currentBattleDisplayed;
    int count = 0;
    while (displayed == null || !battleID.equals(displayed)) {
      count++;
      ThreadUtil.sleep(count);
      // something is wrong, we shouldnt have to wait this long
      if (count > 200) {
        ErrorConsole.getConsole().dumpStacks();
        new IllegalStateException(
            "battle not displayed, looking for:" + battleID + " showing:" + m_currentBattleDisplayed).printStackTrace();
        return false;
      }
      displayed = m_currentBattleDisplayed;
    }
    return true;
  }

  protected JFrame getBattleFrame() {
    return m_battleFrame;
  }

  public void listBattle(final GUID battleID, final List<String> steps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      final Runnable r = () -> {
        // recursive call
        listBattle(battleID, steps);
      };
      try {
        SwingUtilities.invokeLater(r);
      } catch (final Exception e) {
        ClientLogger.logQuietly(e);
      }
      return;
    }
    removeAll();
    if (m_battleDisplay != null) {
      getMap().centerOn(m_battleDisplay.getBattleLocation());
      m_battleDisplay.listBattle(steps);
    }
  }

  public void showBattle(final GUID battleID, final Territory location,
      final Collection<Unit> attackingUnits, final Collection<Unit> defendingUnits, final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie, final Collection<Unit> defendingWaitingToDie,
      final PlayerID attacker, final PlayerID defender,
      final boolean isAmphibious, final BattleType battleType, final Collection<Unit> amphibiousLandAttackers) {
    SwingAction.invokeAndWait(() -> {
      if (m_battleDisplay != null) {
        cleanUpBattleWindow();
        m_currentBattleDisplayed = null;
      }
      if (!getMap().getUIContext().getShowMapOnly()) {
        m_battleDisplay = new BattleDisplay(getData(), location, attacker, defender, attackingUnits, defendingUnits,
            killedUnits, attackingWaitingToDie, defendingWaitingToDie, battleID, BattlePanel.this.getMap(),
            isAmphibious, battleType, amphibiousLandAttackers);
        m_battleFrame.setTitle(attacker.getName() + " attacks " + defender.getName() + " in " + location.getName());
        m_battleFrame.getContentPane().removeAll();
        m_battleFrame.getContentPane().add(m_battleDisplay);
        m_battleFrame.setSize(800, 600);
        m_battleFrame.setLocationRelativeTo(JOptionPane.getFrameForComponent(BattlePanel.this));
        games.strategy.engine.random.PBEMDiceRoller.setFocusWindow(m_battleFrame);
        boolean foundHumanInBattle = false;
        for (final IGamePlayer gamePlayer : getMap().getUIContext().getLocalPlayers().getLocalPlayers()) {
          if ((gamePlayer.getPlayerID().equals(attacker) && gamePlayer instanceof TripleAPlayer)
              || (gamePlayer.getPlayerID().equals(defender) && gamePlayer instanceof TripleAPlayer)) {
            foundHumanInBattle = true;
            break;
          }
        }
        if (getMap().getUIContext().getShowBattlesBetweenAIs() || foundHumanInBattle) {
          m_battleFrame.setVisible(true);
          m_battleFrame.validate();
          m_battleFrame.invalidate();
          m_battleFrame.repaint();
        } else {
          m_battleFrame.setVisible(false);
        }
        m_battleFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        m_currentBattleDisplayed = battleID;
        SwingUtilities.invokeLater(() -> m_battleFrame.toFront());
      }
    });
  }

  public FightBattleDetails waitForBattleSelection() {
    waitForRelease();
    if (m_fightBattleMessage != null) {
      getMap().centerOn(m_fightBattleMessage.getWhere());
    }
    return m_fightBattleMessage;
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
      if (m_battleDisplay != null) {
        m_battleDisplay.casualtyNotification(step, dice, player, killed, damaged, dependents);
      }
    });
  }

  public void deadUnitNotification(final PlayerID player, final Collection<Unit> killed,
      final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.deadUnitNotification(player, killed, dependents);
      }
    });
  }

  public void changedUnitsNotification(final PlayerID player, final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits, final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.changedUnitsNotification(player, removedUnits, addedUnits, dependents);
      }
    });
  }

  public void confirmCasualties(final GUID battleId, final String message) {
    // something is wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return;
    }
    m_battleDisplay.waitForConfirmation(message);
  }

  public CasualtyDetails getCasualties(final Collection<Unit> selectFrom, final Map<Unit, Collection<Unit>> dependents,
      final int count, final String message, final DiceRoll dice, final PlayerID hit,
      final CasualtyList defaultCasualties, final GUID battleID, final boolean allowMultipleHitsPerUnit) {
    // if the battle display is null, then this is an aa fire during move
    if (battleID == null) {
      return getCasualtiesAA(selectFrom, dependents, count, message, dice, hit, defaultCasualties,
          allowMultipleHitsPerUnit);
    } else {
      // something is wong
      if (!ensureBattleIsDisplayed(battleID)) {
        System.out.println("Battle Not Displayed?? " + message);
        return new CasualtyDetails(defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
      }
      return m_battleDisplay.getCasualties(selectFrom, dependents, count, message, dice, hit, defaultCasualties,
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

  public Territory getRetreat(final GUID battleID, final String message, final Collection<Territory> possible,
      final boolean submerge) {
    // something is really wrong
    if (!ensureBattleIsDisplayed(battleID)) {
      return null;
    }
    return m_battleDisplay.getRetreat(message, possible, submerge);
  }

  public void gotoStep(final GUID battleID, final String step) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.setStep(step);
      }
    });
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.notifyRetreat(retreating);
      }
    });
  }

  public void bombingResults(final GUID battleID, final List<Die> dice, final int cost) {
    SwingUtilities.invokeLater(() -> {
      if (m_battleDisplay != null) {
        m_battleDisplay.bombingResults(dice, cost);
      }
    });
  }

  Territory m_oldCenteredTerritory = null;
  Timer m_CenterBattleActionTimer = null;

  class CenterBattleAction extends AbstractAction {
    private static final long serialVersionUID = -5071133874755970334L;
    Territory m_territory;

    CenterBattleAction(final Territory battleSite) {
      super("Center");
      m_territory = battleSite;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (m_CenterBattleActionTimer != null) {
        m_CenterBattleActionTimer.cancel();
      }
      if (m_oldCenteredTerritory != null) {
        getMap().clearTerritoryOverlay(m_oldCenteredTerritory);
      }
      getMap().centerOn(m_territory);
      m_CenterBattleActionTimer = new Timer();
      m_CenterBattleActionTimer.scheduleAtFixedRate(new MyTimerTask(m_territory, m_CenterBattleActionTimer), 150, 150);
      m_oldCenteredTerritory = m_territory;
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
      if (m_oldCenteredTerritory != null) {
        getMap().clearTerritoryOverlay(m_oldCenteredTerritory);
      }
      m_fightBattleMessage = new FightBattleDetails(m_territory, m_bomb, m_type);
      release();
    }
  }

  @Override
  public String toString() {
    return "BattlePanel";
  }

  private class BombardComponent extends JPanel {
    private static final long serialVersionUID = -2388895995673156507L;
    private final JList<Object> m_list;

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
      m_list = new JList<>(listElements);
      m_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (listElements.size() >= 1) {
        m_list.setSelectedIndex(0);
      }
      final JScrollPane scroll = new JScrollPane(m_list);
      this.add(scroll, BorderLayout.CENTER);
    }

    public Territory getSelection() {
      final Object selected = m_list.getSelectedValue();
      if (selected instanceof Territory) {
        return (Territory) selected;
      }
      // User selected "None" option
      return null;
    }
  }
}
