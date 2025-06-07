package games.strategy.triplea.ui;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleListing;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import games.strategy.triplea.delegate.data.FightBattleDetails;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.panels.map.MapPanel;
import games.strategy.ui.Util;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.EventThreadJOptionPane.ConfirmDialogType;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

/** UI for fighting battles. */
@Slf4j
public final class BattlePanel extends ActionPanel {
  private static final long serialVersionUID = 5304208569738042592L;

  private FightBattleDetails fightBattleMessage;
  private volatile BattleDisplay battleDisplay;
  // if we are showing a battle, then this will be set to the currently displayed battle. This will
  // only be set after
  // the display is shown on the screen
  private volatile UUID currentBattleDisplayed;
  private final JDialog battleWindow;
  private BattleListing battleListing;

  BattlePanel(final TripleAFrame frame) {
    super(frame);
    battleWindow = new JDialog(frame);
    battleWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    battleWindow.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowActivated(final WindowEvent e) {
            SwingUtilities.invokeLater(
                () -> Optional.ofNullable(battleDisplay).ifPresent(BattleDisplay::takeFocus));
          }
        });
    final Dimension screenSize = Util.getScreenSize(battleWindow);
    if (screenSize.width > 1024 && screenSize.height > 768) {
      battleWindow.setMinimumSize(new Dimension(1024, 768));
    } else {
      battleWindow.setMinimumSize(new Dimension(800, 600));
    }
    getMap().getUiContext().addShutdownWindow(battleWindow);
  }

  void setBattlesAndBombing(final BattleListing battleListing) {
    this.battleListing = battleListing;
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
          battleListing.forEachBattle(
              (battleType, territory) -> addBattleActions(panel, territory, battleType));
          add(panel, BorderLayout.NORTH);
          refresh.run();
        });
  }

  @Override
  public void performDone() {
    // no-op; battles are done when they all have been fought, there is no done button.
  }

  private void addBattleActions(
      final JPanel panel, final Territory territory, final BattleType battleType) {

    panel.add(
        new JPanelBuilder()
            .borderLayout()
            .addCenter(
                new JButton(
                    SwingAction.of(
                        battleType.toDisplayText() + " in " + territory.getName() + "...",
                        () -> fightBattleAction(territory, battleType))))
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

  private void fightBattleAction(final Territory territory, final BattleType battleType) {
    getMap().clearHighlightedTerritory();
    fightBattleMessage =
        FightBattleDetails.builder()
            .where(territory)
            .bombingRaid(battleType.isBombingRun())
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
    Util.ensureNotOnEventDispatchThread();
    UUID displayed = currentBattleDisplayed;
    int count = 0;
    while (!battleId.equals(displayed)) {
      count++;
      Interruptibles.sleep(count);
      // something is wrong, we shouldn't have to wait this long
      if (count > 200) {
        log.error(
            "battle not displayed, looking for: "
                + battleId
                + " showing: "
                + currentBattleDisplayed);
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
      final BattleType battleType) {
    // Copy all collection params so that they don't change underneath us while we execute async
    // code.
    final Collection<Unit> attackingUnitsCopy = List.copyOf(attackingUnits);
    final Collection<Unit> defendingUnitsCopy = List.copyOf(defendingUnits);
    final Collection<Unit> killedUnitsCopy = List.copyOf(killedUnits);
    final Collection<Unit> attackingWaitingToDieCopy = List.copyOf(attackingWaitingToDie);
    final Collection<Unit> defendingWaitingToDieCopy = List.copyOf(defendingWaitingToDie);
    SwingUtilities.invokeLater(
        () -> {
          if (battleDisplay != null) {
            cleanUpBattleWindow();
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
                  battleType);
          battleWindow.setTitle(battleDisplay.getDescription());
          battleWindow.getContentPane().add(battleDisplay);

          final var localPlayers = getMap().getUiContext().getLocalPlayers();
          if (ClientSetting.showBattlesWhenObserving.getValueOrThrow()
              || localPlayers.playing(attacker)
              || localPlayers.playing(defender)) {
            battleWindow.setLocationRelativeTo(battleWindow);
            battleWindow.setVisible(true);
            SwingComponents.redraw(battleWindow);
            battleWindow.toFront();
          } else {
            battleWindow.setVisible(false);
          }
          currentBattleDisplayed = battleId;
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
      final Unit unit, final Territory unitTerritory, final Collection<Territory> territories) {
    final Supplier<SelectTerritoryComponent> action =
        () -> {
          final var panel = new SelectTerritoryComponent(unitTerritory, territories, map);
          final String unitName = unit.getType().getName() + " in " + unitTerritory;
          panel.setLabelText("Which territory should " + unitName + " bombard?");
          return panel;
        };
    return Interruptibles.awaitResult(() -> SwingAction.invokeAndWaitResult(action))
        .result
        .map(
            panel -> {
              int choice =
                  EventThreadJOptionPane.showOptionDialog(
                      this,
                      panel,
                      "Bombardment Territory Selection",
                      JOptionPane.OK_CANCEL_OPTION,
                      JOptionPane.PLAIN_MESSAGE,
                      null,
                      new String[] {"OK", "None"},
                      null,
                      getMap().getUiContext().getCountDownLatchHandler());
              if (choice != JOptionPane.OK_OPTION) {
                // User selected the "None" option.
                return null;
              }
              return panel.getSelection();
            })
        .orElse(null);
  }

  public boolean getAttackSubs(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
        null, "Attack submarines in " + terr.toString() + "?", "Attack", ConfirmDialogType.YES_NO);
  }

  public boolean getAttackTransports(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
        null, "Attack transports in " + terr.toString() + "?", "Attack", ConfirmDialogType.YES_NO);
  }

  public boolean getAttackUnits(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
        null, "Attack units in " + terr.toString() + "?", "Attack", ConfirmDialogType.YES_NO);
  }

  public boolean getShoreBombard(final Territory terr) {
    getMap().centerOn(terr);
    return EventThreadJOptionPane.showConfirmDialog(
        null,
        "Conduct naval bombard in " + terr.toString() + "?",
        "Bombard",
        ConfirmDialogType.YES_NO);
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
                  Properties.getPartialAmphibiousRetreat(hit.getData().getProperties()),
                  false,
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

  public Optional<Territory> getRetreat(
      final UUID battleId,
      final String message,
      final Collection<Territory> possible,
      final boolean submerge) {
    // something is really wrong
    if (!ensureBattleIsDisplayed(battleId)) {
      return Optional.empty();
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
}
