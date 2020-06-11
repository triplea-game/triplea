package games.strategy.triplea.delegate.battle;

import static games.strategy.triplea.Constants.LAND_BATTLE_ROUNDS;
import static games.strategy.triplea.Constants.RETREATING_UNITS_REMAIN_IN_PLACE;
import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.battleDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.move;
import static games.strategy.triplea.delegate.GameDataTestUtil.moveDelegate;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.AbstractDelegateTestCase;
import games.strategy.triplea.delegate.AbstractMoveDelegate;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.triplea.java.collections.IntegerMap;

class MustFightBattleTest extends AbstractDelegateTestCase {
  @Test
  void testFightWithIsSuicideOnHit() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create battle with 1 cruiser attacking 1 mine
    final GamePlayer usa = GameDataTestUtil.usa(twwGameData);
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final Territory sz33 = territory("33 Sea Zone", twwGameData);
    addTo(sz33, GameDataTestUtil.americanCruiser(twwGameData).create(1, usa));
    final Territory sz40 = territory("40 Sea Zone", twwGameData);
    addTo(sz40, GameDataTestUtil.germanMine(twwGameData).create(1, germany));
    final IDelegateBridge bridge = newDelegateBridge(usa);
    advanceToStep(bridge, "CombatMove");
    moveDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    moveDelegate(twwGameData).start();
    move(sz33.getUnits(), new Route(sz33, sz40));
    moveDelegate(twwGameData).end();
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(sz40);

