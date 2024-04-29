package games.strategy.triplea.delegate.battle;

import static java.util.function.Predicate.not;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.change.HistoryChangeFactory;
import games.strategy.engine.history.change.units.RemoveUnitsHistoryChange;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.casualty.CasualtySelector;
import games.strategy.triplea.delegate.battle.casualty.CasualtySortingUtil;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.PredicateBuilder;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/** Battle class used for air battles and interception before a standard battle. */
@Slf4j
public class AirBattle extends AbstractBattle {
  protected static final String AIR_BATTLE = "Air Battle";
  protected static final String INTERCEPTORS_LAUNCH = "Defender Launches Interceptors";
  protected static final String ATTACKERS_FIRE = "Attackers Fire";
  protected static final String DEFENDERS_FIRE = "Defenders Fire";
  protected static final String ATTACKERS_WITHDRAW = "Attackers Withdraw?";
  protected static final String DEFENDERS_WITHDRAW = "Defenders Withdraw?";
  private static final long serialVersionUID = 4686241714027216395L;

  protected final ExecutionStack stack = new ExecutionStack();
  protected List<String> steps;
  protected final Collection<Unit> defendingWaitingToDie = new ArrayList<>();
  protected final Collection<Unit> attackingWaitingToDie = new ArrayList<>();
  protected boolean intercept = false;
  // -1 would mean forever until one side is eliminated. (default is 1 round)
  protected final int maxRounds;

