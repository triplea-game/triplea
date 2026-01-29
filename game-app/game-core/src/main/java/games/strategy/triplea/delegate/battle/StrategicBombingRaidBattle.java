package games.strategy.triplea.delegate.battle;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.history.change.HistoryChangeFactory;
import games.strategy.engine.player.Player;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.EditDelegate;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.IExecutable;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.casualty.AaCasualtySelector;
import games.strategy.triplea.delegate.battle.casualty.CasualtySortingUtil;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.dice.RollDiceFactory;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.util.TuvUtils;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.triplea.java.Interruptibles;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/** A strategic bombing raid (SBR) battle. */
public class StrategicBombingRaidBattle extends AbstractBattle implements BattleStepStrings {
  private static final long serialVersionUID = 8490171037606078890L;
  private static final String RAID = "Strategic bombing raid";

  // these would be the factories or other targets. does not include aa.
  private final Map<Unit, Set<Unit>> targetsForAiFire = new HashMap<>();
  private final ExecutionStack stack = new ExecutionStack();
  private final IntegerMap<Unit> bombingRaidDamage = new IntegerMap<>();
  private List<String> steps;
  private List<Unit> defendingAa;
  private List<String> aaTypes;
  private int bombingRaidTotal;

  public StrategicBombingRaidBattle(
      final Territory battleSite,
      final GameData data,
      final GamePlayer attacker,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, BattleType.BOMBING_RAID, data);
    isAmphibious = false;
    updateDefendingUnits();
  }

  private static int getSbrRolls(final Collection<Unit> units, final GamePlayer gamePlayer) {
    return units.stream().mapToInt(u -> getSbrRolls(u, gamePlayer)).sum();
  }

  @VisibleForTesting
  public static int getSbrRolls(final Unit unit, final GamePlayer gamePlayer) {
    return unit.getUnitAttachment().getAttackRolls(gamePlayer);
  }

  @Override
  protected void removeUnitsThatNoLongerExist() {
    if (headless) {
      return;
    }
    // we were having a problem with units that had been killed previously were still part of
    // battle's variables, so we
    // double-check that the stuff still exists here.
    defendingUnits.retainAll(battleSite.getUnits());
    attackingUnits.retainAll(battleSite.getUnits());
    targetsForAiFire.keySet().removeIf(unit -> !battleSite.getUnits().contains(unit));
  }

  protected void updateDefendingUnits() {
    // fill in defenders

    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(
            TechTracker.getCurrentTechAdvances(attacker, gameData.getTechnologyFrontier()));
    final Predicate<Unit> defenders =
        Matches.enemyUnit(attacker)
            .and(
                Matches.unitCanBeDamaged()
                    .or(
                        Matches.unitIsAaThatCanFire(
                            attackingUnits,
                            airborneTechTargetsAllowed,
                            attacker,
                            Matches.unitIsAaForBombingThisUnitOnly(),
                            round,
                            true)));
    if (targetsForAiFire.isEmpty()) {
      defendingUnits = CollectionUtils.getMatches(battleSite.getUnits(), defenders);
    } else {
      final List<Unit> targetsForAaFire =
          CollectionUtils.getMatches(
              battleSite.getUnits(),
              Matches.unitIsAaThatCanFire(
                  attackingUnits,
                  airborneTechTargetsAllowed,
                  attacker,
                  Matches.unitIsAaForBombingThisUnitOnly(),
                  round,
                  true));
      targetsForAaFire.addAll(this.targetsForAiFire.keySet());
      defendingUnits = targetsForAaFire;
    }
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty();
  }

  @Override
  public Change removeAttack(final Route route, final Collection<Unit> units) {
    removeAttackers(units, true);
    return new CompositeChange();
  }

  private void removeAttackers(final Collection<Unit> units, final boolean removeTarget) {
    attackingUnits.removeAll(units);
    final Iterator<Unit> targetIter = targetsForAiFire.keySet().iterator();
    while (targetIter.hasNext()) {
      final Set<Unit> currentAttackers = targetsForAiFire.get(targetIter.next());
      currentAttackers.removeAll(units);
      if (currentAttackers.isEmpty() && removeTarget) {
        targetIter.remove();
      }
    }
  }

  private Unit getTarget(final Unit attacker) {
    return targetsForAiFire.entrySet().stream()
        .filter(e -> e.getValue().contains(attacker))
        .map(Entry::getKey)
        .findAny()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    MessageFormat.format("Unit {0} has no target", attacker.getType().getName())));
  }

  @Override
  public Change addAttackChange(
      final Route route, final Collection<Unit> units, final Map<Unit, Set<Unit>> targets) {
    attackingUnits.addAll(units);
    if (targets == null) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    targets.forEach(
        (target, targetAttackers) -> {
          final Set<Unit> currentAttackers =
              this.targetsForAiFire.computeIfAbsent(target, i -> new HashSet<>());
          currentAttackers.addAll(targetAttackers);
        });
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    // remove units that may already be dead due to a previous event (like they died from a
    // strategic bombing raid,
    // rocket attack, etc.)
    removeUnitsThatNoLongerExist();
    // we were interrupted
    if (stack.isExecuting()) {
      showBattle(bridge);
      stack.execute(bridge);
      return;
    }
    // We update Defending Units twice: first time when the battle is created, and second time
    // before the battle begins.
    // The reason is when the battle is created, there are no attacking units yet in it,
    // meaning that targets
    // is empty. We need to update right as battle begins to know we have the full list of targets.
    updateDefendingUnits();
    bridge
        .getHistoryWriter()
        .startEvent(MessageFormat.format("Strategic bombing raid in {0}", battleSite), battleSite);
    if (attackingUnits.isEmpty()
        || (defendingUnits.isEmpty()
            || defendingUnits.stream().noneMatch(Matches.unitCanBeDamaged()))) {
      endBeforeRolling(bridge);
      return;
    }
    CasualtySortingUtil.sortPreBattle(attackingUnits);
    // TODO: determine if the target has the property, not just any unit with the property
    // isAaForBombingThisUnitOnly

    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(
            TechTracker.getCurrentTechAdvances(attacker, gameData.getTechnologyFrontier()));
    defendingAa =
        battleSite
            .getUnitCollection()
            .getMatches(
                Matches.unitIsAaThatCanFire(
                    attackingUnits,
                    airborneTechTargetsAllowed,
                    attacker,
                    Matches.unitIsAaForBombingThisUnitOnly(),
                    round,
                    true));
    aaTypes = UnitAttachment.getAllOfTypeAas(defendingAa);
    // reverse since stacks are in reverse order
    Collections.reverse(aaTypes);
    final boolean hasAa = !defendingAa.isEmpty();
    steps = new ArrayList<>();
    if (hasAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(defendingAa)) {
        steps.add(typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(NOTIFY_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    steps.add(RAID);
    showBattle(bridge);
    final List<IExecutable> fightSteps = new ArrayList<>();
    if (hasAa) {
      // global1940 rules - each target type fires an AA shot against the planes bombing it
      fightSteps.addAll(
          targetsForAiFire.entrySet().stream()
              .filter(entry -> entry.getKey().getUnitAttachment().isAaForBombingThisUnitOnly())
              .map(Entry::getValue)
              .map(FireAa::new)
              .collect(Collectors.toList()));

      // otherwise fire an AA shot at all the planes
      if (fightSteps.isEmpty()) {
        fightSteps.add(new FireAa());
      }
    }
    fightSteps.add(conductBombing());
    fightSteps.add(postBombing());
    fightSteps.add(end());
    Collections.reverse(fightSteps);
    for (final IExecutable executable : fightSteps) {
      stack.push(executable);
    }
    stack.execute(bridge);
  }

  @Nonnull
  private IExecutable postBombing() {
    return new IExecutable() {
      private static final long serialVersionUID = 4299575008166316488L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleId, RAID);
        addPostBombingToHistory(bridge);
        // TODO remove the reference to the constant.japanese- replace with a rule
        if ((Properties.getPacificTheater(gameData.getProperties())
                || Properties.getSbrVictoryPoints(gameData.getProperties()))
            && defender.getName().equals(Constants.PLAYER_NAME_JAPANESE)) {
          final PlayerAttachment pa = PlayerAttachment.get(defender);
          if (pa != null) {
            final Change changeVp =
                ChangeFactory.attachmentPropertyChange(
                    pa, (-(bombingRaidTotal / 10) + pa.getVps()), "vps");
            bridge.addChange(changeVp);
            bridge
                .getHistoryWriter()
                .addChildToEvent(
                    MessageFormat.format(
                        "Bombing raid costs {0} {1}",
                        bombingRaidTotal / 10,
                        MyFormatter.pluralize("vp", (bombingRaidTotal / 10))));
          }
        }
        killAnySuicideAttackers(bridge);
        killAnyWithMaxDamageReached(bridge);
      }

      private void addPostBombingToHistory(IDelegateBridge bridge) {
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
            gameData.getProperties())) {
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  MessageFormat.format(
                      "Bombing raid in {0} causes {1} damage total. {2}",
                      battleSite.getName(),
                      bombingRaidTotal,
                      bombingRaidDamage.size() > 1
                          ? (MessageFormat.format(
                              " Damaged units is as follows: {0}",
                              MyFormatter.integerUnitMapToString(
                                  bombingRaidDamage, ", ", " = ", false)))
                          : ""));
        } else {
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  MessageFormat.format(
                      "Bombing raid costs {0} {1}",
                      bombingRaidTotal, MyFormatter.pluralize("PU", bombingRaidTotal)));
        }
      }

      private void killAnyWithMaxDamageReached(IDelegateBridge bridge) {
        if (targetsForAiFire.keySet().stream()
            .anyMatch(Matches.unitCanDieFromReachingMaxDamage())) {
          final List<Unit> unitsCanDie =
              CollectionUtils.getMatches(
                  targetsForAiFire.keySet(), Matches.unitCanDieFromReachingMaxDamage());
          unitsCanDie.retainAll(
              CollectionUtils.getMatches(
                  unitsCanDie, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(battleSite)));
          if (!unitsCanDie.isEmpty()) {
            HistoryChangeFactory.removeUnitsFromTerritory(battleSite, unitsCanDie).perform(bridge);
            final IntegerMap<UnitType> costs = bridge.getCostsForTuv(defender);
            final int tuvLostDefender = TuvUtils.getTuv(unitsCanDie, defender, costs, gameData);
            defenderLostTuv += tuvLostDefender;
          }
        }
      }

      private void killAnySuicideAttackers(IDelegateBridge bridge) {
        if (attackingUnits.stream().anyMatch(Matches.unitIsSuicideOnAttack())) {
          final List<Unit> suicideUnits =
              CollectionUtils.getMatches(attackingUnits, Matches.unitIsSuicideOnAttack());
          attackingUnits.removeAll(suicideUnits);

          HistoryChangeFactory.removeUnitsFromTerritory(battleSite, suicideUnits).perform(bridge);

          final IntegerMap<UnitType> costs = bridge.getCostsForTuv(attacker);
          final int tuvLostAttacker = TuvUtils.getTuv(suicideUnits, attacker, costs, gameData);
          attackerLostTuv += tuvLostAttacker;
        }
      }
    };
  }

  private void endBeforeRolling(final IDelegateBridge bridge) {
    bridge.getDisplayChannelBroadcaster().battleEnd(battleId, "Bombing raid does no damage");
    whoWon = WhoWon.DRAW;
    battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
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
    isOver = true;
    battleTracker.removeBattle(StrategicBombingRaidBattle.this, gameData);
  }

  private IExecutable end() {
    return new IExecutable() {
      private static final long serialVersionUID = -7649516174883172328L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
            gameData.getProperties())) {
          bridge
              .getDisplayChannelBroadcaster()
              .battleEnd(
                  battleId,
                  MessageFormat.format(
                      "Raid causes {0} damage total.{1}",
                      bombingRaidTotal,
                      bombingRaidDamage.size() > 1
                          ? (MessageFormat.format(
                              " To units: {0}",
                              MyFormatter.integerUnitMapToString(
                                  bombingRaidDamage, ", ", " = ", false)))
                          : ""));
        } else {
          bridge
              .getDisplayChannelBroadcaster()
              .battleEnd(
                  battleId,
                  MessageFormat.format(
                      "Bombing raid cost {0} {1}",
                      bombingRaidTotal, MyFormatter.pluralize("PU", bombingRaidTotal)));
        }
        if (bombingRaidTotal > 0) {
          whoWon = WhoWon.ATTACKER;
          battleResultDescription = BattleRecord.BattleResultDescription.BOMBED;
        } else {
          whoWon = WhoWon.DEFENDER;
          battleResultDescription = BattleRecord.BattleResultDescription.LOST;
        }
        battleTracker
            .getBattleRecords()
            .addResultToBattle(
                attacker,
                battleId,
                defender,
                attackerLostTuv,
                defenderLostTuv,
                battleResultDescription,
                new BattleResults(StrategicBombingRaidBattle.this, gameData));
        isOver = true;
        battleTracker.removeBattle(StrategicBombingRaidBattle.this, gameData);
      }
    };
  }

  private void showBattle(final IDelegateBridge bridge) {
    final String title = MessageFormat.format("Bombing raid in {0}", battleSite.getName());
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

  private CasualtyDetails calculateCasualties(
      final Collection<Unit> validAttackingUnitsForThisRoll,
      final Collection<Unit> defendingAa,
      final IDelegateBridge bridge,
      final DiceRoll dice,
      final String currentTypeAa) {
    bridge
        .getDisplayChannelBroadcaster()
        .notifyDice(dice, SELECT_PREFIX + currentTypeAa + CASUALTIES_SUFFIX);
    final CasualtyDetails casualties =
        AaCasualtySelector.getAaCasualties(
            validAttackingUnitsForThisRoll,
            defendingAa,
            CombatValueBuilder.mainCombatValue()
                .enemyUnits(defendingUnits)
                .friendlyUnits(attackingUnits)
                .side(BattleState.Side.OFFENSE)
                .gameSequence(bridge.getData().getSequence())
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportRules())
                .lhtrHeavyBombers(Properties.getLhtrHeavyBombers(bridge.getData().getProperties()))
                .gameDiceSides(bridge.getData().getDiceSides())
                .territoryEffects(territoryEffects)
                .build(),
            CombatValueBuilder.aaCombatValue()
                .enemyUnits(attackingUnits)
                .friendlyUnits(defendingUnits)
                .side(BattleState.Side.DEFENSE)
                .supportAttachments(bridge.getData().getUnitTypeList().getSupportAaRules())
                .build(),
            "Hits from " + currentTypeAa + ", ",
            dice,
            bridge,
            attacker,
            battleId,
            battleSite);
    final int totalExpectingHits = Math.min(dice.getHits(), validAttackingUnitsForThisRoll.size());
    if (casualties.size() != totalExpectingHits) {
      throw new IllegalStateException(
          MessageFormat.format(
              "Wrong number of casualties, expecting:{0} but got:{1}",
              totalExpectingHits, casualties.size()));
    }
    return casualties;
  }

  private void notifyAaHits(
      final IDelegateBridge bridge,
      final DiceRoll dice,
      final CasualtyDetails casualties,
      final String currentTypeAa) {
    bridge
        .getDisplayChannelBroadcaster()
        .casualtyNotification(
            battleId,
            NOTIFY_PREFIX + currentTypeAa + CASUALTIES_SUFFIX,
            dice,
            attacker,
            new ArrayList<>(casualties.getKilled()),
            new ArrayList<>(casualties.getDamaged()),
            Map.of());
    final Thread t =
        new Thread(
            () -> {
              try {
                final Player defender = bridge.getRemotePlayer(this.defender);
                defender.confirmEnemyCasualties(battleId, "Press space to continue", attacker);
              } catch (final Exception e) {
                // ignore
              }
            },
            "click to continue waiter");
    t.start();
    final Player attacker = bridge.getRemotePlayer(this.attacker);
    attacker.confirmOwnCasualties(battleId, "Press space to continue");
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  private void removeAaHits(
      final IDelegateBridge bridge, final CasualtyDetails casualties, final String currentTypeAa) {
    final List<Unit> killed = casualties.getKilled();
    if (!killed.isEmpty()) {
      final IntegerMap<UnitType> costs = bridge.getCostsForTuv(attacker);
      final int tuvLostAttacker = TuvUtils.getTuv(killed, attacker, costs, gameData);
      attackerLostTuv += tuvLostAttacker;
      removeAttackers(killed, false);
      HistoryChangeFactory.removeUnitsWithAa(battleSite, killed, currentTypeAa).perform(bridge);
    }
  }

  @Override
  public void unitsLostInPrecedingBattle(
      final Collection<Unit> units, final IDelegateBridge bridge, final boolean withdrawn) {
    throw new IllegalStateException("This code should never be reached");
  }

  @Nonnull
  private IExecutable conductBombing() {
    return new IExecutable() {
      private static final long serialVersionUID = 5579796391988452213L;

      private int[] dice;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        final IExecutable rollDice =
            new IExecutable() {
              private static final long serialVersionUID = -4097858758514452368L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                rollDice(bridge);
              }
            };
        final IExecutable findCost =
            new IExecutable() {
              private static final long serialVersionUID = 8573539936364094095L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                findCost(bridge);
              }
            };
        // push in reverse order of execution
        StrategicBombingRaidBattle.this.stack.push(findCost);
        StrategicBombingRaidBattle.this.stack.push(rollDice);
      }

      private void rollDice(final IDelegateBridge bridge) {
        final int rollCount = getSbrRolls(attackingUnits, attacker);
        if (rollCount == 0) {
          dice = null;
          return;
        }
        dice = new int[rollCount];

        final boolean isEditMode = EditDelegate.getEditMode(gameData.getProperties());
        if (isEditMode) {
          final String annotation =
              MessageFormat.format(
                  "{0} fixing dice to allocate cost of strategic bombing raid against {1} in {2}",
                  attacker.getName(), defender.getName(), battleSite.getName());
          final Player attacker = bridge.getRemotePlayer(StrategicBombingRaidBattle.this.attacker);
          // does not take into account bombers with dice sides higher than getDiceSides
          dice = attacker.selectFixedDice(rollCount, 0, annotation, gameData.getDiceSides());
          return;
        }

        final String annotation =
            MessageFormat.format(
                "{0} rolling to allocate cost of strategic bombing raid against {1} in {2}",
                attacker.getName(), defender.getName(), battleSite.getName());
        final boolean lowLuck = Properties.getLowLuckDamageOnly(gameData.getProperties());
        final boolean useBombingBonus =
            Properties.getUseBombingMaxDiceSidesAndBonus(gameData.getProperties());
        if (!lowLuck && !useBombingBonus) {
          // no low luck, and no bonus, so just roll based on the map's dice sides
          final int diceSides = gameData.getDiceSides();
          dice = bridge.getRandom(diceSides, rollCount, attacker, DiceType.BOMBING, annotation);
          return;
        }

        rollDiceComplex(bridge, useBombingBonus, lowLuck, annotation);
      }

      private void rollDiceComplex(
          IDelegateBridge bridge, boolean useBombingBonus, boolean lowLuck, String annotation) {
        int nextDieIndex = 0;
        for (final Unit u : attackingUnits) {
          final int rolls = getSbrRolls(u, attacker);
          if (rolls < 1) {
            continue;
          }

          final UnitAttachment ua = u.getUnitAttachment();
          int maxDice = ua.getBombingMaxDieSides();
          // both could be -1, meaning they were not set. if they were not set, then we use
          // default dice sides for the map, and zero for the bonus.
          if (maxDice < 0 || !useBombingBonus) {
            maxDice = gameData.getDiceSides();
          }
          int bonus = useBombingBonus ? ua.getBombingBonus() : 0;

          // now, regardless of whether they were set or not, we have to apply "low luck" to them,
          // meaning in this case that we reduce the luck by 2/3.
          if (lowLuck && maxDice >= 5) {
            bonus += (maxDice + 1) / 3;
            maxDice = (maxDice + 1) / 3;
          }

          // now we roll, or don't if there is nothing to roll.
          rollDie(bridge, annotation, maxDice, rolls, nextDieIndex++, bonus);
        }
      }

      private void rollDie(
          IDelegateBridge bridge,
          String annotation,
          int maxDice,
          int rolls,
          int dieIndex,
          int bonus) {
        if (maxDice > 0) {
          final int[] diceRolls =
              bridge.getRandom(maxDice, rolls, attacker, DiceType.BOMBING, annotation);
          for (final int die : diceRolls) {
            // min value is -1 as we add 1 when setting damage
            dice[dieIndex] = Math.max(-1, die + bonus);
          }
        } else {
          for (int i = 0; i < rolls; i++) {
            // min value is -1 as we add 1 when setting damage
            dice[dieIndex] = Math.max(-1, bonus);
          }
        }
      }

      private void addToTargetDiceMap(
          final Unit attackerUnit, final Die roll, final Map<Unit, List<Die>> targetToDiceMap) {
        if (targetsForAiFire.isEmpty()) {
          return;
        }
        final Unit target = getTarget(attackerUnit);
        targetToDiceMap.computeIfAbsent(target, unit -> new ArrayList<>()).add(roll);
      }

      private void findCost(final IDelegateBridge bridge) {
        // if no planes left after aa fires, this is possible
        if (attackingUnits.isEmpty()) {
          return;
        }
        int damageLimit = TerritoryAttachment.getProduction(battleSite);
        int cost = 0;
        final boolean lhtrBombers = Properties.getLhtrHeavyBombers(gameData.getProperties());
        int index = 0;
        final boolean limitDamage =
            Properties.getWW2V2(gameData.getProperties())
                || Properties.getLimitRocketAndSbrDamageToProduction(gameData.getProperties());
        final List<Die> bombingDice = new ArrayList<>();
        final Map<Unit, List<Die>> targetToDiceMap = new HashMap<>();
        // limit to maxDamage
        for (final Unit attacker : attackingUnits) {
          final UnitAttachment ua = attacker.getUnitAttachment();
          final int rolls = getSbrRolls(attacker, StrategicBombingRaidBattle.this.attacker);
          int costThisUnit = 0;
          if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
            // LHTR means we select the best Dice roll for the unit
            int max = 0;
            int maxIndex = index;
            int startIndex = index;
            for (int i = 0; i < rolls; i++) {
              // +1 since 0 based
              if (this.dice[index] + 1 > max) {
                max = this.dice[index] + 1;
                maxIndex = index;
              }
              index++;
            }
            costThisUnit = max;
            // for show
            final Die best = new Die(this.dice[maxIndex]);
            bombingDice.add(best);
            addToTargetDiceMap(attacker, best, targetToDiceMap);
            for (int i = 0; i < rolls; i++) {
              if (startIndex != maxIndex) {
                final Die notBest = new Die(this.dice[startIndex], -1, DieType.IGNORED);
                bombingDice.add(notBest);
                addToTargetDiceMap(attacker, notBest, targetToDiceMap);
              }
              startIndex++;
            }
          } else {
            for (int i = 0; i < rolls; i++) {
              costThisUnit += this.dice[index] + 1;
              final Die die = new Die(this.dice[index]);
              bombingDice.add(die);
              addToTargetDiceMap(attacker, die, targetToDiceMap);
              index++;
            }
          }

          final int bonus =
              gameData.getTechTracker().getBombingBonus(attacker.getOwner(), attacker.getType());
          costThisUnit = Math.max(0, (costThisUnit + bonus));
          if (limitDamage) {
            costThisUnit = Math.min(costThisUnit, damageLimit);
          }
          cost += costThisUnit;
          if (!targetsForAiFire.isEmpty()) {
            bombingRaidDamage.add(getTarget(attacker), costThisUnit);
          }
        }
        // Limit PUs lost if we would like to cap PUs lost at territory value
        if (Properties.getPuCap(gameData.getProperties())
            || Properties.getLimitSbrDamagePerTurn(gameData.getProperties())) {
          final int alreadyLost = gameData.getMoveDelegate().pusAlreadyLost(battleSite);
          final int limit = Math.max(0, damageLimit - alreadyLost);
          cost = Math.min(cost, limit);
          if (!targetsForAiFire.isEmpty()) {
            for (final Unit u : bombingRaidDamage.keySet()) {
              if (bombingRaidDamage.getInt(u) > limit) {
                bombingRaidDamage.put(u, limit);
              }
            }
          }
        }
        // If we damage units instead of territories
        if (Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(
            gameData.getProperties())) {
          // at this point, bombingRaidDamage should contain all units that targets contains
          if (!targetsForAiFire.keySet().containsAll(bombingRaidDamage.keySet())) {
            throw new IllegalStateException("targets should contain all damaged units");
          }
          for (final Unit current : bombingRaidDamage.keySet()) {
            int currentUnitCost = bombingRaidDamage.getInt(current);
            // determine the max allowed damage
            damageLimit = current.getHowMuchMoreDamageCanThisUnitTake(battleSite);
            if (bombingRaidDamage.getInt(current) > damageLimit) {
              bombingRaidDamage.put(current, damageLimit);
              cost = (cost - currentUnitCost) + damageLimit;
              currentUnitCost = bombingRaidDamage.getInt(current);
            }
            final int totalDamage = current.getUnitDamage() + currentUnitCost;
            // display the results
            if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
              bridge.sendMessage(
                  new IDisplay.BombingResultsMessage(battleId, bombingDice, currentUnitCost));
            } else {
              bridge
                  .getDisplayChannelBroadcaster()
                  .bombingResults(battleId, bombingDice, currentUnitCost);
            }

            if (currentUnitCost > 0) {
              bridge
                  .getSoundChannelBroadcaster()
                  .playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, attacker);
            }
            // Record production lost
            gameData.getMoveDelegate().pusLost(battleSite, currentUnitCost);
            // apply the hits to the targets
            final IntegerMap<Unit> damageMap = new IntegerMap<>();
            damageMap.put(current, totalDamage);
            bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap, List.of(battleSite)));
            bridge
                .getHistoryWriter()
                .addChildToEvent(
                    MessageFormat.format(
                        "Bombing raid in {0} rolls: {1} and causes: {2} damage to unit: {3}",
                        battleSite.getName(),
                        MyFormatter.asDice(targetToDiceMap.get(current)),
                        currentUnitCost,
                        current.getType().getName()));
            getRemote(bridge)
                .reportMessage(
                    MessageFormat.format(
                        "Bombing raid in {0} rolls: {1} and causes: {2} damage to unit: {3}",
                        battleSite.getName(),
                        MyFormatter.asDice(targetToDiceMap.get(current)),
                        currentUnitCost,
                        current.getType().getName()),
                    MessageFormat.format(
                        "Bombing raid causes {0} damage to {1}",
                        currentUnitCost, current.getType().getName()));
          }
        } else {
          // Record PUs lost
          gameData.getMoveDelegate().pusLost(battleSite, cost);
          cost *= Properties.getPuMultiplier(gameData.getProperties());
          if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
            bridge.sendMessage(new IDisplay.BombingResultsMessage(battleId, bombingDice, cost));
          } else {
            bridge.getDisplayChannelBroadcaster().bombingResults(battleId, bombingDice, cost);
          }
          if (cost > 0) {
            bridge
                .getSoundChannelBroadcaster()
                .playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, attacker);
          }
          // get resources
          final Resource pus = gameData.getResourceList().getResourceOrThrow(Constants.PUS);
          final int have = defender.getResources().getQuantity(pus);
          final int toRemove = Math.min(cost, have);
          final Change change = ChangeFactory.changeResourcesChange(defender, pus, -toRemove);
          bridge.addChange(change);
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  MessageFormat.format(
                      "Bombing raid in {0} rolls: {1} and costs: {2} {3}.",
                      battleSite.getName(),
                      MyFormatter.asDice(this.dice),
                      cost,
                      MyFormatter.pluralize("PU", cost)));
        }
        bombingRaidTotal = cost;
      }
    };
  }

  class FireAa implements IExecutable {
    private static final long serialVersionUID = -4667856856747597406L;
    final Collection<Unit> casualtiesSoFar = new ArrayList<>();
    final boolean determineAttackers;
    DiceRoll dice;
    CasualtyDetails casualties;
    Collection<Unit> validAttackingUnitsForThisRoll;

    FireAa(final Collection<Unit> attackers) {
      validAttackingUnitsForThisRoll = attackers;
      determineAttackers = false;
    }

    FireAa() {
      validAttackingUnitsForThisRoll = List.of();
      determineAttackers = true;
    }

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      final boolean isEditMode = EditDelegate.getEditMode(bridge.getData().getProperties());
      for (final String currentTypeAa : aaTypes) {
        final Collection<Unit> currentPossibleAa =
            CollectionUtils.getMatches(defendingAa, Matches.unitIsAaOfTypeAa(currentTypeAa));
        prepareValidAttackingUnitsForThisRoll(currentTypeAa, currentPossibleAa);

        final IExecutable roll =
            new IExecutable() {
              private static final long serialVersionUID = 379538344036513009L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                validAttackingUnitsForThisRoll.removeAll(casualtiesSoFar);
                if (!validAttackingUnitsForThisRoll.isEmpty()) {
                  // SBR AA currently doesn't take into account support so don't pass in
                  // the enemyUnits or friendlyUnits
                  dice =
                      RollDiceFactory.rollAaDice(
                          validAttackingUnitsForThisRoll,
                          currentPossibleAa,
                          bridge,
                          battleSite,
                          CombatValueBuilder.aaCombatValue()
                              .enemyUnits(List.of())
                              .friendlyUnits(List.of())
                              .side(BattleState.Side.DEFENSE)
                              .supportAttachments(
                                  bridge.getData().getUnitTypeList().getSupportAaRules())
                              .build());
                  final var sound = bridge.getSoundChannelBroadcaster();
                  if (currentTypeAa.equals("AA")) {
                    sound.playSoundForAll(
                        dice.getHits() > 0
                            ? SoundPath.CLIP_BATTLE_AA_HIT
                            : SoundPath.CLIP_BATTLE_AA_MISS,
                        defender);
                  } else {
                    String prefix =
                        SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase(Locale.ROOT);
                    sound.playSoundForAll(
                        prefix
                            + (dice.getHits() > 0
                                ? SoundPath.CLIP_BATTLE_X_HIT
                                : SoundPath.CLIP_BATTLE_X_MISS),
                        defender);
                  }
                }
              }
            };
        final IExecutable calculateCasualties =
            new IExecutable() {
              private static final long serialVersionUID = -4658133491636765763L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                if (!validAttackingUnitsForThisRoll.isEmpty()) {
                  final CasualtyDetails details =
                      calculateCasualties(
                          validAttackingUnitsForThisRoll,
                          currentPossibleAa,
                          bridge,
                          dice,
                          currentTypeAa);
                  markDamaged(details.getDamaged(), bridge);
                  casualties = details;
                  casualtiesSoFar.addAll(details.getKilled());
                }
              }
            };
        final IExecutable notifyCasualties =
            new IExecutable() {
              private static final long serialVersionUID = -4989154196975570919L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                if (!validAttackingUnitsForThisRoll.isEmpty()) {
                  notifyAaHits(bridge, dice, casualties, currentTypeAa);
                }
              }
            };
        final IExecutable removeHits =
            new IExecutable() {
              private static final long serialVersionUID = -3673833177336068509L;

              @Override
              public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
                if (!validAttackingUnitsForThisRoll.isEmpty()) {
                  removeAaHits(bridge, casualties, currentTypeAa);
                }
              }
            };
        // push in reverse order of execution
        stack.push(removeHits);
        stack.push(notifyCasualties);
        stack.push(calculateCasualties);
        if (!isEditMode) {
          stack.push(roll);
        }
      }
    }

    private void prepareValidAttackingUnitsForThisRoll(
        String currentTypeAa, Collection<Unit> currentPossibleAa) {
      final Set<UnitType> targetUnitTypesForThisTypeAa =
          CollectionUtils.getAny(currentPossibleAa)
              .getUnitAttachment()
              .getTargetsAa(gameData.getUnitTypeList());

      final Set<UnitType> airborneTypesTargetedToo =
          TechAbilityAttachment.getAirborneTargettedByAa(
                  TechTracker.getCurrentTechAdvances(attacker, gameData.getTechnologyFrontier()))
              .get(currentTypeAa);
      if (determineAttackers) {
        validAttackingUnitsForThisRoll =
            CollectionUtils.getMatches(
                attackingUnits,
                Matches.unitIsOfTypes(targetUnitTypesForThisTypeAa)
                    .or(
                        Matches.unitIsAirborne()
                            .and(Matches.unitIsOfTypes(airborneTypesTargetedToo))));
      }
    }
  }
}
