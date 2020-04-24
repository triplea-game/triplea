package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.Player;
import games.strategy.triplea.TripleAPlayer;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.data.FightBattleDetails;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import lombok.extern.java.Log;
import org.triplea.java.Interruptibles;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** UI for fighting battles. */
@Log
public final class BattlePanel extends ActionPanel {
  private static final long serialVersionUID = 5304208569738042592L;
  private final JLabel actionLabel = new JLabel();
  private FightBattleDetails fightBattleMessage;
  private volatile BattleDisplay battleDisplay;
  // if we are showing a battle, then this will be set to the currently displayed battle. This will
  // only be set after
  // the display is shown on the screen
  private volatile UUID currentBattleDisplayed;
  private final JDialog battleWindow;
  private Map<BattleType, Collection<Territory>> battles;

  BattlePanel(final GameData data, final MapPanel map, final JFrame parent) {
    super(data, map);
    battleWindow = new JDialog(parent);
    battleWindow.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowActivated(final WindowEvent e) {
            SwingUtilities.invokeLater(
                () -> Optional.ofNullable(battleDisplay).ifPresent(BattleDisplay::takeFocus));
          }
        });
    getMap().getUiContext().addShutdownWindow(battleWindow);
  }

  void setBattlesAndBombing(final Map<BattleType, Collection<Territory>> battles) {
    this.battles = battles;
  }

  @Override
  public void display(final GamePlayer gamePlayer) {
    super.display(gamePlayer);
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          actionLabel.setText(gamePlayer.getName() + " battle");
          setLayout(new BorderLayout());
          final JPanel panel = new JPanelBuilder().gridLayout(0, 1).add(actionLabel).build();
          for (final Entry<BattleType, Collection<Territory>> entry : battles.entrySet()) {
            for (final Territory t : entry.getValue()) {
              addBattleActions(panel, t, entry.getKey().isBombingRun(), entry.getKey());
            }
          }
          add(panel, BorderLayout.NORTH);
          refresh.run();
        });
  }

  @Override
  public void performDone() {
    // no-op; battles are done when they all have been fought, there is no done button.
  }

  private void addBattleActions(
      final JPanel panel,
      final Territory territory,
      final boolean bomb,
      final BattleType battleType) {

    panel.add(
        new JPanelBuilder()
            .borderLayout()
            .addCenter(
                new JButton(
                    SwingAction.of(
                        battleType.toString() + " in " + territory.getName() + "...",
                        () -> fightBattleAction(territory, bomb, battleType))))
            .addEast(
                new JButtonBuilder()
                    .title("Center")
                    .actionListener(
                        () ->
                            getMap()
                                .highlightTerritory(
                                    territory,
                                    MapPanel.AnimationDuration.STANDARD,
                                    MapPanel.HighlightDelay.STANDARD_DELAY))
                    .build())
            .build());
  }

  private void fightBattleAction(
      final Territory territory, final boolean bomb, final BattleType battleType) {
    getMap().clearHighlightedTerritory();
    fightBattleMessage =
        FightBattleDetails.builder()
            .where(territory)
            .bombingRaid(bomb)
            .battleType(battleType)
            .build();
    release();
  }

  public void notifyRetreat(final String messageLong, final String step) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.battleInfo(messageLong, step);
          }
        });
  }

  public void notifyRetreat(final Collection<Unit> retreating) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.notifyRetreat(retreating);
          }
        });
  }

  public void showDice(final DiceRoll dice, final String step) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.battleInfo(dice, step);
          }
        });
  }

  public void battleEndMessage(final String message) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.endBattle(message, battleWindow);
          }
        });
  }

  private void cleanUpBattleWindow() {
    if (battleDisplay != null) {
      currentBattleDisplayed = null;
      battleDisplay.cleanUp();
      battleWindow.getContentPane().removeAll();
      battleDisplay = null;
    }
  }

  private boolean ensureBattleIsDisplayed(final UUID battleId) {
    if (SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong threads");
    }
    UUID displayed = currentBattleDisplayed;
    int count = 0;
    while (!battleId.equals(displayed)) {
      count++;
      Interruptibles.sleep(count);
      // something is wrong, we shouldn't have to wait this long
      if (count > 200) {
        log.severe(
            "battle not displayed, looking for:" + battleId + " showing:" + currentBattleDisplayed);
        return false;
      }
      displayed = currentBattleDisplayed;
    }
    return true;
  }

  boolean isBattleShowing() {
    return battleWindow.isVisible();
  }

  public void listBattle(final List<String> steps) {
    SwingAction.invokeNowOrLater(
        () -> {
          removeAll();
          if (battleDisplay != null) {
            getMap().centerOn(battleDisplay.getBattleLocation());
            battleDisplay.listBattle(steps);
          }
        });
  }

  /** Shows the Battle Window for the specified battle. */
  public void showBattle(
      final UUID battleId,
      final Territory location,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Collection<Unit> killedUnits,
      final Collection<Unit> attackingWaitingToDie,
      final Collection<Unit> defendingWaitingToDie,
      final GamePlayer attacker,
      final GamePlayer defender,
      final boolean isAmphibious,
      final BattleType battleType,
      final Collection<Unit> amphibiousLandAttackers) {
    // Copy all collection params so that they don't change underneath us while we execute async code.
    final Collection<Unit> attackingUnitsCopy = List.copyOf(attackingUnits);
    final Collection<Unit> defendingUnitsCopy = List.copyOf(defendingUnits);
    final Collection<Unit> killedUnitsCopy = List.copyOf(killedUnits);
    final Collection<Unit> attackingWaitingToDieCopy = List.copyOf(attackingWaitingToDie);
    final Collection<Unit> defendingWaitingToDieCopy = List.copyOf(defendingWaitingToDie);
    final Collection<Unit> amphibiousLandAttackersCopy = List.copyOf(amphibiousLandAttackers);
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            cleanUpBattleWindow();
            currentBattleDisplayed = null;
          }
          battleDisplay =
              new BattleDisplay(
                  getData(),
                  location,
                  attacker,
                  defender,
                  attackingUnitsCopy,
                  defendingUnitsCopy,
                  killedUnitsCopy,
                  attackingWaitingToDieCopy,
                  defendingWaitingToDieCopy,
                  BattlePanel.this.getMap(),
                  isAmphibious,
                  battleType,
                  amphibiousLandAttackersCopy);
          battleWindow.setTitle(
              attacker.getName() + " attacks " + defender.getName() + " in " + location.getName());
          battleWindow.getContentPane().removeAll();
          battleWindow.getContentPane().add(battleDisplay);
          battleWindow.setMinimumSize(new Dimension(800, 600));
          battleWindow.setLocationRelativeTo(JOptionPane.getFrameForComponent(BattlePanel.this));
          boolean foundHumanInBattle = false;
          for (final Player gamePlayer :
              getMap().getUiContext().getLocalPlayers().getLocalPlayers()) {
            if ((gamePlayer.getGamePlayer().equals(attacker) && gamePlayer instanceof TripleAPlayer)
                || (gamePlayer.getGamePlayer().equals(defender)
                    && gamePlayer instanceof TripleAPlayer)) {
              foundHumanInBattle = true;
              break;
            }
          }
          if (ClientSetting.showBattlesWhenObserving.getValueOrThrow() || foundHumanInBattle) {
            battleWindow.setVisible(true);
            battleWindow.validate();
            battleWindow.invalidate();
            battleWindow.repaint();
            battleWindow.toFront();
          } else {
            battleWindow.setVisible(false);
          }
          battleWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
          currentBattleDisplayed = battleId;
          SwingUtilities.invokeLater(battleWindow::toFront);
        });
  }

  FightBattleDetails waitForBattleSelection() {
    waitForRelease();
    if (fightBattleMessage != null) {
      getMap().centerOn(fightBattleMessage.getWhere());
    }
    return fightBattleMessage;
  }

  /** Ask user which territory to bombard with a given unit. */
  public @Nullable Territory getBombardment(
      final Unit unit,
      final Territory unitTerritory,
      final Collection<Territory> territories,
      final boolean noneAvailable) {
    final Supplier<BombardComponent> action =
        () -> new BombardComponent(unit, unitTerritory, territories, noneAvailable);
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            comp -> {
              int option = JOptionPane.NO_OPTION;
              while (option != JOptionPane.OK_OPTION) {
                option =
                    EventThreadJOptionPane.showConfirmDialog(
                        this,
                        comp,
                        "Bombardment Territory Selection",
                        JOptionPane.OK_OPTION,
                        getMap().getUiContext().getCountDownLatchHandler());
              }
              return comp.getSelection();
            })
        .orElse(null);
  }

  public boolean getAttackSubs(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
            null,
            "Attack submarines in " + terr.toString() + "?",
            "Attack",
            JOptionPane.YES_NO_OPTION,
            getMap().getUiContext().getCountDownLatchHandler())
        == 0;
  }

  public boolean getAttackTransports(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
            null,
            "Attack transports in " + terr.toString() + "?",
            "Attack",
            JOptionPane.YES_NO_OPTION,
            getMap().getUiContext().getCountDownLatchHandler())
        == 0;
  }

  public boolean getAttackUnits(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
            null,
            "Attack units in " + terr.toString() + "?",
            "Attack",
            JOptionPane.YES_NO_OPTION,
            getMap().getUiContext().getCountDownLatchHandler())
        == 0;
  }

  public boolean getShoreBombard(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
            null,
            "Conduct naval bombard in " + terr.toString() + "?",
            "Bombard",
            JOptionPane.YES_NO_OPTION,
            getMap().getUiContext().getCountDownLatchHandler())
        == 0;
  }

  public void casualtyNotification(
      final String step,
      final DiceRoll dice,
      final GamePlayer player,
      final Collection<Unit> killed,
      final Collection<Unit> damaged,
      final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.casualtyNotification(step, dice, player, killed, damaged, dependents);
          }
        });
  }

  public void deadUnitNotification(
      final GamePlayer player,
      final Collection<Unit> killed,
      final Map<Unit, Collection<Unit>> dependents) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.deadUnitNotification(player, killed, dependents);
          }
        });
  }

  public void changedUnitsNotification(
      final GamePlayer player,
      final Collection<Unit> removedUnits,
      final Collection<Unit> addedUnits) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.changedUnitsNotification(player, removedUnits, addedUnits);
          }
        });
  }

  public void confirmCasualties(final UUID battleId, final String message) {
    // something is wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return;
    }
    battleDisplay.waitForConfirmation(message);
  }

  /**
   * Prompts the user to select casualties from the specified collection of units.
   *
   * @return The selected casualties.
   */
  public CasualtyDetails getCasualties(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final CasualtyList defaultCasualties,
      final UUID battleId,
      final boolean allowMultipleHitsPerUnit) {
    // if the battle display is null, then this is an aa fire during move
    if (battleId == null) {
      return getCasualtiesAa(
          selectFrom,
          dependents,
          count,
          message,
          dice,
          hit,
          defaultCasualties,
          allowMultipleHitsPerUnit);
    }

    // something is wong
    if (!ensureBattleIsDisplayed(battleId)) {
      return new CasualtyDetails(
          defaultCasualties.getKilled(), defaultCasualties.getDamaged(), true);
    }
    return battleDisplay.getCasualties(
        selectFrom,
        dependents,
        count,
        message,
        dice,
        hit,
        defaultCasualties,
        allowMultipleHitsPerUnit);
  }

  private @Nullable CasualtyDetails getCasualtiesAa(
      final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents,
      final int count,
      final String message,
      final DiceRoll dice,
      final GamePlayer hit,
      final CasualtyList defaultCasualties,
      final boolean allowMultipleHitsPerUnit) {
    final Supplier<CasualtyDetails> action =
        () -> {
          final boolean isEditMode = (dice == null);
          final UnitChooser chooser =
              new UnitChooser(
                  selectFrom,
                  defaultCasualties,
                  dependents,
                  allowMultipleHitsPerUnit,
                  getMap().getUiContext());
          chooser.setTitle(message);
          if (isEditMode) {
            chooser.setMax(selectFrom.size());
          } else {
            chooser.setMax(count);
          }
          final DicePanel dicePanel = new DicePanel(getMap().getUiContext(), getData());
          if (!isEditMode) {
            dicePanel.setDiceRoll(dice);
          }
          final JPanel panel = new JPanel();
          panel.setLayout(new BorderLayout());
          panel.add(chooser, BorderLayout.CENTER);
          dicePanel.setMaximumSize(new Dimension(450, 600));
          dicePanel.setPreferredSize(
              new Dimension(300, (int) dicePanel.getPreferredSize().getHeight()));
          panel.add(dicePanel, BorderLayout.SOUTH);
          final String[] options = {"OK"};
          EventThreadJOptionPane.showOptionDialog(
              getRootPane(),
              panel,
              hit.getName() + " select casualties",
              JOptionPane.OK_OPTION,
              JOptionPane.PLAIN_MESSAGE,
              null,
              options,
              null,
              getMap().getUiContext().getCountDownLatchHandler());
          final List<Unit> killed = chooser.getSelected(false);
          return new CasualtyDetails(
              killed, chooser.getSelectedDamagedMultipleHitPointUnits(), false);
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .orElse(null);
  }

  public Territory getRetreat(
      final UUID battleId,
      final String message,
      final Collection<Territory> possible,
      final boolean submerge) {
    // something is really wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return null;
    }
    return battleDisplay.getRetreat(message, possible, submerge);
  }

  public void gotoStep(final String step) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.setStep(step);
          }
        });
  }

  public void bombingResults(final List<Die> dice, final int cost) {
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            battleDisplay.bombingResults(dice, cost);
          }
        });
  }

  @Override
  public String toString() {
    return "BattlePanel";
  }

  private static class BombardComponent extends JPanel {
    private static final long serialVersionUID = -2388895995673156507L;
    private final JList<Object> list;

    BombardComponent(
        final Unit unit,
        final Territory unitTerritory,
        final Collection<Territory> territories,
        final boolean noneAvailable) {
      this.setLayout(new BorderLayout());
      final String unitName = unit.getType().getName() + " in " + unitTerritory;
      final JLabel label = new JLabel("Which territory should " + unitName + " bombard?");
      this.add(label, BorderLayout.NORTH);
      final List<Object> listElements = new ArrayList<>(territories);
      if (noneAvailable) {
        listElements.add(0, "None");
      }
      list = new JList<>(SwingComponents.newListModel(listElements));
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (!listElements.isEmpty()) {
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
