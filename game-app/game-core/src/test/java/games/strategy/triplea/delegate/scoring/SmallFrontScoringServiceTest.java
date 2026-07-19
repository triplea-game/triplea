package games.strategy.triplea.delegate.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.SmallFrontScoringAttachment;
import games.strategy.triplea.attachments.SupplyTerritoryAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.supply.SupplyNetworkResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Scores the rubric the map's README defines: a point per objective held, a German point for
 * supplied occupation of Bastogne or Neufchateau, an American point for keeping supplied Germans
 * out of the west, and ties to the Americans.
 */
class SmallFrontScoringServiceTest {
  private final GameData data = new GameData();
  private final GamePlayer germans = new GamePlayer("Germans", data);
  private final GamePlayer americans = new GamePlayer("Americans", data);

  // Objectives, east to west.
  private final Territory stVith = new Territory("St. Vith", data);
  private final Territory bastogne = new Territory("Bastogne", data);
  private final Territory neufchateau = new Territory("Neufchateau", data);
  // The German rear supply source, and the west the Americans want kept clear.
  private final Territory prum = new Territory("Prum", data);
  private final Territory marche = new Territory("Marche", data);

  private final UnitType infantry = new UnitType("infantry", data);

  private SmallFrontScoringAttachment germanScoring;
  private SmallFrontScoringAttachment americanScoring;

  @BeforeEach
  void setUp() throws Exception {
    data.getPlayerList().addPlayerId(germans);
    data.getPlayerList().addPlayerId(americans);
    for (final Territory territory :
        new Territory[] {prum, stVith, bastogne, neufchateau, marche}) {
      data.getMap().addTerritory(territory);
      territory.setOwner(americans);
    }
    prum.setOwner(germans);
    data.getRelationshipTracker().setSelfRelations();
    data.getRelationshipTracker().setNullPlayerRelations();
    data.getRelationshipTracker()
        .setRelationship(
            germans, americans, data.getRelationshipTypeList().getDefaultWarRelationship());

    // A default UnitAttachment is neither air nor sea, which is what makes the unit count as land.
    infantry.addAttachment(
        Constants.UNIT_ATTACHMENT_NAME,
        new UnitAttachment(Constants.UNIT_ATTACHMENT_NAME, infantry, data));
    data.getUnitTypeList().addUnitType(infantry);

    // Every objective is worth one point; Prum and Marche are not objectives.
    for (final Territory objective : new Territory[] {stVith, bastogne, neufchateau}) {
      victoryCity(objective);
    }
    territoryAttachment(prum);
    territoryAttachment(marche);

    // Supply runs from Prum along a road through every territory, so occupation alone decides
    // whether a unit counts as supplied.
    data.getProperties().set(SupplyNetworkResolver.SUPPLY_NETWORK_ENABLED, true);
    final SupplyTerritoryAttachment prumSupply = supply(prum);
    prumSupply.setSupplySource("true");
    prumSupply.setRoadConnection("St. Vith");
    supply(stVith).setRoadConnection("Bastogne");
    supply(bastogne).setRoadConnection("Neufchateau");
    supply(neufchateau).setRoadConnection("Marche");
    supply(marche);

    germanScoring = scoring(germans);
    germanScoring.setPointsPerObjective("1");
    germanScoring.setSuppliedOccupationBonus("1:Bastogne:Neufchateau");

    americanScoring = scoring(americans);
    americanScoring.setPointsPerObjective("1");
    americanScoring.setEnemyAbsentBonus("1:Marche");
    americanScoring.setWinsTies("true");
  }

  @Test
  void americansHoldingEverythingScoreObjectivesPlusTheClearWestBonus() {
    assertThat(SmallFrontScoringService.score(data))
        .containsEntry(americans, 4)
        .containsEntry(germans, 0);
    assertThat(SmallFrontScoringService.winner(data)).contains(americans);
  }

  @Test
  void capturingAnObjectiveMovesItsPointAcross() {
    stVith.setOwner(germans);

    assertThat(SmallFrontScoringService.score(data))
        .containsEntry(americans, 3)
        .containsEntry(germans, 1);
  }

