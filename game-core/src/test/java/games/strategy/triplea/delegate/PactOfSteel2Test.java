package games.strategy.triplea.delegate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.test.common.Integration;
import org.triplea.test.common.TestType;

@Integration(type = TestType.ACCEPTANCE)
class PactOfSteel2Test {
  private GameData gameData;

  @BeforeEach
  void setUp() throws Exception {
    gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
  }

  private IDelegateBridge newDelegateBridge(final PlayerId player) {
    return MockDelegateBridge.newInstance(gameData, player);
  }

  @Test
  void testDirectOwnershipTerritories() {
    final Territory norway = gameData.getMap().getTerritory("Norway");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    final Territory eastBalkans = gameData.getMap().getTerritory("East Balkans");
    final Territory ukraineSsr = gameData.getMap().getTerritory("Ukraine S.S.R.");
    final Territory belorussia = gameData.getMap().getTerritory("Belorussia");
    final PlayerId british = GameDataTestUtil.british(gameData);
    final PlayerId germans = GameDataTestUtil.germans(gameData);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // this National Objective russia has to own at least 3 of the 5 territories by itself
    final RulesAttachment russianEasternEurope =
        RulesAttachment.get(russians, "objectiveAttachmentRussians1_EasternEurope");
    final Collection<Territory> terrs = new ArrayList<>();
    terrs.add(norway);
    terrs.add(easternEurope);
    terrs.add(eastBalkans);
    terrs.add(ukraineSsr);
    terrs.add(belorussia);
    assertEquals(5, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    norway.setOwner(british);
    easternEurope.setOwner(russians);
    eastBalkans.setOwner(russians);
    ukraineSsr.setOwner(germans);
    belorussia.setOwner(germans);
    assertEquals(2, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(2, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(1, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    ukraineSsr.setOwner(british);
    belorussia.setOwner(british);
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(2, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(3, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    norway.setOwner(russians);
    ukraineSsr.setOwner(germans);
    belorussia.setOwner(germans);
    assertEquals(2, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(3, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
    ukraineSsr.setOwner(russians);
    assertEquals(1, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(4, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
    belorussia.setOwner(russians);
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)));
    assertEquals(5, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)));
    assertEquals(0, CollectionUtils.countMatches(terrs, Matches.isTerritoryOwnedBy(british)));
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
  }

  // TODO: Consider adding the following tests:
  //
  // testSupportAttachments
  //
  // testNationalObjectiveUses
  //
  // testBlockadeAndBlockadeZones
  //
  // testTriggers
  //
  // testConditions
  //
  // testObjectives
  //
  // testTechnologyFrontiers
  // - frontiers, renaming, generic, and new techs and adding of players to frontiers
  //
  // testIsCombatTransport
  //
  // testIsConstruction
  // - isConstruction, constructionType, constructionsPerTerrPerTypePerTurn,
  // maxConstructionsPerTypePerTerr,
  // - "More Constructions with Factory", "More Constructions with Factory", "Unlimited
  // Constructions"
  //
  // testMaxPlacePerTerritory
  //
  // testCapitalCapturePlayerOptions
  // - destroysPUs, retainCapitalNumber, retainCapitalProduceNumber
  //
  // testUnitPlacementRestrictions
  //
  // testRepairsUnits
  // - repairsUnits, "Two HitPoint Units Require Repair Facilities", "Units Repair Hits Start Turn"
  //
  // testProductionPerXTerritories
  //
  // testGiveUnitControl
  // - giveUnitControl, changeUnitOwners, canBeGivenByTerritoryTo, "Give Units By Territory"
  //
  // testDiceSides
  //
  // testMaxBuiltPerPlayer
  //
  // testDestroyedWhenCapturedBy
  // - "Units Can Be Destroyed Instead Of Captured", destroyedWhenCapturedBy
  //
  // testIsInfrastructure
  //
  // testCanBeDamaged
  //
  // testIsSuicide
  // - isSuicide, "Suicide and Munition Casualties Restricted",
  // - "Defending Suicide and Munition Units Do Not Fire"
}
