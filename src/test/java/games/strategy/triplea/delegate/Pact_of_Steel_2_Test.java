package games.strategy.triplea.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.Match;

public class Pact_of_Steel_2_Test {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testDirectOwnershipTerritories() {
    final Territory Norway = gameData.getMap().getTerritory("Norway");
    final Territory Eastern_Europe = gameData.getMap().getTerritory("Eastern Europe");
    final Territory East_Balkans = gameData.getMap().getTerritory("East Balkans");
    final Territory Ukraine_S_S_R_ = gameData.getMap().getTerritory("Ukraine S.S.R.");
    final Territory Belorussia = gameData.getMap().getTerritory("Belorussia");
    final PlayerID british = GameDataTestUtil.british(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // this National Objective russia has to own at least 3 of the 5 territories by itself
    final RulesAttachment russian_easternEurope =
        RulesAttachment.get(russians, "objectiveAttachmentRussians1_EasternEurope");
    final Collection<Territory> terrs = new ArrayList<>();
    terrs.add(Norway);
    terrs.add(Eastern_Europe);
    terrs.add(East_Balkans);
    terrs.add(Ukraine_S_S_R_);
    terrs.add(Belorussia);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 5);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertFalse(russian_easternEurope.isSatisfied(null, bridge));
    Norway.setOwner(british);
    Eastern_Europe.setOwner(russians);
    East_Balkans.setOwner(russians);
    Ukraine_S_S_R_.setOwner(germans);
    Belorussia.setOwner(germans);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 1);
    assertFalse(russian_easternEurope.isSatisfied(null, bridge));
    Ukraine_S_S_R_.setOwner(british);
    Belorussia.setOwner(british);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 3);
    assertFalse(russian_easternEurope.isSatisfied(null, bridge));
    Norway.setOwner(russians);
    Ukraine_S_S_R_.setOwner(germans);
    Belorussia.setOwner(germans);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 3);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russian_easternEurope.isSatisfied(null, bridge));
    Ukraine_S_S_R_.setOwner(russians);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 1);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 4);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russian_easternEurope.isSatisfied(null, bridge));
    Belorussia.setOwner(russians);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 5);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russian_easternEurope.isSatisfied(null, bridge));
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
  // - isConstruction, constructionType, constructionsPerTerrPerTypePerTurn, maxConstructionsPerTypePerTerr,
  // - "More Constructions with Factory", "More Constructions with Factory", "Unlimited Constructions"
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
