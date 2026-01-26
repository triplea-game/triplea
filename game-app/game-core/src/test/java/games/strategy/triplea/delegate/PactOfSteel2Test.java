package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.MockDelegateBridge.newDelegateBridge;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.CollectionUtils;

class PactOfSteel2Test {
  private final GameState gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();

  @Test
  void testDirectOwnershipTerritories() {
    final Territory norway = gameData.getMap().getTerritoryOrNull("Norway");
    final Territory easternEurope = gameData.getMap().getTerritoryOrNull("Eastern Europe");
    final Territory eastBalkans = gameData.getMap().getTerritoryOrNull("East Balkans");
    final Territory ukraineSsr = gameData.getMap().getTerritoryOrNull("Ukraine S.S.R.");
    final Territory belorussia = gameData.getMap().getTerritoryOrNull("Belorussia");
    final GamePlayer british = GameDataTestUtil.british(gameData);
    final GamePlayer germans = GameDataTestUtil.germans(gameData);
    final GamePlayer russians = GameDataTestUtil.russians(gameData);
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
