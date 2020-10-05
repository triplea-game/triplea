package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.StrategicBombingRaidBattle;
import games.strategy.triplea.delegate.dice.TotalPowerAndTotalRolls;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.collections.CollectionUtils;

class DiceRollTest {
  private GameData gameData = TestMapGameData.LHTR.getGameData();

  @Test
  void testSimple() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(westRussia);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // infantry defends and hits at 1 (0 based)
        .thenAnswer(withValues(2)) // infantry does not hit at 2 (0 based)
        .thenAnswer(withValues(0)) // infantry attacks and hits at 0 (0 based)
        .thenAnswer(withValues(1)); // infantry attack does not hit at 1 (0 based)
    // infantry defends
    final DiceRoll roll =
        DiceRoll.rollDice(
            infantry, true, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll.getHits(), is(1));
    // infantry
    final DiceRoll roll2 =
        DiceRoll.rollDice(
            infantry, true, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll2.getHits(), is(0));
    // infantry attacks
    final DiceRoll roll3 =
        DiceRoll.rollDice(
            infantry, false, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll3.getHits(), is(1));
    // infantry attack
    final DiceRoll roll4 =
        DiceRoll.rollDice(
            infantry, false, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  void testSimpleLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects =
        TerritoryEffectHelper.getEffects(westRussia);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // infantry defends and hits at 1 (0 based)
        .thenAnswer(withValues(2)) // infantry does not hit at 2 (0 based)
        .thenAnswer(withValues(0)) // infantry attacks and hits at 0 (0 based)
        .thenAnswer(withValues(1)); // infantry attack does not hit at 1 (0 based)
    // infantry defends
    final DiceRoll roll =
        DiceRoll.rollDice(
            infantry, true, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll.getHits(), is(1));
    // infantry
    final DiceRoll roll2 =
        DiceRoll.rollDice(
            infantry, true, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll2.getHits(), is(0));
    // infantry attacks
    final DiceRoll roll3 =
        DiceRoll.rollDice(
            infantry, false, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll3.getHits(), is(1));
    // infantry attack
    final DiceRoll roll4 =
        DiceRoll.rollDice(
            infantry, false, russians, bridge, battle.getTerritory(), territoryEffects);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  void testArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(1, russians);
    final UnitType artillery =
        gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    units.addAll(artillery.create(1, russians));
    // artillery supported infantry and art attack at 1 (0 based)
    whenGetRandom(bridge).thenAnswer(withValues(1, 1));
    final DiceRoll roll =
        DiceRoll.rollDice(
            units,
            false,
            russians,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(westRussia));
    assertThat(roll.getHits(), is(2));
  }

