package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;
import games.strategy.util.PredicateBuilder;

public class AirBattle extends AbstractBattle {
  private static final long serialVersionUID = 4686241714027216395L;
  protected static final String AIR_BATTLE = "Air Battle";
  protected static final String INTERCEPTORS_LAUNCH = "Defender Launches Interceptors";
  protected static final String ATTACKERS_FIRE = "Attackers Fire";
  protected static final String DEFENDERS_FIRE = "Defenders Fire";
  protected static final String ATTACKERS_WITHDRAW = "Attackers Withdraw?";
  protected static final String DEFENDERS_WITHDRAW = "Defenders Withdraw?";
  protected final ExecutionStack m_stack = new ExecutionStack();
  protected List<String> m_steps;
  protected final Collection<Unit> m_defendingWaitingToDie = new ArrayList<>();
  protected final Collection<Unit> m_attackingWaitingToDie = new ArrayList<>();
  protected boolean m_intercept = false;
  // -1 would mean forever until one side is eliminated. (default is 1 round)
  protected final int m_maxRounds;

  AirBattle(final Territory battleSite, final boolean bombingRaid, final GameData data, final PlayerID attacker,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, bombingRaid, (bombingRaid ? BattleType.AIR_RAID : BattleType.AIR_BATTLE),
        data);
    m_isAmphibious = false;
    m_maxRounds = Properties.getAirBattleRounds(data);
    updateDefendingUnits();
  }

  protected void updateDefendingUnits() {
    // fill in defenders
    if (m_isBombingRun) {
      m_defendingUnits = m_battleSite.getUnits().getMatches(defendingBombingRaidInterceptors(m_attacker, m_data));
    } else {
      m_defendingUnits = m_battleSite.getUnits().getMatches(defendingGroundSeaBattleInterceptors(m_attacker, m_data));
    }
  }

  @Override
  public Change addAttackChange(final Route route, final Collection<Unit> units,
      final HashMap<Unit, HashSet<Unit>> targets) {
    m_attackingUnits.addAll(units);
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    m_attackingUnits.removeAll(units);
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
    updateDefendingUnits();
    bridge.getHistoryWriter().startEvent("Air Battle in " + m_battleSite, m_battleSite);
    BattleCalculator.sortPreBattle(m_attackingUnits);
    BattleCalculator.sortPreBattle(m_defendingUnits);
    m_steps = determineStepStrings(true);
    showBattle(bridge);
    pushFightLoopOnStack(true);
    m_stack.execute(bridge);
  }

  private void pushFightLoopOnStack(final boolean firstRun) {
    if (m_isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables(firstRun);
    // add in the reverse order we create them
    Collections.reverse(steps);
    for (final IExecutable step : steps) {
      m_stack.push(step);
    }
    return;
  }

  private boolean shouldFightAirBattle() {
    return !m_defendingUnits.isEmpty()
        && (!m_attackingUnits.isEmpty()
            || (m_isBombingRun && m_attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())));
  }

  public boolean shouldEndBattleDueToMaxRounds() {
    return (m_maxRounds > 0) && (m_maxRounds <= m_round);
  }

  protected boolean canAttackerRetreat() {
    return !shouldEndBattleDueToMaxRounds() && shouldFightAirBattle()
        && Properties.getAirBattleAttackersCanRetreat(m_data);
  }

  protected boolean canDefenderRetreat() {
    return !shouldEndBattleDueToMaxRounds() && shouldFightAirBattle()
        && Properties.getAirBattleDefendersCanRetreat(m_data);
  }

  List<IExecutable> getBattleExecutables(final boolean firstRun) {
    final List<IExecutable> steps = new ArrayList<>();
    if (shouldFightAirBattle()) {
      if (firstRun) {
        steps.add(new InterceptorsLaunch());
      }
      steps.add(new AttackersFire());
      steps.add(new DefendersFire());
      steps.add(new IExecutable() { // just calculates lost TUV and kills off any suicide units
        private static final long serialVersionUID = -5575569705493214941L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!m_intercept) {
            return;
          }
          final IntegerMap<UnitType> defenderCosts = TuvUtils.getCostsForTuv(m_defender, m_data);
          final IntegerMap<UnitType> attackerCosts = TuvUtils.getCostsForTuv(m_attacker, m_data);
          m_attackingUnits.removeAll(m_attackingWaitingToDie);
          remove(m_attackingWaitingToDie, bridge, m_battleSite);
          m_defendingUnits.removeAll(m_defendingWaitingToDie);
          remove(m_defendingWaitingToDie, bridge, m_battleSite);
          int tuvLostAttacker = TuvUtils.getTuv(m_attackingWaitingToDie, m_attacker, attackerCosts, m_data);
          m_attackerLostTUV += tuvLostAttacker;
          int tuvLostDefender = TuvUtils.getTuv(m_defendingWaitingToDie, m_defender, defenderCosts, m_data);
          m_defenderLostTUV += tuvLostDefender;
          m_attackingWaitingToDie.clear();
          m_defendingWaitingToDie.clear();
          // kill any suicide attackers (veqryn)
          final Predicate<Unit> attackerSuicide = PredicateBuilder
              .of(Matches.unitIsSuicide())
              .andIf(m_isBombingRun, Matches.unitIsNotStrategicBomber())
              .build();
          if (m_attackingUnits.stream().anyMatch(attackerSuicide)) {
            final List<Unit> suicideUnits = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsSuicide());
            m_attackingUnits.removeAll(suicideUnits);
            remove(suicideUnits, bridge, m_battleSite);
            tuvLostAttacker = TuvUtils.getTuv(suicideUnits, m_attacker, attackerCosts, m_data);
            m_attackerLostTUV += tuvLostAttacker;
          }
          if (m_defendingUnits.stream().anyMatch(Matches.unitIsSuicide())) {
            final List<Unit> suicideUnits = CollectionUtils.getMatches(m_defendingUnits, Matches.unitIsSuicide());
            m_defendingUnits.removeAll(suicideUnits);
            remove(suicideUnits, bridge, m_battleSite);
            tuvLostDefender = TuvUtils.getTuv(suicideUnits, m_defender, defenderCosts, m_data);
            m_defenderLostTUV += tuvLostDefender;
          }
        }
      });
    }
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 3148193405425861565L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
          return;
        }
        makeBattle(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 3148193405425861565L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
          return;
        }
        end(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -5408702756335356985L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver && canAttackerRetreat()) {
          attackerRetreat(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -7819137222487595113L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver && canDefenderRetreat()) {
          defenderRetreat(bridge);
        }
      }
    });
    final IExecutable loop = new IExecutable() {
      private static final long serialVersionUID = -5408702756335356985L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        pushFightLoopOnStack(false);
      }
    };
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -4136481765101946944L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!m_isOver) {
          m_steps = determineStepStrings(false);
          final ITripleADisplay display = getDisplay(bridge);
          display.listBattleSteps(m_battleID, m_steps);
          m_round++;
          // continue fighting
          // the recursive step
          // this should always be the base of the stack
          // when we execute the loop, it will populate the stack with the battle steps
          if (!m_stack.isEmpty()) {
            throw new IllegalStateException("Stack not empty:" + m_stack);
          }
          m_stack.push(loop);
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

  private static void recordUnitsWereInAirBattle(final Collection<Unit> units, final IDelegateBridge bridge) {
    final CompositeChange wasInAirBattleChange = new CompositeChange();
    for (final Unit u : units) {
      wasInAirBattleChange.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_IN_AIR_BATTLE));
    }
    if (!wasInAirBattleChange.isEmpty()) {
      bridge.addChange(wasInAirBattleChange);
    }
  }

  private void makeBattle(final IDelegateBridge bridge) {
    // record who was in this battle first, so that they do not take part in any ground battles
    if (m_isBombingRun) {
      recordUnitsWereInAirBattle(m_attackingUnits, bridge);
      recordUnitsWereInAirBattle(m_defendingUnits, bridge);
    }
    // so as of right now, Air Battles are created before both normal battles and strategic bombing raids
    // once completed, the air battle will create a strategic bombing raid, if that is the purpose of those aircraft
    // however, if the purpose is a normal battle, it will have already been created by the battle tracker / combat move
    // so we do not have to create normal battles, only bombing raids
    // setup new battle here
    if (m_isBombingRun) {
      final Collection<Unit> bombers = CollectionUtils.getMatches(m_attackingUnits, Matches.unitIsStrategicBomber());
      if (!bombers.isEmpty()) {
        HashMap<Unit, HashSet<Unit>> targets = null;
        final Collection<Unit> enemyTargetsTotal = m_battleSite.getUnits().getMatches(
            Matches.enemyUnit(bridge.getPlayerId(), m_data)
                .and(Matches.unitCanBeDamaged())
                .and(Matches.unitIsBeingTransported().negate()));
        for (final Unit unit : bombers) {
          final Collection<Unit> enemyTargets =
              CollectionUtils.getMatches(enemyTargetsTotal, Matches.unitIsLegalBombingTargetBy(unit));
          if (!enemyTargets.isEmpty()) {
            Unit target = null;
            if ((enemyTargets.size() > 1)
                && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(m_data)) {
              while (target == null) {
                target =
                    getRemote(bridge).whatShouldBomberBomb(m_battleSite, enemyTargets, Collections.singletonList(unit));
              }
            } else if (!enemyTargets.isEmpty()) {
              target = enemyTargets.iterator().next();
            }
            if (target != null) {
              targets = new HashMap<>();
              targets.put(target, new HashSet<>(Collections.singleton(unit)));
            }
            m_battleTracker.addBattle(new RouteScripted(m_battleSite), Collections.singleton(unit), true, m_attacker,
                bridge, null, null, targets, true);
          }
        }
        final IBattle battle = m_battleTracker.getPendingBattle(m_battleSite, true, null);
        final IBattle dependent = m_battleTracker.getPendingBattle(m_battleSite, false, BattleType.NORMAL);
        if (dependent != null) {
          m_battleTracker.addDependency(dependent, battle);
        }
        final IBattle dependentAirBattle = m_battleTracker.getPendingBattle(m_battleSite, false, BattleType.AIR_BATTLE);
        if (dependentAirBattle != null) {
          m_battleTracker.addDependency(dependentAirBattle, battle);
        }
      }
    }
  }

  private void end(final IDelegateBridge bridge) {
    // record it
    final String text;
    if (!m_attackingUnits.isEmpty()) {
      if (m_isBombingRun) {
        if (m_attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())) {
          m_whoWon = WhoWon.ATTACKER;
          if (m_defendingUnits.isEmpty()) {
            m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          } else {
            m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITH_ENEMY_LEFT;
          }
          text = "Air Battle is over, the remaining bombers go on to their targets";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker);
        } else {
          m_whoWon = WhoWon.DRAW;
          m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, the bombers have all died";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, m_attacker);
        }
      } else {
        if (m_defendingUnits.isEmpty()) {
          m_whoWon = WhoWon.ATTACKER;
          m_battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          text = "Air Battle is over, the defenders have all died";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, m_attacker);
        } else {
          m_whoWon = WhoWon.DRAW;
          m_battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, neither side is eliminated";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_STALEMATE, m_attacker);
        }
      }
    } else {
      m_whoWon = WhoWon.DEFENDER;
      m_battleResultDescription = BattleRecord.BattleResultDescription.LOST;
      text = "Air Battle is over, the attackers have all died";
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, m_attacker);
    }
    bridge.getHistoryWriter().addChildToEvent(text);
    m_battleTracker.getBattleRecords().addResultToBattle(m_attacker, m_battleID, m_defender, m_attackerLostTUV,
        m_defenderLostTUV, m_battleResultDescription, new BattleResults(this, m_data));
    getDisplay(bridge).battleEnd(m_battleID, "Air Battle over");
    m_isOver = true;
    m_battleTracker.removeBattle(AirBattle.this);
  }

  void finishBattleAndRemoveFromTrackerHeadless(final IDelegateBridge bridge) {
    makeBattle(bridge);
    m_whoWon = WhoWon.ATTACKER;
    m_battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
    m_battleTracker.getBattleRecords().removeBattle(m_attacker, m_battleID);
    m_isOver = true;
    m_battleTracker.removeBattle(AirBattle.this);
  }

  private void attackerRetreat(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should
    // move during non combat to their landing site, or be scrapped if they can't find one.
    final Collection<Territory> possible = new ArrayList<>(2);
    possible.add(m_battleSite);
    // retreat planes
    if (!m_attackingUnits.isEmpty()) {
      queryRetreat(false, bridge, possible);
    }
  }

  private void defenderRetreat(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should
    // move during non combat to their landing site, or be scrapped if they can't find one.
    final Collection<Territory> possible = new ArrayList<>(2);
    possible.add(m_battleSite);
    // retreat planes
    if (!m_defendingUnits.isEmpty()) {
      queryRetreat(true, bridge, possible);
    }
  }

  private void queryRetreat(final boolean defender, final IDelegateBridge bridge,
      final Collection<Territory> availableTerritories) {
    if (availableTerritories.isEmpty()) {
      return;
    }
    final Collection<Unit> units =
        defender ? new ArrayList<>(m_defendingUnits) : new ArrayList<>(m_attackingUnits);
    if (units.isEmpty()) {
      return;
    }
    final PlayerID retreatingPlayer = defender ? m_defender : m_attacker;
    final String text = retreatingPlayer.getName() + " retreat?";
    final String step = defender ? DEFENDERS_WITHDRAW : ATTACKERS_WITHDRAW;
    getDisplay(bridge).gotoBattleStep(m_battleID, step);
    final Territory retreatTo =
        getRemote(retreatingPlayer, bridge).retreatQuery(m_battleID, false, m_battleSite, availableTerritories, text);
    if ((retreatTo != null) && !availableTerritories.contains(retreatTo)) {
      System.err.println("Invalid retreat selection :" + retreatTo + " not in "
          + MyFormatter.defaultNamedToTextList(availableTerritories));
      Thread.dumpStack();
      return;
    }
    if (retreatTo != null) {
      if (!m_headless) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, m_attacker);
      }
      retreat(units, defender, bridge);
      final String messageShort = retreatingPlayer.getName() + " retreats";
      final String messageLong = retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
      getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
    }
  }

  private void retreat(final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge) {
    if (!defender) {
      // we must remove any of these units from the land battle that follows (this comes before we remove them from this
      // battle, because
      // after we remove from this battle we are no longer blocking any battles)
      final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(AirBattle.this);
      removeFromDependents(retreating, bridge, dependentBattles, true);
    }
    final String transcriptText = MyFormatter.unitsToText(retreating) + (defender ? " grounded" : " retreated");
    final Collection<Unit> units = defender ? m_defendingUnits : m_attackingUnits;
    units.removeAll(retreating);
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
    recordUnitsWereInAirBattle(retreating, bridge);
  }

  private void showBattle(final IDelegateBridge bridge) {
    final String title = "Air Battle in " + m_battleSite.getName();
    getDisplay(bridge).showBattle(m_battleID, m_battleSite, title, m_attackingUnits, m_defendingUnits, null, null, null,
        Collections.emptyMap(), m_attacker, m_defender, isAmphibious(), getBattleType(),
        Collections.emptySet());
    getDisplay(bridge).listBattleSteps(m_battleID, m_steps);
  }

  class InterceptorsLaunch implements IExecutable {
    private static final long serialVersionUID = 4300406315014471768L;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      getInterceptors(bridge);
      if (!m_defendingUnits.isEmpty()) {
        m_intercept = true;
        // play a sound
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR, m_attacker);
      }
    }

    private void getInterceptors(final IDelegateBridge bridge) {
      final boolean groundedPlanesRetreated;
      final Collection<Unit> interceptors;
      if (m_isBombingRun) {
        // if bombing run, ask who will intercept
        interceptors = getRemote(m_defender, bridge).selectUnitsQuery(m_battleSite,
            new ArrayList<>(m_defendingUnits), "Select Air to Intercept");
        groundedPlanesRetreated = false;
      } else {
        // if normal battle, we may choose to withdraw some air units (keep them grounded for both Air battle and the
        // subsequent normal
        // battle) instead of launching
        if (Properties.getAirBattleDefendersCanRetreat(m_data)) {
          interceptors = getRemote(m_defender, bridge).selectUnitsQuery(m_battleSite,
              new ArrayList<>(m_defendingUnits), "Select Air to Intercept");
          groundedPlanesRetreated = true;
        } else {
          // if not allowed to withdraw, we must commit all air
          interceptors = new ArrayList<>(m_defendingUnits);
          groundedPlanesRetreated = false;
        }
      }
      if ((interceptors != null) && !m_defendingUnits.containsAll(interceptors)) {
        throw new IllegalStateException("Interceptors choose from outside of available units");
      }
      final Collection<Unit> beingRemoved = new ArrayList<>(m_defendingUnits);
      m_defendingUnits.clear();
      if (interceptors != null) {
        beingRemoved.removeAll(interceptors);
        m_defendingUnits.addAll(interceptors);
      }
      getDisplay(bridge).changedUnitsNotification(m_battleID, m_defender, beingRemoved, null, null);
      if (groundedPlanesRetreated) {
        // this removes them from the subsequent normal battle. (do not use this for bombing battles)
        retreat(beingRemoved, true, bridge);
      }
      if (!m_attackingUnits.isEmpty()) {
        bridge.getHistoryWriter().addChildToEvent(m_attacker.getName() + " attacks with " + m_attackingUnits.size()
            + " units heading to " + m_battleSite.getName(), new ArrayList<>(m_attackingUnits));
      }
      if (!m_defendingUnits.isEmpty()) {
        bridge.getHistoryWriter().addChildToEvent(m_defender.getName() + " launches " + m_defendingUnits.size()
            + " interceptors out of " + m_battleSite.getName(), new ArrayList<>(m_defendingUnits));
      }
    }
  }

  class AttackersFire implements IExecutable {
    private static final long serialVersionUID = -5289634214875797408L;
    DiceRoll m_dice;
    CasualtyDetails m_details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!m_intercept) {
        return;
      }
      final IExecutable roll = new IExecutable() {
        private static final long serialVersionUID = 6579019987019614374L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_dice = DiceRoll.airBattle(m_attackingUnits, false, m_attacker, bridge, "Attackers Fire, ");
        }
      };
      final IExecutable calculateCasualties = new IExecutable() {
        private static final long serialVersionUID = 4556409970663527142L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_details = BattleCalculator.selectCasualties(ATTACKERS_FIRE, m_defender, m_defendingUnits, m_defendingUnits,
              m_attacker, m_attackingUnits, false, new ArrayList<>(), m_battleSite, null, bridge, ATTACKERS_FIRE,
              m_dice, true, m_battleID, false, m_dice.getHits(), true);
          m_defendingWaitingToDie.addAll(m_details.getKilled());
          markDamaged(m_details.getDamaged(), bridge);
        }
      };
      final IExecutable notifyCasualties = new IExecutable() {
        private static final long serialVersionUID = 4224354422817922451L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          notifyCasualties(m_battleID, bridge, ATTACKERS_FIRE, m_dice, m_defender, m_attacker, m_details);
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
    DiceRoll m_dice;
    CasualtyDetails m_details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!m_intercept) {
        return;
      }
      final IExecutable roll = new IExecutable() {
        private static final long serialVersionUID = 5953506121350176595L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          final List<Unit> allEnemyUnitsAliveOrWaitingToDie = new ArrayList<>();
          allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingUnits);
          allEnemyUnitsAliveOrWaitingToDie.addAll(m_attackingWaitingToDie);
          m_dice = DiceRoll.airBattle(m_defendingUnits, true, m_defender, bridge, "Defenders Fire, ");
        }
      };
      final IExecutable calculateCasualties = new IExecutable() {
        private static final long serialVersionUID = 6658309931909306564L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_details = BattleCalculator.selectCasualties(DEFENDERS_FIRE, m_attacker, m_attackingUnits, m_attackingUnits,
              m_defender, m_defendingUnits, false, new ArrayList<>(), m_battleSite, null, bridge, DEFENDERS_FIRE,
              m_dice, false, m_battleID, false, m_dice.getHits(), true);
          m_attackingWaitingToDie.addAll(m_details.getKilled());
          markDamaged(m_details.getDamaged(), bridge);
        }
      };
      final IExecutable notifyCasualties = new IExecutable() {
        private static final long serialVersionUID = 4461950841000674515L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          notifyCasualties(m_battleID, bridge, DEFENDERS_FIRE, m_dice, m_attacker, m_defender, m_details);
        }
      };
      // push in reverse order of execution
      stack.push(notifyCasualties);
      stack.push(calculateCasualties);
      stack.push(roll);
    }
  }

  private static Predicate<Unit> unitHasAirDefenseGreaterThanZero() {
    return u -> UnitAttachment.get(u.getType()).getAirDefense(u.getOwner()) > 0;
  }

  private static Predicate<Unit> unitHasAirAttackGreaterThanZero() {
    return u -> UnitAttachment.get(u.getType()).getAirAttack(u.getOwner()) > 0;
  }

  static Predicate<Unit> attackingGroundSeaBattleEscorts() {
    return Matches.unitCanAirBattle();
  }

  private static Predicate<Unit> defendingGroundSeaBattleInterceptors(final PlayerID attacker, final GameData data) {
    return PredicateBuilder.of(
        Matches.unitCanAirBattle())
        .and(Matches.unitIsEnemyOf(data, attacker))
        .and(Matches.unitWasInAirBattle().negate())
        .andIf(!Properties.getCanScrambleIntoAirBattles(data), Matches.unitWasScrambled().negate())
        .build();
  }

  private static Predicate<Unit> defendingBombingRaidInterceptors(final PlayerID attacker, final GameData data) {
    return PredicateBuilder.of(
        Matches.unitCanIntercept())
        .and(Matches.unitIsEnemyOf(data, attacker))
        .and(Matches.unitWasInAirBattle().negate())
        .andIf(!Properties.getCanScrambleIntoAirBattles(data), Matches.unitWasScrambled().negate())
        .build();
  }

  static boolean territoryCouldPossiblyHaveAirBattleDefenders(final Territory territory, final PlayerID attacker,
      final GameData data, final boolean bombing) {
    final boolean canScrambleToAirBattle = Properties.getCanScrambleIntoAirBattles(data);
    final Predicate<Unit> defendingAirMatch = bombing ? defendingBombingRaidInterceptors(attacker, data)
        : defendingGroundSeaBattleInterceptors(attacker, data);
    int maxScrambleDistance = 0;
    if (canScrambleToAirBattle) {
      for (final UnitType unitType : data.getUnitTypeList()) {
        final UnitAttachment ua = UnitAttachment.get(unitType);
        if (ua.getCanScramble() && (maxScrambleDistance < ua.getMaxScrambleDistance())) {
          maxScrambleDistance = ua.getMaxScrambleDistance();
        }
      }
    } else {
      return territory.getUnits().anyMatch(defendingAirMatch);
    }
    // should we check if the territory also has an air base?
    return territory.getUnits().anyMatch(defendingAirMatch)
        || data.getMap().getNeighbors(territory, maxScrambleDistance).stream()
            .anyMatch(Matches.territoryHasUnitsThatMatch(defendingAirMatch));
  }

  static int getAirBattleRolls(final Collection<Unit> units, final boolean defending) {
    int rolls = 0;
    for (final Unit u : units) {
      rolls += getAirBattleRolls(u, defending);
    }
    return rolls;
  }

  static int getAirBattleRolls(final Unit unit, final boolean defending) {
    if (defending) {
      if (!unitHasAirDefenseGreaterThanZero().test(unit)) {
        return 0;
      }
    } else {
      if (!unitHasAirAttackGreaterThanZero().test(unit)) {
        return 0;
      }
    }
    return Math.max(0, (defending ? UnitAttachment.get(unit.getType()).getDefenseRolls(unit.getOwner())
        : UnitAttachment.get(unit.getType()).getAttackRolls(unit.getOwner())));
  }

  private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite) {
    if (killed.size() == 0) {
      return;
    }
    final Collection<Unit> dependent = getDependentUnits(killed);
    killed.addAll(dependent);
    final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
    // m_killed.addAll(killed);
    final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(killed));
    bridge.addChange(killedChange);
    final Collection<IBattle> dependentBattles = m_battleTracker.getBlocked(AirBattle.this);
    removeFromDependents(killed, bridge, dependentBattles, false);
  }

  private static void notifyCasualties(final GUID battleId, final IDelegateBridge bridge, final String stepName,
      final DiceRoll dice, final PlayerID hitPlayer, final PlayerID firingPlayer, final CasualtyDetails details) {
    getDisplay(bridge).casualtyNotification(battleId, stepName, dice, hitPlayer, details.getKilled(),
        details.getDamaged(), Collections.emptyMap());
    // execute in a seperate thread to allow either player to click continue first.
    final Thread t = new Thread(() -> {
      try {
        getRemote(firingPlayer, bridge).confirmEnemyCasualties(battleId, "Press space to continue", hitPlayer);
      } catch (final Exception e) {
        ClientLogger.logQuietly("Error during casualty notification", e);
      }
    }, "Click to continue waiter");
    t.start();
    getRemote(hitPlayer, bridge).confirmOwnCasualties(battleId, "Press space to continue");
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  private void removeFromDependents(final Collection<Unit> units, final IDelegateBridge bridge,
      final Collection<IBattle> dependents, final boolean withdrawn) {
    for (final IBattle dependent : dependents) {
      dependent.unitsLostInPrecedingBattle(this, units, bridge, withdrawn);
    }
  }

  @Override
  public boolean isEmpty() {
    return m_attackingUnits.isEmpty();
  }

  @Override
  public void unitsLostInPrecedingBattle(final IBattle battle, final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {}
}
