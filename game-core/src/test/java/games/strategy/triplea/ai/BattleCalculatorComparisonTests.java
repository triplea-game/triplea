package games.strategy.triplea.ai;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.fast.FastOddsEstimator;
import games.strategy.triplea.ai.pro.ProData;
import games.strategy.triplea.ai.tree.BattleTreeCalculator;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.odds.calculator.AggregateResults;
import games.strategy.triplea.odds.calculator.BattleCalculator;
import games.strategy.triplea.odds.calculator.ConcurrentBattleCalculator;
import games.strategy.triplea.odds.calculator.IBattleCalculator;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.triplea.xml.TestMapGameData;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.util.Tuple;

class BattleCalculatorComparisonTests {
  private static final GameData data = TestMapGameData.GLOBAL1940.getGameData();
  private static final GamePlayer BRITISH =
      checkNotNull(data.getPlayerList().getPlayerId("British"));
  private static final GamePlayer GERMAN =
      checkNotNull(data.getPlayerList().getPlayerId("Germans"));
  private static final Territory FRANCE = checkNotNull(territory("France", data));
  private static final Territory SEA_ZONE = checkNotNull(territory("110 Sea Zone", data));
  private static final UnitType AAGUN = checkNotNull(data.getUnitTypeList().getUnitType("aaGun"));
  private static final UnitType INFANTRY =
      checkNotNull(data.getUnitTypeList().getUnitType("infantry"));
  private static final UnitType ARTILLERY =
      checkNotNull(data.getUnitTypeList().getUnitType("artillery"));
  private static final UnitType ARMOUR = checkNotNull(data.getUnitTypeList().getUnitType("armour"));
  private static final UnitType FIGHTER =
      checkNotNull(data.getUnitTypeList().getUnitType("fighter"));
  private static final UnitType TACTICAL_BOMBER =
      checkNotNull(data.getUnitTypeList().getUnitType("tactical_bomber"));
  private static final UnitType BOMBER = checkNotNull(data.getUnitTypeList().getUnitType("bomber"));
  private static final UnitType DESTROYER =
      checkNotNull(data.getUnitTypeList().getUnitType("destroyer"));
  private static final UnitType CRUISER =
      checkNotNull(data.getUnitTypeList().getUnitType("cruiser"));
  private static final UnitType BATTLESHIP =
      checkNotNull(data.getUnitTypeList().getUnitType("battleship"));

  private static final GameData dataTww = TestMapGameData.TWW.getGameData();
  private static final GamePlayer BRITAIN = checkNotNull(GameDataTestUtil.britain(dataTww));
  private static final GamePlayer GERMANY = checkNotNull(GameDataTestUtil.germany(dataTww));
  private static final Territory LAND_NO_ATTACHMENTS =
      checkNotNull(GameDataTestUtil.territory("Alberta", dataTww));