  @Test
  void testVariableArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // Add 1 artillery
    final UnitType artillery =
        gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    final List<Unit> units = artillery.create(1, russians);
    // Set the supported unit count
    for (final Unit unit : units) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      ua.setUnitSupportCount("2");
    }
    // Now add the infantry
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    units.addAll(infantryType.create(2, russians));
    // artillery supported infantry and art attack at 1 (0 based)
    whenGetRandom(bridge).thenAnswer(withValues(1, 1, 1));
    final DiceRoll roll =
        DiceRoll.rollDice(
            units,
            false,
            russians,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(westRussia));
    assertThat(roll.getHits(), is(3));
  }

  @Test
  void testLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(3, russians);
    // 3 infantry on defense should produce exactly one hit, without rolling the dice
    final DiceRoll roll =
        DiceRoll.rollDice(
            units,
            true,
            russians,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(westRussia));
    assertThat(roll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testMarineAttackPlus1() {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(1, americans);
    attackers.forEach(
        unit -> {
          unit.getProperty(Unit.UNLOADED_AMPHIBIOUS)
              .ifPresent(
                  property -> {
                    try {
                      property.setValue(true);
                    } catch (final MutableProperty.InvalidValueException e) {
                      // ignore
                    }
                  });
        });
    final IDelegateBridge bridge = newDelegateBridge(americans);
    whenGetRandom(bridge).thenAnswer(withValues(1));
    final IBattle battle = mock(IBattle.class);
    when(battle.isAmphibious()).thenReturn(true);
    final DiceRoll roll =
        DiceRoll.rollDice(
            attackers,
            false,
            americans,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(algeria));
    assertThat(roll.getHits(), is(1));
  }

  @Test
  void testMarineAttackPlus1LowLuck() {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(3, americans);
    attackers.forEach(
        unit -> {
          unit.getProperty(Unit.UNLOADED_AMPHIBIOUS)
              .ifPresent(
                  property -> {
                    try {
                      property.setValue(true);
                    } catch (final MutableProperty.InvalidValueException e) {
                      // ignore
                    }
                  });
        });
    final IDelegateBridge bridge = newDelegateBridge(americans);
    final IBattle battle = mock(IBattle.class);
    when(battle.isAmphibious()).thenReturn(true);
    final DiceRoll roll =
        DiceRoll.rollDice(
            attackers,
            false,
            americans,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(algeria));
    assertThat(roll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testMarineAttackNormalIfNotAmphibious() {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final GamePlayer americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(1, americans);
    attackers.forEach(
        unit -> {
          unit.getProperty(Unit.UNLOADED_AMPHIBIOUS)
              .ifPresent(
                  property -> {
                    try {
                      property.setValue(false);
                    } catch (final MutableProperty.InvalidValueException e) {
                      // ignore
                    }
                  });
        });
    final IDelegateBridge bridge = newDelegateBridge(americans);
    whenGetRandom(bridge).thenAnswer(withValues(1));
    final IBattle battle = mock(IBattle.class);
    final DiceRoll roll =
        DiceRoll.rollDice(
            attackers,
            false,
            americans,
            bridge,
            battle.getTerritory(),
            TerritoryEffectHelper.getEffects(algeria));
    assertThat(roll.getHits(), is(0));
  }

  @Test
  void testAa() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final List<Unit> bombers = bomber(gameData).create(1, british(gameData));
    whenGetRandom(bridge)
        .thenAnswer(withValues(0)) // aa hits at 0 (0 based)
        .thenAnswer(withValues(1)); // aa misses at 1 (0 based)
    // aa hits
    final DiceRoll hit =
        DiceRoll.rollAa(bombers, aaGunList, bombers, aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss =
        DiceRoll.rollAa(bombers, aaGunList, bombers, aaGunList, bridge, westRussia, true);
    assertThat(miss.getHits(), is(0));
  }

  @Test
  void testAaLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    whenGetRandom(bridge)
        .thenAnswer(withValues(0)) // aa hits at 0 (0 based)
        .thenAnswer(withValues(1)); // aa misses at 1 (0 based)
    // aa hits
    final DiceRoll hit =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            westRussia,
            true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            westRussia,
            true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 1 should hit, and nothing should be rolled
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            westRussia,
            true);
    assertThat(hitNoRoll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
  }

  @Test
  void testAaLowLuckDifferentMovement() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    final List<Unit> fighterList = fighterType.create(6, russians);
    fighterList.get(0).setAlreadyMoved(BigDecimal.ONE);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // aa hits at 0 (0 based)
    final DiceRoll hit =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            westRussia,
            true);
    assertThat(hit.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  void testAaLowLuckWithRadar() {
    gameData = TestMapGameData.WW2V3_1941.getGameData();
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory finnland = gameData.getMap().getTerritory("Finland");
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(finnland, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    TechAttachment.get(germans).setAaRadar("true");
    final IDelegateBridge bridge = newDelegateBridge(russians);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // aa radar hits at 1 (0 based)
        .thenAnswer(withValues(2)); // aa misses at 2 (0 based)
    // aa radar hits
    final DiceRoll hit =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            finnland,
            true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            finnland,
            true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 2 should hit, and nothing should be rolled
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll =
        DiceRoll.rollAa(
            CollectionUtils.getMatches(
                fighterList,
                Matches.unitIsOfTypes(
                    UnitAttachment.get(aaGunList.iterator().next().getType())
                        .getTargetsAa(gameData))),
            aaGunList,
            fighterList,
            aaGunList,
            bridge,
            finnland,
            true);
    assertThat(hitNoRoll.getHits(), is(2));
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
  }

  @Test
  void testAaSupport() {
    gameData = TestMapGameData.TWW.getGameData();
    GameDataTestUtil.makeGameLowLuck(gameData);

    final GamePlayer usa = GameDataTestUtil.usa(gameData);
    final GamePlayer germany = GameDataTestUtil.germany(gameData);
    final Territory westernFrance = territory("Western France", gameData);
    final List<Unit> atGuns = GameDataTestUtil.germanAntiTankGun(gameData).create(1, germany);
    GameDataTestUtil.addTo(westernFrance, atGuns);
    final List<Unit> targets = GameDataTestUtil.americanTank(gameData).create(1, usa);

    final IDelegateBridge bridge = newDelegateBridge(usa);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // at hits at 1 (0 based)
        .thenAnswer(withValues(2)) // at misses at 2 (0 based)
        .thenAnswer(withValues(3)) // at hits at 3 with support (0 based)
        .thenAnswer(withValues(4)) // at misses at 4 with support (0 based)
        .thenAnswer(withValues(7)) // at hits at 7 with 2 AT + support (0 based)
        .thenAnswer(withValues(8)) // at misses at 8 with 2 AT + support (0 based)
        .thenAnswer(withValues(1)) // at hits at 1 with 4 AT + support + counter support (0 based)
        .thenAnswer(
            withValues(2)); // at misses at 2 with 4 AT + support + counter support (0 based)

    // 1 AT gun
    final DiceRoll hit =
        DiceRoll.rollAa(targets, atGuns, targets, atGuns, bridge, westernFrance, true);
    assertThat(hit.getHits(), is(1));
    final DiceRoll miss =
        DiceRoll.rollAa(targets, atGuns, targets, atGuns, bridge, westernFrance, true);
    assertThat(miss.getHits(), is(0));

    // 1 AT gun + 1 AT support (AT support is a unit that provides +2 AA strength for 3 units)
    final List<Unit> supportUnits = GameDataTestUtil.germanAtSupport(gameData).create(1, germany);
    final DiceRoll hitWithSupport =
        DiceRoll.rollAa(targets, atGuns, targets, supportUnits, bridge, westernFrance, true);
    assertThat(hitWithSupport.getHits(), is(1));
    final DiceRoll missWithSupport =
        DiceRoll.rollAa(targets, atGuns, targets, supportUnits, bridge, westernFrance, true);
    assertThat(missWithSupport.getHits(), is(0));

    // 2 AT guns + 1 AT support
    atGuns.addAll(GameDataTestUtil.germanAntiTankGun(gameData).create(1, germany));
    final DiceRoll hitWith2AtAndSupport =
        DiceRoll.rollAa(targets, atGuns, targets, supportUnits, bridge, westernFrance, true);
    assertThat(hitWith2AtAndSupport.getHits(), is(1));
    final DiceRoll missWith2AtAndSupport =
        DiceRoll.rollAa(targets, atGuns, targets, supportUnits, bridge, westernFrance, true);
    assertThat(missWith2AtAndSupport.getHits(), is(0));

    // 2 AT guns + 1 AT support + 1 enemy AT counter (AT counter is a unit that provides -10 AA
    // strength for 3 units)
    // Doesn't even roll any dice since strength ends up being 0 for both AT guns
    final List<Unit> enemySupportUnits =
        GameDataTestUtil.americanAtCounter(gameData).create(1, usa);
    final DiceRoll missWith2AtAndSupportAndEnemySupport =
        DiceRoll.rollAa(
            targets, atGuns, enemySupportUnits, supportUnits, bridge, westernFrance, true);
    assertThat(missWith2AtAndSupportAndEnemySupport.getHits(), is(0));

    // 4 AT guns + 1 AT support + 1 enemy AT counter
    // Enemy AT counter zeroes out 3 AT guns and AT support so just 1 AT gun fires
    atGuns.addAll(GameDataTestUtil.germanAntiTankGun(gameData).create(2, germany));
    final DiceRoll hitWith4AtAndSupportAndEnemySupport =
        DiceRoll.rollAa(
            targets, atGuns, enemySupportUnits, supportUnits, bridge, westernFrance, true);
    assertThat(hitWith4AtAndSupportAndEnemySupport.getHits(), is(1));
    final DiceRoll missWith4AtAndSupportAndEnemySupport =
        DiceRoll.rollAa(
            targets, atGuns, enemySupportUnits, supportUnits, bridge, westernFrance, true);
    assertThat(missWith4AtAndSupportAndEnemySupport.getHits(), is(0));

    thenGetRandomShouldHaveBeenCalled(bridge, times(8));
  }

  @Test
  void testHeavyBombers() {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final GamePlayer british = british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(2, 3, 2));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            false,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.HIT));
  }

  @Test
  void testHeavyBombersDefend() {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final GamePlayer british = british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            true,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));
    assertThat(dice.getRolls(1).size(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  void testLhtrBomberDefend() {
    final GamePlayer british = GameDataTestUtil.british(gameData);
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, true);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            true,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));

    assertThat(dice.getRolls(1).size(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  void testHeavyBombersLhtr() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(2, 3));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            false,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));

    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  void testHeavyBombersLhtr2() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(3, 2));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            false,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  void testHeavyBombersDefendLhtr() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0, 1));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice =
        DiceRoll.rollDice(
            bombers,
            true,
            british,
            testDelegateBridge,
            mock(Territory.class),
            TerritoryEffectHelper.getEffects(germany));
    assertThat(dice.getRolls(1).size(), is(2));
    assertThat(dice.getHits(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(1).get(1).getType(), is(Die.DieType.IGNORED));
  }

  @Test
  void testSbrRolls() {
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final Unit bomber =
        gameData
            .getMap()
            .getTerritory("United Kingdom")
            .getUnitCollection()
            .getMatches(Matches.unitIsStrategicBomber())
            .get(0);
    // default 1 roll
    assertThat(StrategicBombingRaidBattle.getSbrRolls(bomber, british), is(1));
    assertThat(StrategicBombingRaidBattle.getSbrRolls(bomber, british), is(1));
    // hb, for revised 2 on attack, 1 on defence
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(
        british,
        testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    // lhtr hb, 2 for both
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    assertThat(StrategicBombingRaidBattle.getSbrRolls(bomber, british), is(2));
    assertThat(StrategicBombingRaidBattle.getSbrRolls(bomber, british), is(2));
  }

  @Test
  void testGetTotalPowerForSupportBonusTypeCount() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Move regular units
    final GamePlayer germans = GameDataTestUtil.germany(twwGameData);
    final Territory berlin = territory("Berlin", twwGameData);
    final List<Unit> attackers = new ArrayList<>();

    attackers.addAll(GameDataTestUtil.germanInfantry(twwGameData).create(1, germans));
    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    int attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(attackPower, 6, "1 artillery should provide +1 support to the infantry");

    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(
        attackPower,
        10,
        "2 artillery should provide +2 support to the infantry as stack count is 2");

    attackers.addAll(GameDataTestUtil.germanArtillery(twwGameData).create(1, germans));
    attackPower =
        TotalPowerAndTotalRolls.getTotalPower(
            TotalPowerAndTotalRolls.getUnitPowerAndRollsForNormalBattles(
                attackers,
                new ArrayList<>(),
                attackers,
                false,
                twwGameData,
                berlin,
                new ArrayList<>()),
            twwGameData);
    assertEquals(
        attackPower,
        13,
        "3 artillery should provide +2 support to the infantry as can't provide more than 2");
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class SortAaHighToLowTest {

    @Mock private Unit unit1;
    @Mock private Unit unit2;
    @Mock private Unit unit3;
    @Mock private Unit unit4;
    @Mock private Unit unit5;

    private final List<Unit> units = new ArrayList<>();

    private UnitAttachment setupUnitAttachment(final Unit unit) {
      final UnitType unitTypeMock = mock(UnitType.class);
      final UnitAttachment unitAttachment = mock(UnitAttachment.class);
      when(unitTypeMock.getAttachment(any())).thenReturn(unitAttachment);
      when(unit.getType()).thenReturn(unitTypeMock);
      return unitAttachment;
    }

    @BeforeEach
    void setUp() {
      units.addAll(List.of(unit1, unit2, unit3, unit4, unit5));
    }

    @Test
    void testAttacking() {
      int index = 4;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getOffensiveAttackAa(any())).thenReturn(index / 2);
        index--;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, false, new HashMap<>());
      assertThat(units.get(0), is(unit1));
      assertThat(units.get(1), is(unit2));
      assertThat(units.get(2), is(unit3));
      assertThat(units.get(3), is(unit4));
      assertThat(units.get(4), is(unit5));
    }

    @Test
    void testDefending() {
      int index = 0;
      for (final var unit : units) {
        final var unitAttachment = setupUnitAttachment(unit);
        // We're integer dividing the index at this point to get duplicate sorting keys
        // in order to reach some edge cases
        when(unitAttachment.getAttackAa(any())).thenReturn(index / 2);
        index++;
      }
      TotalPowerAndTotalRolls.sortAaHighToLow(units, gameData, true, new HashMap<>());
      assertThat(units.get(0), is(unit5));
      assertThat(units.get(1), is(unit3));
      assertThat(units.get(2), is(unit4));
      assertThat(units.get(3), is(unit1));
      assertThat(units.get(4), is(unit2));
    }
  }
}