  AirBattle(
      final Territory battleSite,
      final BattleType battleType,
      final GameData data,
      final GamePlayer attacker,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, battleType, data);
    isAmphibious = false;
    maxRounds = Properties.getAirBattleRounds(data.getProperties());
    updateDefendingUnits();
  }

  /** Updates the set of defending units from current battle site. */
  public void updateDefendingUnits() {
    // fill in defenders
    if (isBombingRun) {
      defendingUnits =
          battleSite.getMatches(defendingBombingRaidInterceptors(battleSite, attacker, gameData));
    } else {
      defendingUnits =
          battleSite.getMatches(defendingGroundSeaBattleInterceptors(attacker, gameData));
    }
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {
    // Avoid duplicates. Note: This is needed as scrambling code calls addAirBattle(), but the
    // battle may have already been created with the same attackers. They should not be added twice.
    units.stream().filter(not(attackingUnits::contains)).forEach(attackingUnits::add);
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public Change removeAttack(final Route route, final Collection<Unit> units) {
    attackingUnits.removeAll(units);
    return new CompositeChange();
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    // remove units that may already be dead due to a previous event (like they died from a
    // strategic bombing raid,
    // rocket attack, etc)
    removeUnitsThatNoLongerExist();
    // we were interrupted
    if (stack.isExecuting()) {
      showBattle(bridge);
      stack.execute(bridge);
      return;
    }
    updateDefendingUnits();
    bridge.getHistoryWriter().startEvent("Air Battle in " + battleSite, battleSite);
    CasualtySortingUtil.sortPreBattle(attackingUnits);
    CasualtySortingUtil.sortPreBattle(defendingUnits);
    steps = determineStepStrings(true);
    showBattle(bridge);
    pushFightLoopOnStack(true);
    stack.execute(bridge);
  }

  private void pushFightLoopOnStack(final boolean firstRun) {
    if (isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables(firstRun);
    // add in the reverse order we create them
    Collections.reverse(steps);
    for (final IExecutable step : steps) {
      stack.push(step);
    }
  }

  private boolean shouldFightAirBattle() {
    return !defendingUnits.isEmpty()
        && (isBombingRun
            ? attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())
            : !attackingUnits.isEmpty());
  }

  public boolean shouldEndBattleDueToMaxRounds() {
    return maxRounds > 0 && maxRounds <= round;
  }

  protected boolean canAttackerRetreat() {
    return !shouldEndBattleDueToMaxRounds()
        && shouldFightAirBattle()
        && Properties.getAirBattleAttackersCanRetreat(gameData.getProperties());
  }

  protected boolean canDefenderRetreat() {
    return !shouldEndBattleDueToMaxRounds()
        && shouldFightAirBattle()
        && Properties.getAirBattleDefendersCanRetreat(gameData.getProperties());
  }

  List<IExecutable> getBattleExecutables(final boolean firstRun) {
    final List<IExecutable> steps = new ArrayList<>();
    if (shouldFightAirBattle()) {
      if (firstRun) {
        steps.add(new InterceptorsLaunch());
      }
      steps.add(new AttackersFire());
      steps.add(new DefendersFire());
      steps.add(
          new IExecutable() { // just calculates lost TUV and kills off any suicide units
            private static final long serialVersionUID = -5575569705493214941L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              if (!intercept) {
                return;
              }
              final IntegerMap<UnitType> defenderCosts = bridge.getCostsForTuv(defender);
              final IntegerMap<UnitType> attackerCosts = bridge.getCostsForTuv(attacker);
              attackingUnits.removeAll(attackingWaitingToDie);
              remove(attackingWaitingToDie, bridge, battleSite);
              defendingUnits.removeAll(defendingWaitingToDie);
              remove(defendingWaitingToDie, bridge, battleSite);
              int tuvLostAttacker =
                  TuvUtils.getTuv(attackingWaitingToDie, attacker, attackerCosts, gameData);
              attackerLostTuv += tuvLostAttacker;
              int tuvLostDefender =
                  TuvUtils.getTuv(defendingWaitingToDie, defender, defenderCosts, gameData);
              defenderLostTuv += tuvLostDefender;
              attackingWaitingToDie.clear();
              defendingWaitingToDie.clear();
              // kill any suicide attackers (veqryn)
              final Predicate<Unit> attackerSuicide =
                  PredicateBuilder.of(Matches.unitIsSuicideOnAttack())
                      .andIf(isBombingRun, not(Matches.unitIsStrategicBomber()))
                      .build();
              if (attackingUnits.stream().anyMatch(attackerSuicide)) {
                final List<Unit> suicideUnits =
                    CollectionUtils.getMatches(attackingUnits, Matches.unitIsSuicideOnAttack());
                attackingUnits.removeAll(suicideUnits);
                remove(suicideUnits, bridge, battleSite);
                tuvLostAttacker = TuvUtils.getTuv(suicideUnits, attacker, attackerCosts, gameData);
                attackerLostTuv += tuvLostAttacker;
              }
              if (defendingUnits.stream().anyMatch(Matches.unitIsSuicideOnDefense())) {
                final List<Unit> suicideUnits =
                    CollectionUtils.getMatches(defendingUnits, Matches.unitIsSuicideOnDefense());
                defendingUnits.removeAll(suicideUnits);
                remove(suicideUnits, bridge, battleSite);
                tuvLostDefender = TuvUtils.getTuv(suicideUnits, defender, defenderCosts, gameData);
                defenderLostTuv += tuvLostDefender;
              }
            }
          });
    }
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 3148193405425861565L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
              return;
            }
            makeBattle(bridge);
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = 3148193405425861565L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
              return;
            }
            end(bridge);
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -5408702756335356985L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver && canAttackerRetreat()) {
              // planes retreat to the same square the battle is in, and then should move during
              // non combat to their landing site, or be scrapped if they can't find one.
              queryRetreat(false, bridge, battleSite);
            }
          }
        });
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -7819137222487595113L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver && canDefenderRetreat()) {
              // planes retreat to the same square the battle is in, and then should move during
              // non combat to their landing site, or be scrapped if they can't find one
              queryRetreat(true, bridge, battleSite);
            }
          }
        });
    final IExecutable loop =
        new IExecutable() {
          private static final long serialVersionUID = -5408702756335356985L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            pushFightLoopOnStack(false);
          }
        };
    steps.add(
        new IExecutable() {
          private static final long serialVersionUID = -4136481765101946944L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!isOver) {
              AirBattle.this.steps = determineStepStrings(false);
              final IDisplay display = bridge.getDisplayChannelBroadcaster();
              display.listBattleSteps(battleId, AirBattle.this.steps);
              round++;
              // continue fighting
              // the recursive step
              // this should always be the base of the stack
              // when we execute the loop, it will populate the stack with the battle steps
              if (!AirBattle.this.stack.isEmpty()) {
                throw new IllegalStateException("Stack not empty:" + AirBattle.this.stack);
              }
              AirBattle.this.stack.push(loop);
            }
          }
        });
    return steps;
  }

  private List<String> determineStepStrings(final boolean showFirstRun) {
    final List<String> steps = new ArrayList<>();
    if (showFirstRun) {
      steps.add(AIR_BATTLE);
      steps.add(INTERCEPTORS_LAUNCH);
    }
    steps.add(ATTACKERS_FIRE);
    steps.add(DEFENDERS_FIRE);
    if (canAttackerRetreat()) {
      steps.add(ATTACKERS_WITHDRAW);
    }
    if (canDefenderRetreat()) {
      steps.add(DEFENDERS_WITHDRAW);
    }
    // steps.add(BOMBERS_TO_TARGETS);
    return steps;
  }

  private static void recordUnitsWereInAirBattle(
      final Collection<Unit> units, final IDelegateBridge bridge) {
    final CompositeChange wasInAirBattleChange = new CompositeChange();
    for (final Unit u : units) {
      wasInAirBattleChange.add(ChangeFactory.unitPropertyChange(u, true, Unit.WAS_IN_AIR_BATTLE));
    }
    if (!wasInAirBattleChange.isEmpty()) {
      bridge.addChange(wasInAirBattleChange);
    }
  }

  private void makeBattle(final IDelegateBridge bridge) {
    // record who was in this battle first, so that they do not take part in any ground battles
    if (isBombingRun) {
      recordUnitsWereInAirBattle(attackingUnits, bridge);
      recordUnitsWereInAirBattle(defendingUnits, bridge);
    }
    // As of right now, Air Battles are created before both normal battles and strategic bombing
    // raids. Once completed, the air battle will create a strategic bombing raid, if that is the
    // purpose of those aircraft. However, if the purpose is a normal battle, it will have already
    // been created by the battle tracker / combat move.
    // So we do not have to create normal battles, only bombing raids setup new battle here.
    if (isBombingRun) {
      final Collection<Unit> bombers =
          CollectionUtils.getMatches(attackingUnits, Matches.unitIsStrategicBomber());
      if (!bombers.isEmpty()) {
        Map<Unit, Set<Unit>> targets = null;
        final Collection<Unit> enemyTargetsTotal =
            battleSite.getMatches(
                Matches.enemyUnit(bridge.getGamePlayer())
                    .and(Matches.unitCanBeDamaged())
                    .and(Matches.unitIsBeingTransported().negate()));
        for (final Unit unit : bombers) {
          final Collection<Unit> enemyTargets =
              CollectionUtils.getMatches(
                  enemyTargetsTotal, Matches.unitIsLegalBombingTargetBy(unit));
          if (!enemyTargets.isEmpty()) {
            Unit target = null;
            if (enemyTargets.size() > 1
                && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
                    gameData.getProperties())) {
              while (target == null) {
                target =
                    getRemote(bridge).whatShouldBomberBomb(battleSite, enemyTargets, List.of(unit));
              }
            } else {
              target = CollectionUtils.getAny(enemyTargets);
            }
            if (target != null) {
              targets = new HashMap<>();
              targets.put(target, new HashSet<>(Set.of(unit)));
            }
            battleTracker.addBattle(
                new RouteScripted(battleSite),
                Set.of(unit),
                true,
                attacker,
                bridge,
                null,
                null,
                targets,
                true);
          }
        }
        final IBattle battle = battleTracker.getPendingBombingBattle(battleSite);
        final IBattle dependent = battleTracker.getPendingBattle(battleSite, BattleType.NORMAL);
        if (dependent != null) {
          battleTracker.addDependency(dependent, battle);
        }
        final IBattle dependentAirBattle =
            battleTracker.getPendingBattle(battleSite, BattleType.AIR_BATTLE);
        if (dependentAirBattle != null) {
          battleTracker.addDependency(dependentAirBattle, battle);
        }
      }
    }
  }

  private void end(final IDelegateBridge bridge) {
    // record it
    final String text;
    if (!attackingUnits.isEmpty()) {
      if (isBombingRun) {
        if (attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())) {
          whoWon = WhoWon.ATTACKER;
          if (defendingUnits.isEmpty()) {
            battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          } else {
            battleResultDescription = BattleRecord.BattleResultDescription.WON_WITH_ENEMY_LEFT;
          }
          text = "Air Battle is over, the remaining bombers go on to their targets";
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
        } else {
          whoWon = WhoWon.DRAW;
          battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, the bombers have all died";
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, attacker);
        }
      } else {
        if (defendingUnits.isEmpty()) {
          whoWon = WhoWon.ATTACKER;
          battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          text = "Air Battle is over, the defenders have all died";
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
        } else {
          whoWon = WhoWon.DRAW;
          battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, neither side is eliminated";
          bridge
              .getSoundChannelBroadcaster()
              .playSoundForAll(SoundPath.CLIP_BATTLE_STALEMATE, attacker);
        }
      }
    } else {
      whoWon = WhoWon.DEFENDER;
      battleResultDescription = BattleRecord.BattleResultDescription.LOST;
      text = "Air Battle is over, the attackers have all died";
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, attacker);
    }
    bridge.getHistoryWriter().addChildToEvent(text);
    battleTracker
        .getBattleRecords()
        .addResultToBattle(
            attacker,
            battleId,
            defender,
            attackerLostTuv,
            defenderLostTuv,
            battleResultDescription,
            new BattleResults(this, gameData));
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, "Air Battle over");
    isOver = true;
    battleTracker.removeBattle(AirBattle.this, bridge.getData());
  }

  void finishBattleAndRemoveFromTrackerHeadless(final IDelegateBridge bridge) {
    makeBattle(bridge);
    whoWon = WhoWon.ATTACKER;
    battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
    battleTracker.getBattleRecords().removeBattle(attacker, battleId);
    isOver = true;
    battleTracker.removeBattle(AirBattle.this, bridge.getData());
  }

  private void queryRetreat(
      final boolean defender, final IDelegateBridge bridge, final Territory battleSite) {
    final Collection<Unit> units =
        defender ? new ArrayList<>(defendingUnits) : new ArrayList<>(attackingUnits);
    if (units.isEmpty()) {
      return;
    }
    final String step = defender ? DEFENDERS_WITHDRAW : ATTACKERS_WITHDRAW;
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      bridge.sendMessage(new IDisplay.GoToBattleStepMessage(battleId.toString(), step));
    } else {
      bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleId, step);
    }

    final GamePlayer retreatingPlayer = defender ? this.defender : attacker;
    final String text = retreatingPlayer.getName() + " retreat?";
    final Territory retreatTo =
        getRemote(retreatingPlayer, bridge)
            .retreatQuery(battleId, false, battleSite, List.of(battleSite), text);
    if (retreatTo == null) {
      return;
    }
    if (!retreatTo.equals(battleSite)) {
      log.error("Invalid retreat selection : {} does not equal {}", retreatTo, battleSite);
      return;
    }
    if (!headless) {
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, attacker);
    }
    retreat(units, defender, bridge);
    final String messageShort = retreatingPlayer.getName() + " retreats";
    final String messageLong =
        retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
    if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
      bridge.sendMessage(
          IDisplay.NotifyRetreatMessage.builder()
              .shortMessage(messageShort)
              .message(messageLong)
              .step(step)
              .retreatingPlayerName(retreatingPlayer.getName())
              .build());
    } else {
      bridge
          .getDisplayChannelBroadcaster()
          .notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
    }
  }

  private void retreat(
      final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge) {
    if (!defender) {
      // we must remove any of these units from the land battle that follows (this comes before we
      // remove them from this
      // battle, because after we remove from this battle we are no longer blocking any battles)
      final Collection<IBattle> dependentBattles = battleTracker.getBlocked(AirBattle.this);
      removeFromDependents(retreating, bridge, dependentBattles, true);
    }
    final String transcriptText =
        MyFormatter.unitsToText(retreating) + (defender ? " grounded" : " retreated");
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    units.removeAll(retreating);
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
    recordUnitsWereInAirBattle(retreating, bridge);
  }

  private void showBattle(final IDelegateBridge bridge) {
    final String title = "Air Battle in " + battleSite.getName();
    bridge
        .getDisplayChannelBroadcaster()
        .showBattle(
            battleId,
            battleSite,
            title,
            attackingUnits,
            defendingUnits,
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            attacker,
            defender,
            false,
            getBattleType(),
            Set.of());
    bridge.getDisplayChannelBroadcaster().listBattleSteps(battleId, steps);
  }

  /**
   * Finds the maximum number of units that can intercept from a given territory including checking
   * on any air base requirements.
   */
  public static int getMaxInterceptionCount(final Territory t, final Collection<Unit> possible) {
    if (possible.stream().noneMatch(Matches.unitRequiresAirBaseToIntercept())) {
      return Integer.MAX_VALUE;
    }
    int result = 0;
    for (final Unit base : t.getMatches(Matches.unitIsAirBase().and(Matches.unitIsNotDisabled()))) {
      final int baseMax = base.getUnitAttachment().getMaxInterceptCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      result += baseMax;
    }
    return result;
  }

  class InterceptorsLaunch implements IExecutable {
    private static final long serialVersionUID = 4300406315014471768L;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      getInterceptors(bridge);
      if (!defendingUnits.isEmpty()) {
        intercept = true;
        // play a sound
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR, attacker);
      }
    }

    private void getInterceptors(final IDelegateBridge bridge) {
      final boolean groundedPlanesRetreated;
      final Collection<Unit> interceptors;
      if (isBombingRun) {
        // if bombing run, ask who will intercept
        interceptors =
            getRemote(defender, bridge)
                .selectUnitsQuery(
                    battleSite, new ArrayList<>(defendingUnits), "Select Air to Intercept");
        groundedPlanesRetreated = false;
      } else {
        // if normal battle, we may choose to withdraw some air units (keep them grounded for both
        // Air battle and the
        // subsequent normal battle) instead of launching
        if (Properties.getAirBattleDefendersCanRetreat(gameData.getProperties())) {
          interceptors =
              getRemote(defender, bridge)
                  .selectUnitsQuery(
                      battleSite, new ArrayList<>(defendingUnits), "Select Air to Intercept");
          groundedPlanesRetreated = true;
        } else {
          // if not allowed to withdraw, we must commit all air
          interceptors = new ArrayList<>(defendingUnits);
          groundedPlanesRetreated = false;
        }
      }
      if (interceptors != null
          && (!defendingUnits.containsAll(interceptors)
              || interceptors.size() > getMaxInterceptionCount(battleSite, defendingUnits))) {
        throw new IllegalStateException("Interceptors choose from outside of available units");
      }
      final Collection<Unit> beingRemoved = new ArrayList<>(defendingUnits);
      defendingUnits.clear();
      if (interceptors != null) {
        beingRemoved.removeAll(interceptors);
        defendingUnits.addAll(interceptors);
      }
      bridge
          .getDisplayChannelBroadcaster()
          .changedUnitsNotification(battleId, defender, beingRemoved, null, null);
      if (groundedPlanesRetreated) {
        // this removes them from the subsequent normal battle. (do not use this for bombing
        // battles)
        retreat(beingRemoved, true, bridge);
      }
      if (!attackingUnits.isEmpty()) {
        bridge
            .getHistoryWriter()
            .addChildToEvent(
                attacker.getName()
                    + " attacks with "
                    + attackingUnits.size()
                    + " units heading to "
                    + battleSite.getName(),
                new ArrayList<>(attackingUnits));
      }
      if (!defendingUnits.isEmpty()) {
        bridge
            .getHistoryWriter()
            .addChildToEvent(
                defender.getName()
                    + " launches "
                    + defendingUnits.size()
                    + " interceptors out of "
                    + battleSite.getName(),
                new ArrayList<>(defendingUnits));
      }
    }
  }

  class AttackersFire implements IExecutable {
    private static final long serialVersionUID = -5289634214875797408L;

    DiceRoll dice;
    CasualtyDetails details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!intercept) {
        return;
      }
      final IExecutable roll =
          new IExecutable() {
            private static final long serialVersionUID = 6579019987019614374L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              dice =
                  RollDiceFactory.rollBattleDice(
                      attackingUnits,
                      attacker,
                      bridge,
                      "Attackers Fire, ",
                      CombatValueBuilder.airBattleCombatValue()
                          .side(BattleState.Side.OFFENSE)
                          .lhtrHeavyBombers(
                              Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                          .gameDiceSides(bridge.getData().getDiceSides())
                          .build());
            }
          };
      final IExecutable calculateCasualties =
          new IExecutable() {
            private static final long serialVersionUID = 4556409970663527142L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              details =
                  CasualtySelector.selectCasualties(
                      defender,
                      defendingUnits,
                      CombatValueBuilder.mainCombatValue()
                          .enemyUnits(attackingUnits)
                          .friendlyUnits(defendingUnits)
                          .side(BattleState.Side.DEFENSE)
                          .gameSequence(bridge.getData().getSequence())
                          .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                          .lhtrHeavyBombers(
                              Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                          .gameDiceSides(bridge.getData().getDiceSides())
                          .territoryEffects(List.of())
                          .build(),
                      battleSite,
                      bridge,
                      ATTACKERS_FIRE,
                      dice,
                      battleId,
                      false,
                      dice.getHits(),
                      true);
              defendingWaitingToDie.addAll(details.getKilled());
              markDamaged(details.getDamaged(), bridge);
            }
          };
      final IExecutable notifyCasualties =
          new IExecutable() {
            private static final long serialVersionUID = 4224354422817922451L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              notifyCasualties(battleId, bridge, ATTACKERS_FIRE, dice, defender, attacker, details);
            }
          };
      // push in reverse order of execution
      stack.push(notifyCasualties);
      stack.push(calculateCasualties);
      stack.push(roll);
    }
  }

  class DefendersFire implements IExecutable {
    private static final long serialVersionUID = -7277182945495744003L;

    DiceRoll dice;
    CasualtyDetails details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!intercept) {
        return;
      }
      final IExecutable roll =
          new IExecutable() {
            private static final long serialVersionUID = 5953506121350176595L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              dice =
                  RollDiceFactory.rollBattleDice(
                      defendingUnits,
                      defender,
                      bridge,
                      "Defenders Fire, ",
                      CombatValueBuilder.airBattleCombatValue()
                          .side(BattleState.Side.DEFENSE)
                          .lhtrHeavyBombers(
                              Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                          .gameDiceSides(bridge.getData().getDiceSides())
                          .build());
            }
          };
      final IExecutable calculateCasualties =
          new IExecutable() {
            private static final long serialVersionUID = 6658309931909306564L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              details =
                  CasualtySelector.selectCasualties(
                      attacker,
                      attackingUnits,
                      CombatValueBuilder.mainCombatValue()
                          .enemyUnits(defendingUnits)
                          .friendlyUnits(attackingUnits)
                          .side(BattleState.Side.OFFENSE)
                          .gameSequence(bridge.getData().getSequence())
                          .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                          .lhtrHeavyBombers(
                              Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                          .gameDiceSides(bridge.getData().getDiceSides())
                          .territoryEffects(List.of())
                          .build(),
                      battleSite,
                      bridge,
                      DEFENDERS_FIRE,
                      dice,
                      battleId,
                      false,
                      dice.getHits(),
                      true);
              attackingWaitingToDie.addAll(details.getKilled());
              markDamaged(details.getDamaged(), bridge);
            }
          };
      final IExecutable notifyCasualties =
          new IExecutable() {
            private static final long serialVersionUID = 4461950841000674515L;

            @Override
            public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
              notifyCasualties(battleId, bridge, DEFENDERS_FIRE, dice, attacker, defender, details);
            }
          };
      // push in reverse order of execution
      stack.push(notifyCasualties);
      stack.push(calculateCasualties);
      stack.push(roll);
    }
  }

  static Predicate<Unit> attackingGroundSeaBattleEscorts() {
    return Matches.unitCanAirBattle();
  }

  public static Predicate<Unit> defendingGroundSeaBattleInterceptors(
      final GamePlayer attacker, final GameState data) {
    return PredicateBuilder.of(Matches.unitCanAirBattle())
        .and(Matches.unitIsEnemyOf(attacker))
        .and(Matches.unitWasInAirBattle().negate())
        .andIf(
            !Properties.getCanScrambleIntoAirBattles(data.getProperties()),
            Matches.unitWasScrambled().negate())
        .build();
  }

  /**
   * Returns a unit predicate that determines if it can potentially intercept including checking any
   * air base requirements.
   */
  public static Predicate<Unit> defendingBombingRaidInterceptors(
      final Territory territory, final GamePlayer attacker, final GameState data) {
    final Predicate<Unit> canIntercept =
        PredicateBuilder.of(Matches.unitCanIntercept())
            .and(Matches.unitIsEnemyOf(attacker))
            .and(Matches.unitWasInAirBattle().negate())
            .andIf(
                !Properties.getCanScrambleIntoAirBattles(data.getProperties()),
                Matches.unitWasScrambled().negate())
            .build();
    final Predicate<Unit> airbasesCanIntercept =
        Matches.unitIsEnemyOf(attacker)
            .and(Matches.unitIsAirBase())
            .and(Matches.unitIsNotDisabled())
            .and(Matches.unitIsBeingTransported().negate());
    return u ->
        canIntercept.test(u)
            && (!Matches.unitRequiresAirBaseToIntercept().test(u)
                || Matches.territoryHasUnitsThatMatch(airbasesCanIntercept).test(territory));
  }

  /** Determines if enemy has any air units that can intercept to create an air battle. */
  public static boolean territoryCouldPossiblyHaveAirBattleDefenders(
      final Territory territory,
      final GamePlayer attacker,
      final GameState data,
      final boolean bombing) {
    final boolean canScrambleToAirBattle =
        Properties.getCanScrambleIntoAirBattles(data.getProperties());
    final Predicate<Unit> defendingAirMatch =
        bombing
            ? defendingBombingRaidInterceptors(territory, attacker, data)
            : defendingGroundSeaBattleInterceptors(attacker, data);
    int maxScrambleDistance = 0;
    if (canScrambleToAirBattle) {
      for (final UnitType unitType : data.getUnitTypeList()) {
        final UnitAttachment ua = unitType.getUnitAttachment();
        if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance()) {
          maxScrambleDistance = ua.getMaxScrambleDistance();
        }
      }
    } else {
      return territory.anyUnitsMatch(defendingAirMatch);
    }
    // should we check if the territory also has an air base?
    return territory.anyUnitsMatch(defendingAirMatch)
        || data.getMap().getNeighbors(territory, maxScrambleDistance).stream()
            .anyMatch(Matches.territoryHasUnitsThatMatch(defendingAirMatch));
  }

  private void remove(
      final Collection<Unit> killedUnits,
      final IDelegateBridge bridge,
      final Territory battleSite) {
    if (killedUnits.isEmpty()) {
      return;
    }
    final Collection<Unit> killed = getUnitsWithDependents(killedUnits);
    final RemoveUnitsHistoryChange removeUnitsHistoryChange =
        HistoryChangeFactory.removeUnitsFromTerritory(battleSite, killed);
    removeUnitsHistoryChange.perform(bridge);

    final Collection<IBattle> dependentBattles = battleTracker.getBlocked(AirBattle.this);
    removeFromDependents(killed, bridge, dependentBattles, false);
  }

  private static void notifyCasualties(
      final UUID battleId,
      final IDelegateBridge bridge,
      final String stepName,
      final DiceRoll dice,
      final GamePlayer hitPlayer,
      final GamePlayer firingPlayer,
      final CasualtyDetails details) {
    bridge
        .getDisplayChannelBroadcaster()
        .casualtyNotification(
            battleId,
            stepName,
            dice,
            hitPlayer,
            details.getKilled(),
            details.getDamaged(),
            Map.of());
    // execute in a separate thread to allow either player to click continue first.
    final Thread t =
        new Thread(
            () -> {
              try {
                getRemote(firingPlayer, bridge)
                    .confirmEnemyCasualties(battleId, "Press space to continue", hitPlayer);
              } catch (final Exception e) {
                log.error("Error during casualty notification", e);
              }
            },
            "Click to continue waiter");
    t.start();
    getRemote(hitPlayer, bridge).confirmOwnCasualties(battleId, "Press space to continue");
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  private static void removeFromDependents(
      final Collection<Unit> units,
      final IDelegateBridge bridge,
      final Collection<IBattle> dependents,
      final boolean withdrawn) {
    for (final IBattle dependent : dependents) {
      dependent.unitsLostInPrecedingBattle(units, bridge, withdrawn);
    }
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty();
  }

  @Override
  public void unitsLostInPrecedingBattle(
      final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn) {}
}
