package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attatchments.TechAttachment;
import games.strategy.triplea.attatchments.UnitAttachment;
import games.strategy.triplea.delegate.Die.DieType;
import games.strategy.triplea.xml.LoadGameUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class DiceRollTest extends TestCase
{
	private GameData m_data;
	
	@Override
	protected void setUp() throws Exception
	{
		m_data = LoadGameUtil.loadGame("revised", "lhtr.xml");
	}
	
	private ITestDelegateBridge getDelegateBridge(final PlayerID player)
	{
		return GameDataTestUtil.getDelegateBridge(player);
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		m_data = null;
	}
	
	public void testSimple()
	{
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final MockBattle battle = new MockBattle(westRussia);
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		final UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
		final List<Unit> infantry = infantryType.create(1, russians);
		final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
		// infantry defends and hits at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects);
		assertEquals(1, roll.getHits());
		// infantry does not hit at 2 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 2 }));
		final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects);
		assertEquals(0, roll2.getHits());
		// infantry attacks and hits at 0 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 0 }));
		final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects);
		assertEquals(1, roll3.getHits());
		// infantry attack does not hit at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects);
		assertEquals(0, roll4.getHits());
	}
	
	public void testSimpleLowLuck()
	{
		makeGameLowLuck();
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final MockBattle battle = new MockBattle(westRussia);
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		final UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
		final List<Unit> infantry = infantryType.create(1, russians);
		final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
		// infantry defends and hits at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects);
		assertEquals(1, roll.getHits());
		// infantry does not hit at 2 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 2 }));
		final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects);
		assertEquals(0, roll2.getHits());
		// infantry attacks and hits at 0 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 0 }));
		final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects);
		assertEquals(1, roll3.getHits());
		// infantry attack does not hit at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects);
		assertEquals(0, roll4.getHits());
	}
	
	public void testArtillerySupport()
	{
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final MockBattle battle = new MockBattle(westRussia);
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		final UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
		final List<Unit> units = infantryType.create(1, russians);
		final UnitType artillery = m_data.getUnitTypeList().getUnitType("artillery");
		units.addAll(artillery.create(1, russians));
		// artillery supported infantry and art attack at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1, 1 }));
		final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "", TerritoryEffectHelper.getEffects(westRussia));
		assertEquals(2, roll.getHits());
	}
	
	public void testVariableArtillerySupport()
	{
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final MockBattle battle = new MockBattle(westRussia);
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		// Add 1 artillery
		final UnitType artillery = m_data.getUnitTypeList().getUnitType("artillery");
		final List<Unit> units = artillery.create(1, russians);
		// Set the supported unit count
		for (final Unit unit : units)
		{
			final UnitAttachment ua = UnitAttachment.get(unit.getType());
			ua.setUnitSupportCount("2");
		}
		// Now add the infantry
		final UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
		units.addAll(infantryType.create(2, russians));
		// artillery supported infantry and art attack at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1, 1, 1 }));
		final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "", TerritoryEffectHelper.getEffects(westRussia));
		assertEquals(3, roll.getHits());
	}
	
	public void testLowLuck()
	{
		makeGameLowLuck();
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final MockBattle battle = new MockBattle(westRussia);
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		final UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
		final List<Unit> units = infantryType.create(3, russians);
		// 3 infantry on defense should produce exactly one hit, without rolling the dice
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final DiceRoll roll = DiceRoll.rollDice(units, true, russians, bridge, battle, "", TerritoryEffectHelper.getEffects(westRussia));
		assertEquals(1, roll.getHits());
	}
	
	public void testSerialize() throws Exception
	{
		for (int i = 0; i < 254; i++)
		{
			for (int j = 0; j < 254; j++)
			{
				final Die hit = new Die(i, j, DieType.MISS);
				assertEquals(hit, Die.getFromWriteValue(hit.getCompressedValue()));
				final Die notHit = new Die(i, j, DieType.HIT);
				assertEquals(notHit, Die.getFromWriteValue(notHit.getCompressedValue()));
				final Die ignored = new Die(i, j, DieType.IGNORED);
				assertEquals(ignored, Die.getFromWriteValue(ignored.getCompressedValue()));
			}
		}
	}
	
	private void makeGameLowLuck()
	{
		for (final IEditableProperty property : m_data.getProperties().getEditableProperties())
		{
			if (property.getName().equals(Constants.LOW_LUCK))
			{
				((BooleanProperty) property).setValue(true);
			}
		}
	}
	
	public void testMarineAttackPlus1() throws Exception
	{
		m_data = LoadGameUtil.loadGame("classic", "iron_blitz.xml");
		final Territory algeria = m_data.getMap().getTerritory("Algeria");
		final PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
		final UnitType marine = m_data.getUnitTypeList().getUnitType("marine");
		final List<Unit> attackers = marine.create(1, americans);
		final ITestDelegateBridge bridge = getDelegateBridge(americans);
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final MockBattle battle = new MockBattle(algeria);
		battle.setAmphibiousLandAttackers(attackers);
		battle.setIsAmphibious(true);
		final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "", TerritoryEffectHelper.getEffects(algeria));
		assertEquals(1, roll.getHits());
	}
	
	public void testMarineAttackPlus1LowLuck() throws Exception
	{
		m_data = LoadGameUtil.loadGame("classic", "iron_blitz.xml");
		makeGameLowLuck();
		final Territory algeria = m_data.getMap().getTerritory("Algeria");
		final PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
		final UnitType marine = m_data.getUnitTypeList().getUnitType("marine");
		final List<Unit> attackers = marine.create(3, americans);
		final ITestDelegateBridge bridge = getDelegateBridge(americans);
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final MockBattle battle = new MockBattle(algeria);
		battle.setAmphibiousLandAttackers(attackers);
		battle.setIsAmphibious(true);
		final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "", TerritoryEffectHelper.getEffects(algeria));
		assertEquals(1, roll.getHits());
	}
	
	public void testMarineAttacNormalIfNotAmphibious() throws Exception
	{
		m_data = LoadGameUtil.loadGame("classic", "iron_blitz.xml");
		final Territory algeria = m_data.getMap().getTerritory("Algeria");
		final PlayerID americans = m_data.getPlayerList().getPlayerID("Americans");
		final UnitType marine = m_data.getUnitTypeList().getUnitType("marine");
		final List<Unit> attackers = marine.create(1, americans);
		final ITestDelegateBridge bridge = getDelegateBridge(americans);
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final MockBattle battle = new MockBattle(algeria);
		battle.setAmphibiousLandAttackers(Collections.<Unit> emptyList());
		battle.setIsAmphibious(true);
		final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "", TerritoryEffectHelper.getEffects(algeria));
		assertEquals(0, roll.getHits());
	}
	
	public void testAA()
	{
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final UnitType aaGunType = m_data.getUnitTypeList().getUnitType("aaGun");
		final List<Unit> aaGunList = aaGunType.create(1, germans);
		GameDataTestUtil.addTo(westRussia, aaGunList);
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		final List<Unit> bombers = bomber(m_data).create(1, british(m_data));
		// aa hits at 0 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 0 }));
		final DiceRoll hit = DiceRoll.rollAA(bomber(m_data).create(1, british(m_data)), aaGunList,
					Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(hit.getHits(), 1);
		// aa missses at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll miss = DiceRoll.rollAA(bombers, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(miss.getHits(), 0);
	}
	
	public void testAALowLuck()
	{
		makeGameLowLuck();
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final UnitType aaGunType = m_data.getUnitTypeList().getUnitType("aaGun");
		final List<Unit> aaGunList = aaGunType.create(1, germans);
		GameDataTestUtil.addTo(westRussia, aaGunList);
		final UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
		List<Unit> fighterList = fighterType.create(1, russians);
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		// aa hits at 0 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 0 }));
		final DiceRoll hit = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(hit.getHits(), 1);
		// aa missses at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll miss = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(miss.getHits(), 0);
		// 6 bombers, 1 should hit, and nothing should be rolled
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		fighterList = fighterType.create(6, russians);
		final DiceRoll hitNoRoll = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(hitNoRoll.getHits(), 1);
	}
	
	public void testAALowLuckDifferentMovement()
	{
		makeGameLowLuck();
		final Territory westRussia = m_data.getMap().getTerritory("West Russia");
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final UnitType aaGunType = m_data.getUnitTypeList().getUnitType("aaGun");
		final List<Unit> aaGunList = aaGunType.create(1, germans);
		GameDataTestUtil.addTo(westRussia, aaGunList);
		final UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
		final List<Unit> fighterList = fighterType.create(6, russians);
		TripleAUnit.get(fighterList.get(0)).setAlreadyMoved(1);
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		// aa hits at 0 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		final DiceRoll hit = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, westRussia);
		assertEquals(hit.getHits(), 1);
	}
	
	public void testAALowLuckWithRadar()
	{
		m_data = LoadGameUtil.loadGame("AA50", "ww2v3_1941.xml");
		makeGameLowLuck();
		final Territory finnland = m_data.getMap().getTerritory("Finland");
		final PlayerID russians = m_data.getPlayerList().getPlayerID("Russians");
		final PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
		final UnitType aaGunType = m_data.getUnitTypeList().getUnitType("aaGun");
		final List<Unit> aaGunList = aaGunType.create(1, germans);
		GameDataTestUtil.addTo(finnland, aaGunList);
		final UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
		List<Unit> fighterList = fighterType.create(1, russians);
		TechAttachment.get(germans).setAARadar("true");
		final ITestDelegateBridge bridge = getDelegateBridge(russians);
		// aa radar hits at 1 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 1 }));
		final DiceRoll hit = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, finnland);
		assertEquals(hit.getHits(), 1);
		// aa missses at 2 (0 based)
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { 2 }));
		final DiceRoll miss = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, finnland);
		assertEquals(miss.getHits(), 0);
		// 6 bombers, 2 should hit, and nothing should be rolled
		bridge.setRandomSource(new ScriptedRandomSource(new int[] { ScriptedRandomSource.ERROR }));
		fighterList = fighterType.create(6, russians);
		final DiceRoll hitNoRoll = DiceRoll.rollAA(fighterList, aaGunList, Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAA(m_data)), bridge, finnland);
		assertEquals(hitNoRoll.getHits(), 2);
	}
	
	public void testHeavyBombers()
	{
		m_data = LoadGameUtil.loadGame("classic", "iron_blitz.xml");
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 2, 3 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(Die.DieType.HIT, dice.getRolls(4).get(0).getType());
		assertEquals(Die.DieType.HIT, dice.getRolls(4).get(1).getType());
	}
	
	public void testHeavyBombersDefend()
	{
		m_data = LoadGameUtil.loadGame("classic", "iron_blitz.xml");
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 0, 1 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(1, dice.getRolls(1).size());
		assertEquals(Die.DieType.HIT, dice.getRolls(1).get(0).getType());
	}
	
	public void testLHTRBomberDefend()
	{
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, true);
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 0, 1 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(1, dice.getRolls(1).size());
		assertEquals(Die.DieType.HIT, dice.getRolls(1).get(0).getType());
	}
	
	public void testHeavyBombersLHTR()
	{
		m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 2, 3 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(Die.DieType.HIT, dice.getRolls(4).get(0).getType());
		assertEquals(Die.DieType.IGNORED, dice.getRolls(4).get(1).getType());
		assertEquals(1, dice.getHits());
	}
	
	public void testHeavyBombersLHTR2()
	{
		m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 3, 2 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(Die.DieType.HIT, dice.getRolls(4).get(0).getType());
		assertEquals(Die.DieType.IGNORED, dice.getRolls(4).get(1).getType());
		assertEquals(1, dice.getHits());
	}
	
	public void testHeavyBombersDefendLHTR()
	{
		m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		final List<Unit> bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber);
		testDelegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 0, 1 }));
		final Territory germany = m_data.getMap().getTerritory("Germany");
		final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, new MockBattle(germany), "", TerritoryEffectHelper.getEffects(germany));
		assertEquals(2, dice.getRolls(1).size());
		assertEquals(1, dice.getHits());
		assertEquals(Die.DieType.HIT, dice.getRolls(1).get(0).getType());
		assertEquals(Die.DieType.IGNORED, dice.getRolls(1).get(1).getType());
	}
	
	public void testDiceRollCount()
	{
		final PlayerID british = m_data.getPlayerList().getPlayerID("British");
		final Territory location = m_data.getMap().getTerritory("United Kingdom");
		final Unit bombers = m_data.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.UnitIsStrategicBomber).get(0);
		final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(location);
		// default 1 roll
		assertEquals(1, BattleCalculator.getRolls(bombers, location, british, false, territoryEffects));
		assertEquals(1, BattleCalculator.getRolls(bombers, location, british, true, territoryEffects));
		// hb, for revised 2 on attack, 1 on defence
		final ITestDelegateBridge testDelegateBridge = getDelegateBridge(british);
		TechTracker.addAdvance(british, testDelegateBridge, TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, m_data, british));
		// lhtr hb, 2 for both
		m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
		assertEquals(2, BattleCalculator.getRolls(bombers, location, british, false, territoryEffects));
		assertEquals(2, BattleCalculator.getRolls(bombers, location, british, true, territoryEffects));
		// non-lhtr, only 1 for defense.
		// m_data.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.FALSE);
		// assertEquals(2, BattleCalculator.getRolls(bombers, location, british, false));
		// assertEquals(1, BattleCalculator.getRolls(bombers, location, british, true));
		// this last bit can not be tested because with the new way tech works, changing the game option once the game starts does not remove or add the extra die roll
	}
}
