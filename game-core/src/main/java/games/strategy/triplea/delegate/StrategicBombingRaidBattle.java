package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.delegate.data.BattleRecord;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;

public class StrategicBombingRaidBattle extends AbstractBattle implements BattleStepStrings {
  private static final long serialVersionUID = 8490171037606078890L;
  private static final String RAID = "Strategic bombing raid";
  // these would be the factories or other targets. does not include aa.
  private final Map<Unit, Set<Unit>> m_targets = new HashMap<>();
  private final ExecutionStack m_stack = new ExecutionStack();
  private List<String> m_steps;
  private List<Unit> m_defendingAA;
  private List<String> m_AAtypes;
  private int m_bombingRaidTotal;
  private final IntegerMap<Unit> m_bombingRaidDamage = new IntegerMap<>();

  /**
   * Creates new StrategicBombingRaidBattle.
   *
   * @param battleSite - battle territory
   * @param data - game data
   * @param attacker - attacker PlayerID
   * @param battleTracker - BattleTracker
   */
  public StrategicBombingRaidBattle(final Territory battleSite, final GameData data, final PlayerID attacker,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, true, BattleType.BOMBING_RAID, data);
    isAmphibious = false;
    updateDefendingUnits();
  }

  @Override
  protected void removeUnitsThatNoLongerExist() {
    if (headless) {
      return;
    }
    // we were having a problem with units that had been killed previously were still part of battle's variables, so we
    // double check that
    // the stuff still exists here.
    defendingUnits.retainAll(battleSite.getUnits().getUnits());
    attackingUnits.retainAll(battleSite.getUnits().getUnits());
    m_targets.keySet().removeIf(unit -> !battleSite.getUnits().getUnits().contains(unit));
  }

  protected void updateDefendingUnits() {
    // fill in defenders
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(attacker, gameData);
    final Predicate<Unit> defenders = Matches.enemyUnit(attacker, gameData)
        .and(Matches.unitCanBeDamaged()
            .or(Matches.unitIsAaThatCanFire(attackingUnits, airborneTechTargetsAllowed, attacker,
                Matches.unitIsAaForBombingThisUnitOnly(), round, true, gameData)));
    if (m_targets.isEmpty()) {
      defendingUnits = CollectionUtils.getMatches(battleSite.getUnits().getUnits(), defenders);
    } else {
      final List<Unit> targets =
          CollectionUtils.getMatches(battleSite.getUnits().getUnits(), Matches.unitIsAaThatCanFire(attackingUnits,
              airborneTechTargetsAllowed, attacker, Matches.unitIsAaForBombingThisUnitOnly(), round, true, gameData));
      targets.addAll(m_targets.keySet());
      defendingUnits = targets;
    }
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty();
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    removeAttackers(units, true);
  }

  private void removeAttackers(final Collection<Unit> units, final boolean removeTarget) {
    attackingUnits.removeAll(units);
    final Iterator<Unit> targetIter = m_targets.keySet().iterator();
    while (targetIter.hasNext()) {
      final Set<Unit> currentAttackers = m_targets.get(targetIter.next());
      currentAttackers.removeAll(units);
      if (currentAttackers.isEmpty() && removeTarget) {
        targetIter.remove();
      }
    }
  }

  private Unit getTarget(final Unit attacker) {
    return m_targets.entrySet().stream()
        .filter(e -> e.getValue().contains(attacker))
        .map(Entry::getKey)
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Unit " + attacker.getType().getName() + " has no target"));
  }

  @Override
  public Change addAttackChange(final Route route, final Collection<Unit> units,
      final Map<Unit, Set<Unit>> targets) {
    attackingUnits.addAll(units);
    if (targets == null) {
      return ChangeFactory.EMPTY_CHANGE;
    }
    for (final Unit target : targets.keySet()) {
      final Set<Unit> currentAttackers = m_targets.computeIfAbsent(target, i -> new HashSet<>());
      currentAttackers.addAll(targets.get(target));
    }
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    // remove units that may already be dead due to a previous event (like they died from a strategic bombing raid,
    // rocket attack, etc)
    removeUnitsThatNoLongerExist();
    // we were interrupted
    if (m_stack.isExecuting()) {
      showBattle(bridge);
      m_stack.execute(bridge);
      return;
    }
    // We update Defending Units twice: first time when the battle is created, and second time before the battle begins.
    // The reason is because when the battle is created, there are no attacking units yet in it, meaning that m_targets
    // is empty. We need to
    // update right as battle begins to know we have the full list of targets.
    updateDefendingUnits();
    bridge.getHistoryWriter().startEvent("Strategic bombing raid in " + battleSite, battleSite);
    if (attackingUnits.isEmpty() || (defendingUnits.isEmpty()
        || defendingUnits.stream().noneMatch(Matches.unitCanBeDamaged()))) {
      endBeforeRolling(bridge);
      return;
    }
    BattleCalculator.sortPreBattle(attackingUnits);
    // TODO: determine if the target has the property, not just any unit with the property isAAforBombingThisUnitOnly
    final Map<String, Set<UnitType>> airborneTechTargetsAllowed =
        TechAbilityAttachment.getAirborneTargettedByAa(attacker, gameData);
    m_defendingAA = battleSite.getUnits().getMatches(Matches.unitIsAaThatCanFire(attackingUnits,
        airborneTechTargetsAllowed, attacker, Matches.unitIsAaForBombingThisUnitOnly(), round, true, gameData));
    m_AAtypes = UnitAttachment.getAllOfTypeAas(m_defendingAA);
    // reverse since stacks are in reverse order
    Collections.reverse(m_AAtypes);
    final boolean hasAa = m_defendingAA.size() > 0;
    m_steps = new ArrayList<>();
    if (hasAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(m_defendingAA)) {
        m_steps.add(typeAa + AA_GUNS_FIRE_SUFFIX);
        m_steps.add(SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        m_steps.add(REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    m_steps.add(RAID);
    showBattle(bridge);
    final List<IExecutable> steps = new ArrayList<>();
    if (hasAa) {
      // global1940 rules - each target type fires an AA shot against the planes bombing it
      steps.addAll(m_targets.entrySet().stream()
          .filter(entry -> entry.getKey().getUnitAttachment().getIsAaForBombingThisUnitOnly())
          .map(Entry::getValue)
          .map(FireAA::new)
          .collect(Collectors.toList()));

      // otherwise fire an AA shot at all the planes
      if (steps.isEmpty()) {
        steps.add(new FireAA());
      }
    }
    steps.add(new ConductBombing());
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 4299575008166316488L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        getDisplay(bridge).gotoBattleStep(battleId, RAID);
        if (isDamageFromBombingDoneToUnitsInsteadOfTerritories()) {
          bridge.getHistoryWriter()
              .addChildToEvent("Bombing raid in " + battleSite.getName() + " causes " + m_bombingRaidTotal
                  + " damage total. " + (m_bombingRaidDamage.size() > 1 ? (" Damaged units is as follows: "
                      + MyFormatter.integerUnitMapToString(m_bombingRaidDamage, ", ", " = ", false)) : ""));
        } else {
          bridge.getHistoryWriter().addChildToEvent(
              "Bombing raid costs " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
        }
        // TODO remove the reference to the constant.japanese- replace with a rule
        if (isPacificTheater() || isSbrVictoryPoints()) {
          if (defender.getName().equals(Constants.PLAYER_NAME_JAPANESE)) {
            final PlayerAttachment pa = PlayerAttachment.get(defender);
            if (pa != null) {
              final Change changeVp =
                  ChangeFactory.attachmentPropertyChange(pa, ((-(m_bombingRaidTotal / 10)) + pa.getVps()), "vps");
              bridge.addChange(changeVp);
              bridge.getHistoryWriter().addChildToEvent("Bombing raid costs " + (m_bombingRaidTotal / 10) + " "
                  + MyFormatter.pluralize("vp", (m_bombingRaidTotal / 10)));
            }
          }
        }
        // kill any suicide attackers (veqryn)
        if (attackingUnits.stream().anyMatch(Matches.unitIsSuicide())) {
          final List<Unit> suicideUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsSuicide());
          attackingUnits.removeAll(suicideUnits);
          final Change removeSuicide = ChangeFactory.removeUnits(battleSite, suicideUnits);
          final String transcriptText = MyFormatter.unitsToText(suicideUnits) + " lost in " + battleSite.getName();
          final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(attacker, gameData);
          final int tuvLostAttacker = TuvUtils.getTuv(suicideUnits, attacker, costs, gameData);
          attackerLostTuv += tuvLostAttacker;
          bridge.getHistoryWriter().addChildToEvent(transcriptText, suicideUnits);
          bridge.addChange(removeSuicide);
        }
        // kill any units that can die if they have reached max damage (veqryn)
        if (m_targets.keySet().stream().anyMatch(Matches.unitCanDieFromReachingMaxDamage())) {
          final List<Unit> unitsCanDie =
              CollectionUtils.getMatches(m_targets.keySet(), Matches.unitCanDieFromReachingMaxDamage());
          unitsCanDie.retainAll(
              CollectionUtils.getMatches(unitsCanDie, Matches.unitIsAtMaxDamageOrNotCanBeDamaged(battleSite)));
          if (!unitsCanDie.isEmpty()) {
            // m_targets.removeAll(unitsCanDie);
            final Change removeDead = ChangeFactory.removeUnits(battleSite, unitsCanDie);
            final String transcriptText = MyFormatter.unitsToText(unitsCanDie) + " lost in " + battleSite.getName();
            final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(defender, gameData);
            final int tuvLostDefender = TuvUtils.getTuv(unitsCanDie, defender, costs, gameData);
            defenderLostTuv += tuvLostDefender;
            bridge.getHistoryWriter().addChildToEvent(transcriptText, unitsCanDie);
            bridge.addChange(removeDead);
          }
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -7649516174883172328L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        end(bridge);
      }
    });
    Collections.reverse(steps);
    for (final IExecutable executable : steps) {
      m_stack.push(executable);
    }
    m_stack.execute(bridge);
  }

  private void endBeforeRolling(final IDelegateBridge bridge) {
    getDisplay(bridge).battleEnd(battleId, "Bombing raid does no damage");
    whoWon = WhoWon.DRAW;
    battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
    battleTracker.getBattleRecords().addResultToBattle(attacker, battleId, defender, attackerLostTuv,
        defenderLostTuv, battleResultDescription, new BattleResults(this, gameData));
    isOver = true;
    battleTracker.removeBattle(StrategicBombingRaidBattle.this, gameData);
  }

  private void end(final IDelegateBridge bridge) {
    if (isDamageFromBombingDoneToUnitsInsteadOfTerritories()) {
      getDisplay(bridge).battleEnd(battleId,
          "Raid causes " + m_bombingRaidTotal + " damage total."
              + (m_bombingRaidDamage.size() > 1
                  ? (" To units: " + MyFormatter.integerUnitMapToString(m_bombingRaidDamage, ", ", " = ", false))
                  : ""));
    } else {
      getDisplay(bridge).battleEnd(battleId,
          "Bombing raid cost " + m_bombingRaidTotal + " " + MyFormatter.pluralize("PU", m_bombingRaidTotal));
    }
    if (m_bombingRaidTotal > 0) {
      whoWon = WhoWon.ATTACKER;
      battleResultDescription = BattleRecord.BattleResultDescription.BOMBED;
    } else {
      whoWon = WhoWon.DEFENDER;
      battleResultDescription = BattleRecord.BattleResultDescription.LOST;
    }
    battleTracker.getBattleRecords().addResultToBattle(attacker, battleId, defender, attackerLostTuv,
        defenderLostTuv, battleResultDescription, new BattleResults(this, gameData));
    isOver = true;
    battleTracker.removeBattle(this, gameData);
  }

  private void showBattle(final IDelegateBridge bridge) {
    final String title = "Bombing raid in " + battleSite.getName();
    getDisplay(bridge).showBattle(battleId, battleSite, title, attackingUnits, defendingUnits, null, null, null,
        Collections.emptyMap(), attacker, defender, isAmphibious(), getBattleType(),
        Collections.emptySet());
    getDisplay(bridge).listBattleSteps(battleId, m_steps);
  }

  class FireAA implements IExecutable {
    private static final long serialVersionUID = -4667856856747597406L;
    DiceRoll m_dice;
    CasualtyDetails m_casualties;
    final Collection<Unit> m_casualtiesSoFar = new ArrayList<>();
    Collection<Unit> validAttackingUnitsForThisRoll;
    final boolean determineAttackers;

    public FireAA(final Collection<Unit> attackers) {
      validAttackingUnitsForThisRoll = attackers;
      determineAttackers = false;
    }

    public FireAA() {
      validAttackingUnitsForThisRoll = Collections.emptyList();
      determineAttackers = true;
    }

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      final boolean isEditMode = BaseEditDelegate.getEditMode(bridge.getData());
      for (final String currentTypeAa : m_AAtypes) {
        final Collection<Unit> currentPossibleAa =
            CollectionUtils.getMatches(m_defendingAA, Matches.unitIsAaOfTypeAa(currentTypeAa));
        final Set<UnitType> targetUnitTypesForThisTypeAa =
            UnitAttachment.get(currentPossibleAa.iterator().next().getType()).getTargetsAa(gameData);
        final Set<UnitType> airborneTypesTargettedToo =
            TechAbilityAttachment.getAirborneTargettedByAa(attacker, gameData).get(currentTypeAa);
        if (determineAttackers) {
          validAttackingUnitsForThisRoll = CollectionUtils.getMatches(attackingUnits,
              Matches.unitIsOfTypes(targetUnitTypesForThisTypeAa)
                  .or(Matches.unitIsAirborne().and(Matches.unitIsOfTypes(airborneTypesTargettedToo))));
        }

        final IExecutable roll = new IExecutable() {
          private static final long serialVersionUID = 379538344036513009L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            validAttackingUnitsForThisRoll.removeAll(m_casualtiesSoFar);
            if (!validAttackingUnitsForThisRoll.isEmpty()) {
              m_dice = DiceRoll.rollAa(validAttackingUnitsForThisRoll, currentPossibleAa, bridge, battleSite, true);
              if (currentTypeAa.equals("AA")) {
                if (m_dice.getHits() > 0) {
                  bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_HIT, defender);
                } else {
                  bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AA_MISS, defender);
                }
              } else {
                if (m_dice.getHits() > 0) {
                  bridge.getSoundChannelBroadcaster().playSoundForAll(
                      SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_HIT,
                      defender);
                } else {
                  bridge.getSoundChannelBroadcaster().playSoundForAll(
                      SoundPath.CLIP_BATTLE_X_PREFIX + currentTypeAa.toLowerCase() + SoundPath.CLIP_BATTLE_X_MISS,
                      defender);
                }
              }
            }
          }
        };
        final IExecutable calculateCasualties = new IExecutable() {
          private static final long serialVersionUID = -4658133491636765763L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!validAttackingUnitsForThisRoll.isEmpty()) {
              final CasualtyDetails details =
                  calculateCasualties(validAttackingUnitsForThisRoll, currentPossibleAa, bridge, m_dice, currentTypeAa);
              markDamaged(details.getDamaged(), bridge);
              m_casualties = details;
              m_casualtiesSoFar.addAll(details.getKilled());
            }
          }
        };
        final IExecutable notifyCasualties = new IExecutable() {
          private static final long serialVersionUID = -4989154196975570919L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!validAttackingUnitsForThisRoll.isEmpty()) {
              notifyAaHits(bridge, m_dice, m_casualties, currentTypeAa);
            }
          }
        };
        final IExecutable removeHits = new IExecutable() {
          private static final long serialVersionUID = -3673833177336068509L;

          @Override
          public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
            if (!validAttackingUnitsForThisRoll.isEmpty()) {
              removeAaHits(bridge, m_casualties, currentTypeAa);
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
  }

  private boolean isDamageFromBombingDoneToUnitsInsteadOfTerritories() {
    return Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(gameData);
  }

  private boolean isWW2V2() {
    return Properties.getWW2V2(gameData);
  }

  private boolean isLimitSbrDamageToProduction() {
    return Properties.getLimitRocketAndSbrDamageToProduction(gameData);
  }

  private static boolean isLimitSbrDamagePerTurn(final GameData data) {
    return Properties.getLimitSbrDamagePerTurn(data);
  }

  private static boolean isPuCap(final GameData data) {
    return Properties.getPuCap(data);
  }

  private boolean isSbrVictoryPoints() {
    return Properties.getSbrVictoryPoints(gameData);
  }

  private boolean isPacificTheater() {
    return Properties.getPacificTheater(gameData);
  }

  private CasualtyDetails calculateCasualties(final Collection<Unit> validAttackingUnitsForThisRoll,
      final Collection<Unit> defendingAa, final IDelegateBridge bridge, final DiceRoll dice,
      final String currentTypeAa) {
    getDisplay(bridge).notifyDice(dice, SELECT_PREFIX + currentTypeAa + CASUALTIES_SUFFIX);
    final boolean isEditMode = BaseEditDelegate.getEditMode(gameData);
    final boolean allowMultipleHitsPerUnit =
        !defendingAa.isEmpty()
            && defendingAa.stream().allMatch(Matches.unitAaShotDamageableInsteadOfKillingInstantly());
    if (isEditMode) {
      final String text = currentTypeAa + AA_GUNS_FIRE_SUFFIX;
      return BattleCalculator.selectCasualties(RAID, attacker,
          validAttackingUnitsForThisRoll, attackingUnits, defender, defendingUnits, isAmphibious,
          amphibiousLandAttackers, battleSite, territoryEffects, bridge, text, /* dice */null,
          /* defending */false, battleId, /* head-less */false, 0, allowMultipleHitsPerUnit);
    }
    final CasualtyDetails casualties = BattleCalculator.getAaCasualties(false, validAttackingUnitsForThisRoll,
        attackingUnits, defendingAa, defendingUnits, dice, bridge, defender, attacker, battleId, battleSite,
        territoryEffects, isAmphibious, amphibiousLandAttackers);
    final int totalExpectingHits =
        dice.getHits() > validAttackingUnitsForThisRoll.size() ? validAttackingUnitsForThisRoll.size() : dice.getHits();
    if (casualties.size() != totalExpectingHits) {
      throw new IllegalStateException(
          "Wrong number of casualties, expecting:" + totalExpectingHits + " but got:" + casualties.size());
    }
    return casualties;
  }

  private void notifyAaHits(final IDelegateBridge bridge, final DiceRoll dice, final CasualtyDetails casualties,
      final String currentTypeAa) {
    getDisplay(bridge).casualtyNotification(battleId, REMOVE_PREFIX + currentTypeAa + CASUALTIES_SUFFIX, dice,
        attacker, new ArrayList<>(casualties.getKilled()), new ArrayList<>(casualties.getDamaged()),
        Collections.emptyMap());
    final Thread t = new Thread(() -> {
      try {
        final ITripleAPlayer defender = (ITripleAPlayer) bridge.getRemotePlayer(this.defender);
        defender.confirmEnemyCasualties(battleId, "Press space to continue", attacker);
      } catch (final Exception e) {
        // ignore
      }
    }, "click to continue waiter");
    t.start();
    final ITripleAPlayer attacker = (ITripleAPlayer) bridge.getRemotePlayer(this.attacker);
    attacker.confirmOwnCasualties(battleId, "Press space to continue");
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  private void removeAaHits(final IDelegateBridge bridge, final CasualtyDetails casualties,
      final String currentTypeAa) {
    final List<Unit> killed = casualties.getKilled();
    if (!killed.isEmpty()) {
      bridge.getHistoryWriter().addChildToEvent(MyFormatter.unitsToTextNoOwner(killed) + " killed by " + currentTypeAa,
          new ArrayList<>(killed));
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(attacker, gameData);
      final int tuvLostAttacker = TuvUtils.getTuv(killed, attacker, costs, gameData);
      attackerLostTuv += tuvLostAttacker;
      // attackingUnits.removeAll(casualties);
      removeAttackers(killed, false);
      final Change remove = ChangeFactory.removeUnits(battleSite, killed);
      bridge.addChange(remove);
    }
  }

  class ConductBombing implements IExecutable {
    private static final long serialVersionUID = 5579796391988452213L;
    private int[] m_dice;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      final IExecutable rollDice = new IExecutable() {
        private static final long serialVersionUID = -4097858758514452368L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          rollDice(bridge);
        }
      };
      final IExecutable findCost = new IExecutable() {
        private static final long serialVersionUID = 8573539936364094095L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          findCost(bridge);
        }
      };
      // push in reverse order of execution
      m_stack.push(findCost);
      m_stack.push(rollDice);
    }

    private void rollDice(final IDelegateBridge bridge) {
      final Set<Unit> duplicatesCheckSet1 = new HashSet<>(attackingUnits);
      if (attackingUnits.size() != duplicatesCheckSet1.size()) {
        throw new IllegalStateException(
            "Duplicate Units Detected: Original List:" + attackingUnits + "  HashSet:" + duplicatesCheckSet1);
      }

      final int rollCount = BattleCalculator.getRolls(attackingUnits, attacker, false, true, territoryEffects);
      if (rollCount == 0) {
        m_dice = null;
        return;
      }
      m_dice = new int[rollCount];
      final boolean isEditMode = BaseEditDelegate.getEditMode(gameData);
      if (isEditMode) {
        final String annotation =
            attacker.getName() + " fixing dice to allocate cost of strategic bombing raid against "
                + defender.getName() + " in " + battleSite.getName();
        final ITripleAPlayer attacker =
            (ITripleAPlayer) bridge.getRemotePlayer(StrategicBombingRaidBattle.this.attacker);
        // does not take into account bombers with dice sides higher than getDiceSides
        m_dice = attacker.selectFixedDice(rollCount, 0, annotation, gameData.getDiceSides());
      } else {
        final boolean doNotUseBombingBonus =
            !Properties.getUseBombingMaxDiceSidesAndBonus(gameData);
        final String annotation = attacker.getName() + " rolling to allocate cost of strategic bombing raid against "
            + defender.getName() + " in " + battleSite.getName();
        if (!Properties.getLowLuckDamageOnly(gameData)) {
          if (doNotUseBombingBonus) {
            // no low luck, and no bonus, so just roll based on the map's dice sides
            m_dice = bridge.getRandom(gameData.getDiceSides(), rollCount, attacker, DiceType.BOMBING, annotation);
          } else {
            // we must use bombing bonus
            int i = 0;
            final int diceSides = gameData.getDiceSides();
            for (final Unit u : attackingUnits) {
              final int rolls = BattleCalculator.getRolls(u, attacker, false, true, territoryEffects);
              if (rolls < 1) {
                continue;
              }
              final UnitAttachment ua = UnitAttachment.get(u.getType());
              int maxDice = ua.getBombingMaxDieSides();
              final int bonus = ua.getBombingBonus();
              // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for
              // the map, and zero for the bonus.
              if (maxDice < 0) {
                maxDice = diceSides;
              }
              // now we roll, or don't if there is nothing to roll.
              if (maxDice > 0) {
                final int[] dicerolls = bridge.getRandom(maxDice, rolls, attacker, DiceType.BOMBING, annotation);
                for (final int die : dicerolls) {
                  // min value is -1 as we add 1 when setting damage since dice are 0 instead of 1 based
                  m_dice[i] = Math.max(-1, die + bonus);
                  i++;
                }
              } else {
                for (int j = 0; j < rolls; j++) {
                  // min value is -1 as we add 1 when setting damage since dice are 0 instead of 1 based
                  m_dice[i] = Math.max(-1, bonus);
                  i++;
                }
              }
            }
          }
        } else {
          int i = 0;
          final int diceSides = gameData.getDiceSides();
          for (final Unit u : attackingUnits) {
            final int rolls = BattleCalculator.getRolls(u, attacker, false, true, territoryEffects);
            if (rolls < 1) {
              continue;
            }
            final UnitAttachment ua = UnitAttachment.get(u.getType());
            int maxDice = ua.getBombingMaxDieSides();
            int bonus = ua.getBombingBonus();
            // both could be -1, meaning they were not set. if they were not set, then we use default dice sides for the
            // map, and zero for
            // the bonus.
            if (maxDice < 0 || doNotUseBombingBonus) {
              maxDice = diceSides;
            }
            if (doNotUseBombingBonus) {
              bonus = 0;
            }
            // now, regardless of whether they were set or not, we have to apply "low luck" to them, meaning in this
            // case that we reduce the
            // luck by 2/3.
            if (maxDice >= 5) {
              bonus += (maxDice + 1) / 3;
              maxDice = (maxDice + 1) / 3;
            }
            // now we roll, or don't if there is nothing to roll.
            if (maxDice > 0) {
              final int[] dicerolls = bridge.getRandom(maxDice, rolls, attacker, DiceType.BOMBING, annotation);
              for (final int die : dicerolls) {
                // min value is -1 as we add 1 when setting damage since dice are 0 instead of 1 based
                m_dice[i] = Math.max(-1, die + bonus);
                i++;
              }
            } else {
              for (int j = 0; j < rolls; j++) {
                // min value is -1 as we add 1 when setting damage since dice are 0 instead of 1 based
                m_dice[i] = Math.max(-1, bonus);
                i++;
              }
            }
          }
        }
      }
    }

    private void addToTargetDiceMap(final Unit attackerUnit, final Die roll,
        final Map<Unit, List<Die>> targetToDiceMap) {
      if (m_targets.isEmpty()) {
        return;
      }
      final Unit target = getTarget(attackerUnit);
      List<Die> current = targetToDiceMap.get(target);
      if (current == null) {
        current = new ArrayList<>();
      }
      current.add(roll);
      targetToDiceMap.put(target, current);
    }

    private void findCost(final IDelegateBridge bridge) {
      // if no planes left after aa fires, this is possible
      if (attackingUnits.isEmpty()) {
        return;
      }
      int damageLimit = TerritoryAttachment.getProduction(battleSite);
      int cost = 0;
      final boolean lhtrBombers = Properties.getLhtrHeavyBombers(gameData);
      int index = 0;
      final boolean limitDamage = isWW2V2() || isLimitSbrDamageToProduction();
      final List<Die> dice = new ArrayList<>();
      final Map<Unit, List<Die>> targetToDiceMap = new HashMap<>();
      // limit to maxDamage
      for (final Unit attacker : attackingUnits) {
        final UnitAttachment ua = UnitAttachment.get(attacker.getType());
        final int rolls = BattleCalculator.getRolls(attacker, StrategicBombingRaidBattle.this.attacker, false, true,
            territoryEffects);
        int costThisUnit = 0;
        if (rolls > 1 && (lhtrBombers || ua.getChooseBestRoll())) {
          // LHTR means we select the best Dice roll for the unit
          int max = 0;
          int maxIndex = index;
          int startIndex = index;
          for (int i = 0; i < rolls; i++) {
            // +1 since 0 based
            if (m_dice[index] + 1 > max) {
              max = m_dice[index] + 1;
              maxIndex = index;
            }
            index++;
          }
          costThisUnit = max;
          // for show
          final Die best = new Die(m_dice[maxIndex]);
          dice.add(best);
          addToTargetDiceMap(attacker, best, targetToDiceMap);
          for (int i = 0; i < rolls; i++) {
            if (startIndex != maxIndex) {
              final Die notBest = new Die(m_dice[startIndex], -1, DieType.IGNORED);
              dice.add(notBest);
              addToTargetDiceMap(attacker, notBest, targetToDiceMap);
            }
            startIndex++;
          }
        } else {
          for (int i = 0; i < rolls; i++) {
            costThisUnit += m_dice[index] + 1;
            final Die die = new Die(m_dice[index]);
            dice.add(die);
            addToTargetDiceMap(attacker, die, targetToDiceMap);
            index++;
          }
        }
        costThisUnit = Math.max(0,
            (costThisUnit + TechAbilityAttachment.getBombingBonus(attacker.getType(), attacker.getOwner(), gameData)));
        if (limitDamage) {
          costThisUnit = Math.min(costThisUnit, damageLimit);
        }
        cost += costThisUnit;
        if (!m_targets.isEmpty()) {
          m_bombingRaidDamage.add(getTarget(attacker), costThisUnit);
        }
      }
      // Limit PUs lost if we would like to cap PUs lost at territory value
      if (isPuCap(gameData) || isLimitSbrDamagePerTurn(gameData)) {
        final int alreadyLost = DelegateFinder.moveDelegate(gameData).pusAlreadyLost(battleSite);
        final int limit = Math.max(0, damageLimit - alreadyLost);
        cost = Math.min(cost, limit);
        if (!m_targets.isEmpty()) {
          for (final Unit u : m_bombingRaidDamage.keySet()) {
            if (m_bombingRaidDamage.getInt(u) > limit) {
              m_bombingRaidDamage.put(u, limit);
            }
          }
        }
      }
      // If we damage units instead of territories
      if (isDamageFromBombingDoneToUnitsInsteadOfTerritories()) {
        // at this point, m_bombingRaidDamage should contain all units that m_targets contains
        if (!m_targets.keySet().containsAll(m_bombingRaidDamage.keySet())) {
          throw new IllegalStateException("targets should contain all damaged units");
        }
        for (final Unit current : m_bombingRaidDamage.keySet()) {
          int currentUnitCost = m_bombingRaidDamage.getInt(current);
          // determine the max allowed damage
          // UnitAttachment ua = UnitAttachment.get(current.getType());
          final TripleAUnit taUnit = (TripleAUnit) current;
          damageLimit = taUnit.getHowMuchMoreDamageCanThisUnitTake(current, battleSite);
          if (m_bombingRaidDamage.getInt(current) > damageLimit) {
            m_bombingRaidDamage.put(current, damageLimit);
            cost = (cost - currentUnitCost) + damageLimit;
            currentUnitCost = m_bombingRaidDamage.getInt(current);
          }
          final int totalDamage = taUnit.getUnitDamage() + currentUnitCost;
          // display the results
          getDisplay(bridge).bombingResults(battleId, dice, currentUnitCost);
          if (currentUnitCost > 0) {
            bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, attacker);
          }
          // Record production lost
          DelegateFinder.moveDelegate(gameData).pusLost(battleSite, currentUnitCost);
          // apply the hits to the targets
          final IntegerMap<Unit> damageMap = new IntegerMap<>();
          damageMap.put(current, totalDamage);
          bridge.addChange(ChangeFactory.bombingUnitDamage(damageMap));
          bridge.getHistoryWriter()
              .addChildToEvent("Bombing raid in " + battleSite.getName() + " rolls: "
                  + MyFormatter.asDice(targetToDiceMap.get(current)) + " and causes: " + currentUnitCost
                  + " damage to unit: " + current.getType().getName());
          getRemote(bridge).reportMessage(
              "Bombing raid in " + battleSite.getName() + " rolls: "
                  + MyFormatter.asDice(targetToDiceMap.get(current)) + " and causes: " + currentUnitCost
                  + " damage to unit: " + current.getType().getName(),
              "Bombing raid causes " + currentUnitCost + " damage to " + current.getType().getName());
        }
      } else {
        // Record PUs lost
        DelegateFinder.moveDelegate(gameData).pusLost(battleSite, cost);
        cost *= Properties.getPuMultiplier(gameData);
        getDisplay(bridge).bombingResults(battleId, dice, cost);
        if (cost > 0) {
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BOMBING_STRATEGIC, attacker);
        }
        // get resources
        final Resource pus = gameData.getResourceList().getResource(Constants.PUS);
        final int have = defender.getResources().getQuantity(pus);
        final int toRemove = Math.min(cost, have);
        final Change change = ChangeFactory.changeResourcesChange(defender, pus, -toRemove);
        bridge.addChange(change);
        bridge.getHistoryWriter().addChildToEvent("Bombing raid in " + battleSite.getName() + " rolls: "
            + MyFormatter.asDice(m_dice) + " and costs: " + cost + " " + MyFormatter.pluralize("PU", cost) + ".");
      }
      m_bombingRaidTotal = cost;
    }
  }

  @Override
  public void unitsLostInPrecedingBattle(final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {
    // should never happen
  }
}
