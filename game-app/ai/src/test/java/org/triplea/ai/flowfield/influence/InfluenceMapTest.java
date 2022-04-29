package org.triplea.ai.flowfield.influence;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.ai.flowfield.neighbors.MapWithNeighbors;
import org.triplea.ai.flowfield.odds.BattleDetails;

class InfluenceMapTest {
  @Test
  void territories3InLineValues() {
    final List<Territory> territories =
        List.of(mock(Territory.class), mock(Territory.class), mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else {
                return List.of(territories.get(1));
              }
            });
    final InfluenceMap influenceMap =
        new InfluenceMap("Test", 0.5, Map.of(territories.get(0), 100L), mapWithNeighbors);
    assertThat(
        "First territory has the initial value of 100.0",
        influenceMap.getTerritories().get(territories.get(0)).getInfluence(),
        is(100L));
    assertThat(
        "Middle territory has 50% of the initial value",
        influenceMap.getTerritories().get(territories.get(1)).getInfluence(), is(50L));
    assertThat(
        "Last territory has 50% * 50% (or 25%) of the initial value",
        influenceMap.getTerritories().get(territories.get(2)).getInfluence(), is(25L));
  }

  @Test
  void territories4InLineWithInitialOnBothEndsValues() {
    final List<Territory> territories =
        List.of(
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else if (t.equals(territories.get(2))) {
                return List.of(territories.get(1), territories.get(3));
              } else {
                return List.of(territories.get(2));
              }
            });
    final InfluenceMap influenceMap =
        new InfluenceMap(
            "Test",
            0.5,
            Map.of(territories.get(0), 100L, territories.get(3), 100L),
            mapWithNeighbors);
    assertThat(
        "First territory has the initial value of 100 + 12 (diffused from the last)",
        influenceMap.getTerritories().get(territories.get(0)).getInfluence(),
        is(112L));
    assertThat(
        "2nd territory has 50 (diffused from the first) + 25 (diffused from the last)",
        influenceMap.getTerritories().get(territories.get(1)).getInfluence(),
        is(75L));
    assertThat(
        "3nd territory has 50 (diffused from the last) + 25 (diffused from the first)",
        influenceMap.getTerritories().get(territories.get(2)).getInfluence(),
        is(75L));
    assertThat(
        "Last territory has the initial value of 100 + 12 (diffused from the first)",
        influenceMap.getTerritories().get(territories.get(3)).getInfluence(),
        is(112L));
  }

  @Test
  void initialTerritoriesWithDifferentValues() {
    final List<Territory> territories =
        List.of(
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class),
            mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else if (t.equals(territories.get(2))) {
                return List.of(territories.get(1), territories.get(3));
              } else {
                return List.of(territories.get(2));
              }
            });
    final InfluenceMap influenceMap =
        new InfluenceMap(
            "Test",
            0.5,
            Map.of(
                territories.get(0),
                25L,
                territories.get(1),
                50L,
                territories.get(2),
                100L,
                territories.get(3),
                200L),
            mapWithNeighbors);

    assertThat(
        "1st territory has initial value of 25 + 25 from second + 25 from third + 25 from fourth",
        influenceMap.getTerritories().get(territories.get(0)).getInfluence(),
        is(100L));
    assertThat(
        "2nd territory has initial value of 50 + 12 from first + 50 from third + 50 from fourth",
        influenceMap.getTerritories().get(territories.get(1)).getInfluence(),
        is(162L));
    assertThat(
        "3nd territory has initial value of 100 + 6 from first + 25 from second + 100 from fourth",
        influenceMap.getTerritories().get(territories.get(2)).getInfluence(),
        is(231L));
    assertThat(
        "4th territory has initial value of 200 + 3 from first + 12 from second + 50 from third",
        influenceMap.getTerritories().get(territories.get(3)).getInfluence(),
        is(265L));
  }

  @Test
  void battleDetailsDiffusesAwayFromInitialTerritory() {
    final List<Territory> territories =
        List.of(mock(Territory.class), mock(Territory.class), mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else {
                return List.of(territories.get(1));
              }
            });
    final GameData gameData = givenGameData().build();
    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("test", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final GamePlayer player = mock(GamePlayer.class);

    final BattleDetails battleDetails =
        new BattleDetails(
            List.of(),
            unitType.createTemp(1, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());

    final InfluenceMap influenceMap =
        new InfluenceMap(
            "suffix",
            new InfluenceMapSetup("Test", 0.5, Map.of(territories.get(0), 100L)),
            mapWithNeighbors,
            t -> {
              if (t.equals(territories.get(0)) || t.equals(territories.get(2))) {
                return BattleDetails.EMPTY_DETAILS;
              } else {
                return battleDetails;
              }
            });

    assertThat(
        "1st territory's battle details by distance should be empty as the "
            + "2nd territory's details should not diffuse back towards it.",
        influenceMap.getTerritories().get(territories.get(0)).getBattleDetailsByDistance(),
        anEmptyMap());
    assertThat(
        "2nd territory's battle details by distance should also be empty since "
            + "none of the other territories have a battle distance",
        influenceMap.getTerritories().get(territories.get(1)).getBattleDetailsByDistance(),
        anEmptyMap());
    assertThat(
        "2nd territory should have the mocked battle details",
        influenceMap.getTerritories().get(territories.get(1)).getBattleDetails(),
        is(battleDetails));
    assertThat(
        "3th territory's battle details should have the 2nd territory's battle details "
            + "with a distance of 1",
        influenceMap.getTerritories().get(territories.get(2)).getBattleDetailsByDistance(),
        is(Map.of(battleDetails, 1)));
  }

  @Test
  void battleDetailsDistanceIsAlwaysSet() {
    final List<Territory> territories =
        List.of(mock(Territory.class), mock(Territory.class), mock(Territory.class));
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            territories,
            (t) -> {
              if (t.equals(territories.get(0))) {
                return List.of(territories.get(1));
              } else if (t.equals(territories.get(1))) {
                return List.of(territories.get(0), territories.get(2));
              } else {
                return List.of(territories.get(1));
              }
            });
    final GameData gameData = givenGameData().build();
    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("test", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    final GamePlayer player = mock(GamePlayer.class);

    final BattleDetails battleDetails =
        new BattleDetails(
            List.of(),
            unitType.createTemp(1, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());

    final InfluenceMap influenceMap =
        new InfluenceMap(
            "suffix",
            new InfluenceMapSetup("Test", 0.5, Map.of(territories.get(0), 100L)),
            mapWithNeighbors,
            t -> {
              if (t.equals(territories.get(0)) || t.equals(territories.get(2))) {
                return BattleDetails.EMPTY_DETAILS;
              } else {
                return battleDetails;
              }
            });

    assertThat(
        "1st territory's distance from initial territory is 0 since it is the initial territory",
        influenceMap.getTerritories().get(territories.get(0)).getDistanceFromInitialTerritory(),
        is(0));
    assertThat(
        "2nd territory's distance from initial territory is 1",
        influenceMap.getTerritories().get(territories.get(1)).getDistanceFromInitialTerritory(),
        is(1));
    assertThat(
        "3nd territory's distance from initial territory is 2",
        influenceMap.getTerritories().get(territories.get(2)).getDistanceFromInitialTerritory(),
        is(2));
  }

  @Test
  @DisplayName(
      "Two routes from start to end: ABCD and AEFCD. B has a large army and E has a small army. "
          + "So C and D will have the battle details of E but the diffused value of the ABCD path.")
  void smallestBattleDetailsIsUsedEvenIfPathIsLonger() {
    final Territory territoryA = mock(Territory.class);
    final Territory territoryB = mock(Territory.class);
    final Territory territoryC = mock(Territory.class);
    final Territory territoryD = mock(Territory.class);
    final Territory territoryE = mock(Territory.class);
    final Territory territoryF = mock(Territory.class);
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            List.of(territoryA, territoryB, territoryC, territoryD, territoryE, territoryF),
            (t) -> {
              if (t.equals(territoryA)) {
                return List.of(territoryB, territoryE);
              } else if (t.equals(territoryB)) {
                return List.of(territoryA, territoryC);
              } else if (t.equals(territoryC)) {
                return List.of(territoryB, territoryD);
              } else if (t.equals(territoryE)) {
                return List.of(territoryA, territoryF);
              } else if (t.equals(territoryF)) {
                return List.of(territoryE, territoryC);
              } else if (t.equals(territoryD)) {
                return List.of(territoryC);
              } else {
                return List.of();
              }
            });
    final GameData gameData = givenGameData().build();
    when(gameData.getDiceSides()).thenReturn(6);
    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("test", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    unitAttachment.setDefense(1);
    unitAttachment.setAttack(1);
    final GamePlayer player = mock(GamePlayer.class);

    final BattleDetails battleDetailsB =
        new BattleDetails(
            List.of(),
            unitType.createTemp(100, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());
    final BattleDetails battleDetailsE =
        new BattleDetails(
            List.of(),
            unitType.createTemp(1, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());

    final InfluenceMap influenceMap =
        new InfluenceMap(
            "suffix",
            new InfluenceMapSetup("Test", 0.5, Map.of(territoryA, 100L)),
            mapWithNeighbors,
            t -> {
              if (t.equals(territoryB)) {
                return battleDetailsB;
              } else if (t.equals(territoryE)) {
                return battleDetailsE;
              } else {
                return BattleDetails.EMPTY_DETAILS;
              }
            });

    assertThat(
        "C's battle details by distance should contain battle details from E",
        influenceMap.getTerritories().get(territoryC).getBattleDetailsByDistance(),
        is(Map.of(battleDetailsE, 1)));
    assertThat(
        "C's influence value should still be 25 because of the ABC path.",
        influenceMap.getTerritories().get(territoryC).getInfluence(),
        is(25L));
    assertThat(
        "D's battle details by distance should contain battle details from E",
        influenceMap.getTerritories().get(territoryC).getBattleDetailsByDistance(),
        is(Map.of(battleDetailsE, 1)));
    assertThat(
        "D's influence value should still be 12 because of the ABCD path.",
        influenceMap.getTerritories().get(territoryD).getInfluence(),
        is(12L));
  }

  @Test
  @DisplayName(
      "Two routes from start to end: ABCD and AEFGD. B has a large army and E has a small army. "
          + "The battle details of E should not go down to D and then back up to C.")
  void smallestBattleDetailsShouldNotGoBackUpTheDiffusedPath() {
    final Territory territoryA = mock(Territory.class);
    final Territory territoryB = mock(Territory.class);
    final Territory territoryC = mock(Territory.class);
    final Territory territoryD = mock(Territory.class);
    final Territory territoryE = mock(Territory.class);
    final Territory territoryF = mock(Territory.class);
    final Territory territoryG = mock(Territory.class);
    final MapWithNeighbors mapWithNeighbors =
        new MapWithNeighbors(
            List.of(
                territoryA, territoryB, territoryC, territoryD, territoryE, territoryF, territoryG),
            (t) -> {
              if (t.equals(territoryA)) {
                return List.of(territoryB, territoryE);
              } else if (t.equals(territoryB)) {
                return List.of(territoryA, territoryC);
              } else if (t.equals(territoryC)) {
                return List.of(territoryB, territoryD);
              } else if (t.equals(territoryE)) {
                return List.of(territoryA, territoryF);
              } else if (t.equals(territoryF)) {
                return List.of(territoryE, territoryG);
              } else if (t.equals(territoryG)) {
                return List.of(territoryF, territoryD);
              } else if (t.equals(territoryD)) {
                return List.of(territoryG, territoryC);
              } else {
                return List.of();
              }
            });
    final GameData gameData = givenGameData().build();
    when(gameData.getDiceSides()).thenReturn(6);
    final UnitType unitType = new UnitType("test", gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("test", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    unitAttachment.setDefense(1);
    unitAttachment.setAttack(1);
    final GamePlayer player = mock(GamePlayer.class);

    final BattleDetails battleDetailsB =
        new BattleDetails(
            List.of(),
            unitType.createTemp(100, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());
    final BattleDetails battleDetailsE =
        new BattleDetails(
            List.of(),
            unitType.createTemp(1, player),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.OFFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            CombatValueBuilder.mainCombatValue()
                .side(BattleState.Side.DEFENSE)
                .gameDiceSides(6)
                .gameSequence(mock(GameSequence.class))
                .lhtrHeavyBombers(false)
                .supportAttachments(List.of()),
            List.of());

    final InfluenceMap influenceMap =
        new InfluenceMap(
            "suffix",
            new InfluenceMapSetup("Test", 0.5, Map.of(territoryA, 100L)),
            mapWithNeighbors,
            t -> {
              if (t.equals(territoryB)) {
                return battleDetailsB;
              } else if (t.equals(territoryE)) {
                return battleDetailsE;
              } else {
                return BattleDetails.EMPTY_DETAILS;
              }
            });

    assertThat(
        "D's battle details by distance should contain battle details from E",
        influenceMap.getTerritories().get(territoryD).getBattleDetailsByDistance(),
        is(Map.of(battleDetailsE, 1)));
    assertThat(
        "C's battle details by distance should not be updated from E and "
            + "should still contain battle details from B",
        influenceMap.getTerritories().get(territoryC).getBattleDetailsByDistance(),
        is(Map.of(battleDetailsB, 1)));
  }
}