  @Test
  void suppliedGermansInBastogneEarnTheOccupationBonus() {
    stVith.setOwner(germans);
    bastogne.setOwner(germans);
    bastogne.getUnitCollection().addAll(infantry.create(1, germans));

    // Two objectives plus the occupation bonus.
    assertThat(SmallFrontScoringService.score(data)).containsEntry(germans, 3);
  }

  @Test
  void unsuppliedGermansInBastogneEarnNoOccupationBonus() {
    // St. Vith stays American, which cuts the road from Prum before it reaches Bastogne.
    bastogne.setOwner(germans);
    bastogne.getUnitCollection().addAll(infantry.create(1, germans));

    assertThat(SupplyNetworkResolver.isSupplied(bastogne, germans, data)).isFalse();
    assertThat(SmallFrontScoringService.score(data)).containsEntry(germans, 1);
  }

  @Test
  void suppliedGermansInTheWestDenyTheAmericanBonus() {
    for (final Territory territory : new Territory[] {stVith, bastogne, neufchateau, marche}) {
      territory.setOwner(germans);
    }
    bastogne.getUnitCollection().addAll(infantry.create(1, germans));
    marche.getUnitCollection().addAll(infantry.create(1, germans));

    // Three objectives plus the occupation bonus for Bastogne; the Americans hold nothing and the
    // supplied German in Marche costs them their clear-west point.
    assertThat(SmallFrontScoringService.score(data))
        .containsEntry(germans, 4)
        .containsEntry(americans, 0);
    assertThat(SmallFrontScoringService.winner(data)).contains(germans);
  }

  @Test
  void tiesGoToThePlayerThatDeclaresWinsTies() {
    // Germans take two objectives and hold no unit anywhere, so they earn no occupation bonus.
    // The Americans keep Neufchateau and a clear west, which levels the score at two apiece.
    stVith.setOwner(germans);
    bastogne.setOwner(germans);

    assertThat(SmallFrontScoringService.score(data))
        .containsEntry(germans, 2)
        .containsEntry(americans, 2);
    assertThat(SmallFrontScoringService.winner(data)).contains(americans);
  }

  @Test
  void playersWithoutAScoringAttachmentAreNotScored() {
    final GameData bare = new GameData();
    final GamePlayer lonely = new GamePlayer("Lonely", bare);
    bare.getPlayerList().addPlayerId(lonely);

    assertThat(SmallFrontScoringService.score(bare)).isEmpty();
    assertThat(SmallFrontScoringService.winner(bare)).isEmpty();
  }

  @Test
  void autoTerminationIsOffAndScoringRoundIsEightUnlessTheMapSaysOtherwise() {
    assertThat(SmallFrontScoringService.isAutoTerminationEnabled(data)).isFalse();
    assertThat(SmallFrontScoringService.getScoringRound(data)).isEqualTo(8);

    data.getProperties().set(SmallFrontScoringService.AUTO_TERMINATION, true);
    data.getProperties().set(SmallFrontScoringService.SCORING_ROUND, 3);

    assertThat(SmallFrontScoringService.isAutoTerminationEnabled(data)).isTrue();
    assertThat(SmallFrontScoringService.getScoringRound(data)).isEqualTo(3);
  }

  private SmallFrontScoringAttachment scoring(final GamePlayer player) {
    final SmallFrontScoringAttachment attachment =
        new SmallFrontScoringAttachment("scoringAttachment", player, data);
    player.addAttachment("scoringAttachment", attachment);
    return attachment;
  }

  private SupplyTerritoryAttachment supply(final Territory territory) {
    final SupplyTerritoryAttachment attachment =
        new SupplyTerritoryAttachment("supplyAttachment", territory, data);
    territory.addAttachment("supplyAttachment", attachment);
    return attachment;
  }

  private TerritoryAttachment territoryAttachment(final Territory territory) {
    final TerritoryAttachment attachment =
        new TerritoryAttachment(Constants.TERRITORY_ATTACHMENT_NAME, territory, data);
    territory.addAttachment(Constants.TERRITORY_ATTACHMENT_NAME, attachment);
    return attachment;
  }

  /** TerritoryAttachment keeps its victoryCity setter private, so go through the XML property. */
  private void victoryCity(final Territory territory) throws Exception {
    territoryAttachment(territory).getPropertyOrEmpty("victoryCity").orElseThrow().setValue("1");
  }
}