    // Set first roll to hit (mine AA) and check that both units are killed
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.fight(bridge);
    assertEquals(0, sz40.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(1));
  }

  @Test
  void testFightWithBothZeroStrength() {
    final GameData twwGameData = TestMapGameData.TWW.getGameData();

    // Create TWW battle in Celebes with 1 inf attacking 1 strat where both have 0 strength
    final GamePlayer usa = GameDataTestUtil.usa(twwGameData);
    final GamePlayer germany = GameDataTestUtil.germany(twwGameData);
    final Territory celebes = territory("Celebes", twwGameData);
    celebes.getUnitCollection().clear();
    addTo(celebes, GameDataTestUtil.americanStrategicBomber(twwGameData).create(1, usa));
    addTo(celebes, GameDataTestUtil.germanInfantry(twwGameData).create(1, germany));
    final IDelegateBridge bridge = newDelegateBridge(germany);
    battleDelegate(twwGameData).setDelegateBridgeAndPlayer(bridge);
    BattleDelegate.doInitialize(battleDelegate(twwGameData).getBattleTracker(), bridge);
    final IBattle battle =
        AbstractMoveDelegate.getBattleTracker(twwGameData).getPendingBattle(celebes);

    // Ensure battle ends, both units remain, and has 0 rolls
    battle.fight(bridge);
    assertEquals(2, celebes.getUnitCollection().size());
    thenGetRandomShouldHaveBeenCalled(bridge, times(0));
  }

  @ParameterizedTest(name = "{index} {0}")
  @MethodSource
  void attackerRetreatTerritories(
      final String displayName,
      final GamePlayer attacker,
      final Map<Territory, Collection<Unit>> attackingFromMap,
      final List<Unit> attackingUnits,
      final Territory battleSite,
      final BattleTracker battleTracker,
      final GameData gameData,
      final boolean headless,
      final Collection<Territory> expected) {

    final MustFightBattle battle =
        new MustFightBattle(battleSite, attacker, gameData, battleTracker);
    battle.setAttackingFromAndMap(attackingFromMap);
    battle.setUnits(
        List.of(), attackingUnits, List.of(), List.of(), mock(GamePlayer.class), List.of());
    battle.setHeadless(headless);

    final Collection<Territory> retreatTerritories = battle.getAttackerRetreatTerritories();

    assertThat(retreatTerritories, containsInAnyOrder(expected.toArray()));
  }

  static List<Arguments> attackerRetreatTerritories() {
    final List<Arguments> arguments = new ArrayList<>();

    {
      final GamePlayer attacker = mock(GamePlayer.class);
      final GamePlayer defender = mock(GamePlayer.class);
      final Territory battleSite =
          MockTerritory.builder().hasEmptyUnitCollection().owner(defender).build();
      final GameData gameData =
          MockGameData.builder().isAtWar(attacker, defender).landBattleRounds().build();
      arguments.addAll(
          List.of(
              Arguments.of(
                  "Headless returns battleSite",
                  attacker,
                  Map.of(),
                  List.of(),
                  battleSite,
                  mock(BattleTracker.class),
                  gameData,
                  true,
                  Set.of(battleSite)),
              Arguments.of(
                  "All air units returns battleSite",
                  attacker,
                  Map.of(),
                  List.of(MockUnit.builder().isAir().build()),
                  battleSite,
                  mock(BattleTracker.class),
                  gameData,
                  false,
                  Set.of(battleSite))));
    }
    {
      final GamePlayer attacker = mock(GamePlayer.class);
      final GamePlayer defender = mock(GamePlayer.class);
      final Territory battleSite =
          MockTerritory.builder().hasEmptyUnitCollection().owner(defender).build();
      arguments.add(
          Arguments.of(
              "RetreatInPlace returns battleSite",
              attacker,
              Map.of(),
              List.of(),
              battleSite,
              mock(BattleTracker.class),
              MockGameData.builder().isAtWar(attacker, defender).isRetreatingInPlace().build(),
              false,
              Set.of(battleSite)));
    }
    {
      final GamePlayer attacker = mock(GamePlayer.class);
      final GamePlayer defender = mock(GamePlayer.class);
      final Territory battleSite =
          MockTerritory.builder().hasDefenderUnitCollection(defender).build();
      final Territory attackFrom =
          MockTerritory.builder().hasEmptyUnitCollection().owner(attacker).build();
      arguments.add(
          Arguments.of(
              "Retreat to attacker's land territory",
              attacker,
              Map.of(attackFrom, List.of()),
              List.of(MockUnit.builder().hasAttachment().build()),
              battleSite,
              mock(BattleTracker.class),
              MockGameData.builder().isAtWar(attacker, defender).landBattleRounds().build(),
              false,
              List.of(attackFrom)));
    }
    {
      final GamePlayer attacker = mock(GamePlayer.class);
      final GamePlayer defender = mock(GamePlayer.class);
      final Territory battleSite =
          MockTerritory.builder().hasDefenderUnitCollection(defender).build();
      final Territory attackFrom =
          MockTerritory.builder().hasEmptyUnitCollection().nullOwner().isWater().build();
      arguments.add(
          Arguments.of(
              "Retreat to unowned water territory",
              attacker,
              Map.of(attackFrom, List.of()),
              List.of(MockUnit.builder().isSea().build()),
              battleSite,
              mock(BattleTracker.class),
              MockGameData.builder().isAtWar(attacker, defender).landBattleRounds().build(),
              false,
              List.of(attackFrom)));
    }
    {
      final GamePlayer attacker = mock(GamePlayer.class);
      final GamePlayer defender = mock(GamePlayer.class);
      final Territory battleSite =
          MockTerritory.builder().hasDefenderUnitCollection(defender).nullOwner().isWater().build();
      final Territory attackFrom =
          MockTerritory.builder().hasEmptyUnitCollection().nullOwner().isWater().build();
      arguments.add(
          Arguments.of(
              "Don't include battleSite if it is in the attackedFrom",
              attacker,
              Map.of(attackFrom, List.of(), battleSite, List.of()),
              List.of(MockUnit.builder().isSea().build()),
              battleSite,
              mock(BattleTracker.class),
              MockGameData.builder().isAtWar(attacker, defender).landBattleRounds().build(),
              false,
              List.of(attackFrom)));
    }
    return arguments;
  }

  static class MockGameData {
    private final GameData gameData;
    private final GameProperties gameProperties;
    private final RelationshipTracker relationshipTracker;
    private boolean propertiesSetup = false;
    private boolean relationshipTrackerSetup = false;

    private MockGameData() {
      gameData = mock(GameData.class);
      gameProperties = mock(GameProperties.class);
      relationshipTracker = mock(RelationshipTracker.class);
    }

    static MockGameData builder() {
      return new MockGameData();
    }

    GameData build() {
      return gameData;
    }

    private MockGameData landBattleRounds() {
      setupProperties();
      when(gameProperties.get(LAND_BATTLE_ROUNDS, -1)).thenReturn(-1);
      return this;
    }

    private void setupProperties() {
      if (!propertiesSetup) {
        propertiesSetup = true;
        when(gameData.getProperties()).thenReturn(gameProperties);
      }
    }

    MockGameData isRetreatingInPlace() {
      setupProperties();
      when(gameProperties.get(RETREATING_UNITS_REMAIN_IN_PLACE, false)).thenReturn(true);
      return this;
    }

    private void setupRelationshipTracker() {
      if (!relationshipTrackerSetup) {
        relationshipTrackerSetup = true;
        when(gameData.getRelationshipTracker()).thenReturn(relationshipTracker);
      }
    }

    MockGameData isAtWar(final GamePlayer p1, final GamePlayer p2) {
      setupRelationshipTracker();
      when(relationshipTracker.isAtWar(p1, p2)).thenReturn(true);
      return this;
    }
  }

  static class MockTerritory {
    private final Territory territory;
    private final UnitCollection unitCollection;
    private boolean collectionSetup = false;

    private MockTerritory() {
      territory = mock(Territory.class);
      unitCollection = mock(UnitCollection.class);
    }

    static MockTerritory builder() {
      return new MockTerritory();
    }

    Territory build() {
      return territory;
    }

    MockTerritory hasEmptyUnitCollection() {
      setupUnitCollection();
      return this;
    }

    private void setupUnitCollection() {
      if (!collectionSetup) {
        collectionSetup = true;
        when(territory.getUnitCollection()).thenReturn(unitCollection);
      }
    }

    MockTerritory hasDefenderUnitCollection(final GamePlayer defender) {
      setupUnitCollection();
      final IntegerMap<GamePlayer> count = new IntegerMap<>();
      count.put(defender, 10);
      when(unitCollection.getPlayerUnitCounts()).thenReturn(count);
      return this;
    }

    MockTerritory owner(final GamePlayer owner) {
      when(territory.getOwner()).thenReturn(owner);
      return this;
    }

    MockTerritory nullOwner() {
      when(territory.getOwner()).thenReturn(GamePlayer.NULL_PLAYERID);
      return this;
    }

    MockTerritory isWater() {
      when(territory.isWater()).thenReturn(true);
      return this;
    }
  }

  static class MockUnit {
    private final Unit unit;
    private final UnitType unitType;
    private final UnitAttachment unitAttachment;
    private boolean attachmentSetup = false;

    private MockUnit() {
      unit = mock(Unit.class);
      unitType = mock(UnitType.class);
      unitAttachment = mock(UnitAttachment.class);
    }

    static MockUnit builder() {
      return new MockUnit();
    }

    Unit build() {
      return unit;
    }

    MockUnit hasAttachment() {
      setupAttachment();
      return this;
    }

    private void setupAttachment() {
      if (!attachmentSetup) {
        attachmentSetup = true;
        when(unit.getType()).thenReturn(unitType);
        when(unitType.getAttachment(UNIT_ATTACHMENT_NAME)).thenReturn(unitAttachment);
      }
    }

    MockUnit isAir() {
      setupAttachment();
      when(unitAttachment.getIsAir()).thenReturn(true);
      return this;
    }

    MockUnit isSea() {
      setupAttachment();
      when(unitAttachment.getIsSea()).thenReturn(true);
      return this;
    }
  }
}