  @Test
  void comparePerformanceOfCalculators() throws InterruptedException {
    // System.out.println("Sleeping to allow attaching asyncProfiler");
    // Thread.sleep(2000);
    if (false) {
      final int quantity = 3;
      System.out.println(quantity + " of each Total World War land units");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishInfantry").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishAlpineInfantry").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishMarine").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishArtillery").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishHeavyArtillery").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww
              .getUnitTypeList()
              .getUnitType("britishMobileArtillery")
              .create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishMech.Infantry").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishTank").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishHeavyTank").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishFighter").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww
              .getUnitTypeList()
              .getUnitType("britishAdvancedFighter")
              .create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishTacticalBomber").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww
              .getUnitTypeList()
              .getUnitType("britishAdvancedTacticalBomber")
              .create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishAntiAirGun").create(quantity, BRITAIN));
      attackingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("britishAntiTankGun").create(quantity, BRITAIN));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanInfantry").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanAlpineInfantry").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanMarine").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanArtillery").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanHeavyArtillery").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanMobileArtillery").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanMech.Infantry").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanTank").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanHeavyTank").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanFighter").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanAdvancedFighter").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanTacticalBomber").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww
              .getUnitTypeList()
              .getUnitType("germanAdvancedTacticalBomber")
              .create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanAntiAirGun").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanAntiTankGun").create(quantity, GERMANY));
      defendingUnits.addAll(
          dataTww.getUnitTypeList().getUnitType("germanATSupport").create(quantity, GERMANY));
      compareCalculators(attackingUnits, defendingUnits, LAND_NO_ATTACHMENTS, dataTww);
    }
    // if (true) return;

    if (true) {
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.addAll(INFANTRY.create(1, BRITISH));
      attackingUnits.addAll(ARTILLERY.create(1, BRITISH));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.addAll(INFANTRY.create(2, GERMAN));
      compareCalculators(attackingUnits, defendingUnits, FRANCE);
      return;
    }

    if (true) {
      for (int quantity = 5; quantity <= 15; quantity += 5) {
        System.out.println(
            quantity + " of infantry, artillery, armour, fighter, tactical_bomber, bomber vs same");
        final Collection<Unit> attackingUnits = new ArrayList<>();
        attackingUnits.addAll(INFANTRY.create(quantity, BRITISH));
        attackingUnits.addAll(ARTILLERY.create(quantity, BRITISH));
        attackingUnits.addAll(ARMOUR.create(quantity, BRITISH));
        attackingUnits.addAll(FIGHTER.create(quantity, BRITISH));
        attackingUnits.addAll(TACTICAL_BOMBER.create(quantity, BRITISH));
        attackingUnits.addAll(BOMBER.create(quantity, BRITISH));
        final Collection<Unit> defendingUnits = new ArrayList<>();
        defendingUnits.addAll(INFANTRY.create(quantity, GERMAN));
        defendingUnits.addAll(ARTILLERY.create(quantity, GERMAN));
        defendingUnits.addAll(ARMOUR.create(quantity, GERMAN));
        defendingUnits.addAll(FIGHTER.create(quantity, GERMAN));
        defendingUnits.addAll(TACTICAL_BOMBER.create(quantity, GERMAN));
        defendingUnits.addAll(BOMBER.create(quantity, GERMAN));
        compareCalculators(attackingUnits, defendingUnits, FRANCE);
      }
    }

    if (true) {
      System.out.println("1 infantry, 1 armour vs 2 infantry");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(INFANTRY, BRITISH, data));
      attackingUnits.add(new Unit(ARMOUR, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, FRANCE);
    }

    if (true) {
      System.out.println("1 armour, 1 tactical_bomber vs 2 infantry, 1 fighter");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(ARMOUR, BRITISH, data));
      attackingUnits.add(new Unit(TACTICAL_BOMBER, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      defendingUnits.add(new Unit(FIGHTER, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, FRANCE);
    }

    if (true) {
      System.out.println("1 infantry, 1 artillery vs 2 infantry");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(INFANTRY, BRITISH, data));
      attackingUnits.add(new Unit(ARTILLERY, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, FRANCE);
    }

    if (true) {
      System.out.println("1 cruiser, 1 destroyer vs 1 cruiser, 1 destroyer");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(CRUISER, BRITISH, data));
      attackingUnits.add(new Unit(DESTROYER, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(CRUISER, GERMAN, data));
      defendingUnits.add(new Unit(DESTROYER, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, SEA_ZONE);
    }

    if (true) {
      System.out.println("1 cruiser, 1 destroyer, 1 battleship vs 2 cruiser, 1 destroyer");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(CRUISER, BRITISH, data));
      attackingUnits.add(new Unit(DESTROYER, BRITISH, data));
      attackingUnits.add(new Unit(BATTLESHIP, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(CRUISER, GERMAN, data));
      defendingUnits.add(new Unit(CRUISER, GERMAN, data));
      defendingUnits.add(new Unit(DESTROYER, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, SEA_ZONE);
    }

    if (true) {
      System.out.println("1 infantry, 1 fighter vs 1 infantry, 1 aa gun");
      final Collection<Unit> attackingUnits = new ArrayList<>();
      attackingUnits.add(new Unit(INFANTRY, BRITISH, data));
      attackingUnits.add(new Unit(FIGHTER, BRITISH, data));
      final Collection<Unit> defendingUnits = new ArrayList<>();
      defendingUnits.add(new Unit(INFANTRY, GERMAN, data));
      defendingUnits.add(new Unit(AAGUN, GERMAN, data));
      compareCalculators(attackingUnits, defendingUnits, FRANCE);
    }
  }

  void compareCalculators(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Territory territory) {
    compareCalculators(attackingUnits, defendingUnits, territory, data);
  }

  void compareCalculators(
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Territory territory,
      final GameData data) {
    final Collection<Unit> bombardingUnits = new ArrayList<>();

    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(territory);

    final BattleTreeCalculator estimator = new BattleTreeCalculator();
    estimator.setGameData(data);
    final AggregateResults estimate =
        estimator.calculate(
            attackingUnits.iterator().next().getOwner(),
            defendingUnits.iterator().next().getOwner(),
            territory,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            false,
            1);

    printResult("BattleTree", estimate, attackingUnits, defendingUnits, territory, data);
    // if (true) return;

    final BattleCalculator singleCalculator = new BattleCalculator(data, false);
    final AggregateResults singleRunResults =
        singleCalculator.calculate(
            attackingUnits.iterator().next().getOwner(),
            defendingUnits.iterator().next().getOwner(),
            territory,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            false,
            200);

    printResult(
        "BattleCalculator", singleRunResults, attackingUnits, defendingUnits, territory, data);

    final ConcurrentBattleCalculator hardAiCalculator = new ConcurrentBattleCalculator();
    hardAiCalculator.setGameData(data);
    final AggregateResults hardAiResults =
        hardAiCalculator.calculate(
            attackingUnits.iterator().next().getOwner(),
            defendingUnits.iterator().next().getOwner(),
            territory,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            false,
            200);

    printResult("Hard AI 200 runs", hardAiResults, attackingUnits, defendingUnits, territory, data);

    final ConcurrentBattleCalculator hardAi2Calculator = new ConcurrentBattleCalculator();
    hardAi2Calculator.setGameData(data);
    final AggregateResults hardAi2Results =
        hardAi2Calculator.calculate(
            attackingUnits.iterator().next().getOwner(),
            defendingUnits.iterator().next().getOwner(),
            territory,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            false,
            2000);

    printResult(
        "Hard AI 2000 runs", hardAi2Results, attackingUnits, defendingUnits, territory, data);

    final ProData proData = new ProData();
    proData.initializeSimulation(null, data, attackingUnits.iterator().next().getOwner());
    final IBattleCalculator fastOddsEstimator = new FastOddsEstimator(proData);
    final AggregateResults fastOddsResults =
        fastOddsEstimator.calculate(
            attackingUnits.iterator().next().getOwner(),
            defendingUnits.iterator().next().getOwner(),
            territory,
            attackingUnits,
            defendingUnits,
            bombardingUnits,
            territoryEffects,
            false,
            3);

    printResult("FastOdds", fastOddsResults, attackingUnits, defendingUnits, territory, data);
  }

  private void printResult(
      final String name,
      final AggregateResults result,
      final Collection<Unit> attackingUnits,
      final Collection<Unit> defendingUnits,
      final Territory territory,
      final GameData data) {
    final DecimalFormat df = new DecimalFormat("0.00");
    String output = String.format("%-18s", name + ":");
    output += " Time: " + String.format("%-7.3f", result.getTime() / 1000.0);
    output += " Win: " + df.format(result.getAttackerWinPercent() * 100.0);
    output += " AvgRounds: " + String.format("%.1f", result.getAverageBattleRoundsFought());
    final List<Unit> mainCombatAttackers =
        CollectionUtils.getMatches(
            attackingUnits, Matches.unitCanBeInBattle(true, !territory.isWater(), 1, true));
    final List<Unit> mainCombatDefenders =
        CollectionUtils.getMatches(
            defendingUnits, Matches.unitCanBeInBattle(false, !territory.isWater(), 1, true));
    output +=
        " Avg Tuv Swing: "
            + df.format(
                result.getAverageTuvSwing(
                    attackingUnits.iterator().next().getOwner(),
                    mainCombatAttackers,
                    defendingUnits.iterator().next().getOwner(),
                    mainCombatDefenders,
                    data));
    final Tuple<Double, Double> tuvUnitsLeft =
        result.getAverageTuvOfUnitsLeftOver(
            TuvUtils.getCostsForTuv(attackingUnits.iterator().next().getOwner(), data),
            TuvUtils.getCostsForTuv(defendingUnits.iterator().next().getOwner(), data));
    output +=
        " Avg Tuv Units Left: "
            + df.format(tuvUnitsLeft.getFirst())
            + ","
            + df.format(tuvUnitsLeft.getSecond());

    System.out.println(output);
    System.out.println("Avg Attacking Units: " + result.getAverageAttackingUnitsRemaining());
    System.out.println("Avg Defending Units: " + result.getAverageDefendingUnitsRemaining());
  }
}
