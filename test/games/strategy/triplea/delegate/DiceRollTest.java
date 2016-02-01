package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.Match;

public class DiceRollTest {
  private GameData gameData;

  @Before
  protected void setUp() {
    gameData = LoadGameUtil.loadTestGame("lhtr_test.xml");
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testSimple() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final MockBattle battle = new MockBattle(westRussia);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
    // infantry defends and hits at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll.getHits(),is(1));
    // infantry does not hit at 2 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {2}));
    final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll2.getHits(),is(0));
    // infantry attacks and hits at 0 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll3.getHits(), is(1));
    // infantry attack does not hit at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  public void testSimpleLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final MockBattle battle = new MockBattle(westRussia);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
    // infantry defends and hits at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll.getHits(),is(1));
    // infantry does not hit at 2 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {2}));
    final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll2.getHits(),is(0));
    // infantry attacks and hits at 0 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll3.getHits(), is(1));
    // infantry attack does not hit at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  public void testArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final MockBattle battle = new MockBattle(westRussia);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(1, russians);
    final UnitType artillery = gameData.getUnitTypeList().getUnitType("artillery");
    units.addAll(artillery.create(1, russians));
    // artillery supported infantry and art attack at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1, 1}));
    final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(2));
  }

  @Test
  public void testVariableArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final MockBattle battle = new MockBattle(westRussia);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // Add 1 artillery
    final UnitType artillery = gameData.getUnitTypeList().getUnitType("artillery");
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
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1, 1, 1}));
    final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(3));
  }

  @Test
  public void testLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final MockBattle battle = new MockBattle(westRussia);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(3, russians);
    // 3 infantry on defense should produce exactly one hit, without rolling the dice
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll roll = DiceRoll.rollDice(units, true, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(1));
  }

  @Test
  public void testSerialize() throws Exception {
    for (int i = 0; i < 254; i++) {
      for (int j = 0; j < 254; j++) {
        final Die hit = new Die(i, j, DieType.MISS);
        assertThat(hit, is(Die.getFromWriteValue(hit.getCompressedValue())));
        final Die notHit = new Die(i, j, DieType.HIT);
        assertThat(notHit, is(Die.getFromWriteValue(notHit.getCompressedValue())));
        final Die ignored = new Die(i, j, DieType.IGNORED);
        assertThat(ignored, is(Die.getFromWriteValue(ignored.getCompressedValue())));
      }
    }
  }

  @Test
  public void testMarineAttackPlus1() throws Exception {
    gameData = LoadGameUtil.loadTestGame("iron_blitz_test.xml");
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType("marine");
    final List<Unit> attackers = marine.create(1, americans);
    final ITestDelegateBridge bridge = getDelegateBridge(americans);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final MockBattle battle = new MockBattle(algeria);
    battle.setAmphibiousLandAttackers(attackers);
    battle.setIsAmphibious(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(1));
  }

  @Test
  public void testMarineAttackPlus1LowLuck() throws Exception {
    gameData = LoadGameUtil.loadTestGame("iron_blitz_test.xml");
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType("marine");
    final List<Unit> attackers = marine.create(3, americans);
    final ITestDelegateBridge bridge = getDelegateBridge(americans);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final MockBattle battle = new MockBattle(algeria);
    battle.setAmphibiousLandAttackers(attackers);
    battle.setIsAmphibious(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(1));
  }

  @Test
  public void testMarineAttacNormalIfNotAmphibious() throws Exception {
    gameData = LoadGameUtil.loadTestGame("iron_blitz_test.xml");
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerID americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType("marine");
    final List<Unit> attackers = marine.create(1, americans);
    final ITestDelegateBridge bridge = getDelegateBridge(americans);
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final MockBattle battle = new MockBattle(algeria);
    battle.setAmphibiousLandAttackers(Collections.<Unit>emptyList());
    battle.setIsAmphibious(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(0));
  }

  @Test
  public void testAA() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    final List<Unit> bombers = bomber(gameData).create(1, british(gameData));
    // aa hits at 0 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    final DiceRoll hit =
        DiceRoll.rollAA(bomber(gameData).create(1, british(gameData)), aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    // aa missses at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll miss = DiceRoll.rollAA(bombers, aaGunList, bridge, westRussia, true);
    assertThat(miss.getHits(), is(0));
  }

  @Test
  public void testAALowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // aa hits at 0 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {0}));
    final DiceRoll hit =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    // aa missses at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll miss =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, westRussia, true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 1 should hit, and nothing should be rolled
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, westRussia, true);
    assertThat(hitNoRoll.getHits(), is(1));
  }

  @Test
  public void testAALowLuckDifferentMovement() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    final List<Unit> fighterList = fighterType.create(6, russians);
    TripleAUnit.get(fighterList.get(0)).setAlreadyMoved(1);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // aa hits at 0 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    final DiceRoll hit =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
  }

  @Test
  public void testAALowLuckWithRadar() {
    gameData = LoadGameUtil.loadTestGame("ww2v3_1941_test.xml");
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory finnland = gameData.getMap().getTerritory("Finland");
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(finnland, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    TechAttachment.get(germans).setAARadar("true");
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // aa radar hits at 1 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {1}));
    final DiceRoll hit =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches.unitIsOfTypes(
                        UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, finnland, true);
    assertThat(hit.getHits(), is(1));
    // aa missses at 2 (0 based)
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {2}));
    final DiceRoll miss =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, finnland, true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 2 should hit, and nothing should be rolled
    bridge.setRandomSource(new ScriptedRandomSource(new int[] {ScriptedRandomSource.ERROR}));
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll =
        DiceRoll
            .rollAA(
                Match.getMatches(fighterList,
                    Matches
                        .unitIsOfTypes(
                            UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(gameData))),
                aaGunList, bridge, finnland, true);
    assertThat(hitNoRoll.getHits(), is(2));
  }

  @Test
  public void testHeavyBombers() {
    gameData = LoadGameUtil.loadTestGame("iron_blitz_test.xml");
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {2, 3}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testHeavyBombersDefend() {
    gameData = LoadGameUtil.loadTestGame("iron_blitz_test.xml");
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 1}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(1).size(),is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testLHTRBomberDefend() {
    final PlayerID british = GameDataTestUtil.british(gameData);
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, true);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 1}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);

    assertThat(dice.getRolls(1).size(),is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testHeavyBombersLHTR() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {2, 3}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);

    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  public void testHeavyBombersLHTR2() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {3, 2}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  public void testHeavyBombersDefendLHTR() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerID british = GameDataTestUtil.british(gameData);
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
    testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] {0, 1}));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(1).size(), is(2));
    assertThat(dice.getHits(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(1).get(1).getType(), is(Die.DieType.IGNORED));
  }

  @Test
  public void testDiceRollCount() {
    final PlayerID british = GameDataTestUtil.british(gameData);
    final Territory location = gameData.getMap().getTerritory("United Kingdom");
    final Unit bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber).get(0);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(location);
    // default 1 roll
    assertThat(BattleCalculator.getRolls(bombers, british, false, true, territoryEffects), is(1));
    assertThat(BattleCalculator.getRolls(bombers, british, true, true, territoryEffects), is(1));
    // hb, for revised 2 on attack, 1 on defence
    final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    // lhtr hb, 2 for both
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    assertThat(BattleCalculator.getRolls(bombers, british, false, true, territoryEffects), is(2));
    assertThat(BattleCalculator.getRolls(bombers, british, true, true, territoryEffects), is(2));
  }


  @Test
  public void testGetTotalOffensivePower() {
    gameData = LoadGameUtil.loadTestGame("ww2v3_1941_test.xml");
    final Territory territory = gameData.getMap().getTerritory("Finland");


    assertThat(DiceRoll.getTotalOffensivePower( null, gameData, territory), is(0));
    assertThat(DiceRoll.getTotalOffensivePower( Collections.EMPTY_LIST, gameData, territory), is(0));

    List<Unit> units = GameDataTestUtil.infantry(1,gameData);
    assertThat( DiceRoll.getTotalOffensivePower(units, gameData,  territory), is(1));

    units.addAll(GameDataTestUtil.infantry(2,gameData));
    assertThat( DiceRoll.getTotalOffensivePower(units, gameData,  territory), is(3));

    units.addAll(GameDataTestUtil.tank(1,gameData));
    assertThat( DiceRoll.getTotalOffensivePower(units, gameData,  territory), is(6));

    units.addAll(GameDataTestUtil.tank(2,gameData));
    assertThat( DiceRoll.getTotalOffensivePower(units, gameData,  territory), is(12));

    units.addAll(GameDataTestUtil.fighter(1,gameData));
    assertThat( DiceRoll.getTotalOffensivePower(units, gameData,  territory), is(15));
  }

  @Test
  public void testGetTotalDefensivePower() {
    gameData = LoadGameUtil.loadTestGame("ww2v3_1941_test.xml");
    final Territory territory = gameData.getMap().getTerritory("Finland");

    assertThat(DiceRoll.getTotalDefensivePower( null, gameData, territory), is(0));
    assertThat(DiceRoll.getTotalDefensivePower( Collections.EMPTY_LIST, gameData, territory), is(0));

    List<Unit> units = GameDataTestUtil.infantry(1,gameData);
    assertThat( DiceRoll.getTotalDefensivePower(units, gameData,  territory), is(2));

    units.addAll(GameDataTestUtil.infantry(2,gameData));
    assertThat( DiceRoll.getTotalDefensivePower(units, gameData,  territory), is(6));

    units.addAll(GameDataTestUtil.tank(1,gameData));
    assertThat( DiceRoll.getTotalDefensivePower(units, gameData,  territory), is(9));

    units.addAll(GameDataTestUtil.tank(2,gameData));
    assertThat( DiceRoll.getTotalDefensivePower(units, gameData,  territory), is(15));

    units.addAll(GameDataTestUtil.fighter(1,gameData));
    assertThat( DiceRoll.getTotalDefensivePower(units, gameData,  territory), is(19));

  }
}
