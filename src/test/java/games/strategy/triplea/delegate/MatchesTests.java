package games.strategy.triplea.delegate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.annotation.Nullable;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.Match;

@RunWith(Enclosed.class)
public final class MatchesTests {
  public static final class TerritoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemyAndIsNotUnownedWaterTest {
    private GameData gameData;
    private PlayerID player;
    private PlayerID alliedPlayer;
    private PlayerID enemyPlayer;
    private Territory landTerritory;
    private Territory seaTerritory;

    private Match<Territory> newMatch() {
      return Matches.territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemyAndIsNotUnownedWater(player, gameData);
    }

    private Unit newAirUnitFor(final PlayerID player) {
      return GameDataTestUtil.fighter(gameData).create(player);
    }

    private Unit newInfrastructureUnitFor(final PlayerID player) {
      return GameDataTestUtil.aaGun(gameData).create(player);
    }

    private Unit newLandUnitFor(final PlayerID player) {
      return GameDataTestUtil.infantry(gameData).create(player);
    }

    private Unit newSeaUnitFor(final PlayerID player) {
      return GameDataTestUtil.battleship(gameData).create(player);
    }

    @Before
    public void setUp() throws Exception {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);
      alliedPlayer = GameDataTestUtil.japanese(gameData);
      assertThat(gameData.getRelationshipTracker().isAtWar(player, alliedPlayer), is(false));
      enemyPlayer = GameDataTestUtil.russians(gameData);
      assertThat(gameData.getRelationshipTracker().isAtWar(player, enemyPlayer), is(true));

      landTerritory = gameData.getMap().getTerritory("Germany");
      landTerritory.setOwner(player);
      landTerritory.getUnits().clear();

      seaTerritory = gameData.getMap().getTerritory("Baltic Sea Zone");
      seaTerritory.setOwner(player);
      seaTerritory.getUnits().clear();
    }

    @Ignore("territory owner cannot be set to null")
    @Test
    public void shouldNotMatchWhenTerritoryOwnerIsNull() {
      landTerritory.setOwner(null);
      landTerritory.getUnits().add(newLandUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(landTerritory));
    }

    @Test
    public void shouldNotMatchWhenLandTerritoryContainsOnlyAlliedLandUnits() {
      landTerritory.getUnits().add(newLandUnitFor(alliedPlayer));

      assertThat(newMatch(), notMatches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryContainsEnemyLandUnits() {
      landTerritory.getUnits().add(newLandUnitFor(enemyPlayer));

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryContainsEnemyLandUnitsAndIsUnowned() {
      landTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(landTerritory);
      landTerritory.getUnits().add(newLandUnitFor(enemyPlayer));

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldNotMatchWhenLandTerritoryContainsOnlyEnemyAirUnits() {
      landTerritory.getUnits().add(newAirUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(landTerritory));
    }

    @Test
    public void shouldNotMatchWhenLandTerritoryContainsOnlyEnemyInfrastructureUnits() {
      landTerritory.getUnits().add(newInfrastructureUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(landTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryContainsOnlyAlliedSeaUnits() {
      seaTerritory.getUnits().add(newSeaUnitFor(alliedPlayer));

      assertThat(newMatch(), notMatches(seaTerritory));
    }

    @Test
    public void shouldMatchWhenSeaTerritoryContainsEnemySeaUnits() {
      seaTerritory.getUnits().add(newSeaUnitFor(enemyPlayer));

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryContainsEnemySeaUnitsAndIsUnowned() {
      seaTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(seaTerritory);
      seaTerritory.getUnits().add(newSeaUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(seaTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryContainsOnlyEnemyAirUnits() {
      seaTerritory.getUnits().add(newAirUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(seaTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryContainsOnlyEnemyInfrastructureUnits() {
      seaTerritory.getUnits().add(newInfrastructureUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(seaTerritory));
    }
  }

  private static <T> Matcher<Match<T>> matches(final @Nullable T value) {
    return new IsMatch<>(value);
  }

  private static final class IsMatch<T> extends TypeSafeDiagnosingMatcher<Match<T>> {
    private final @Nullable T value;

    private IsMatch(final @Nullable T value) {
      this.value = value;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matcher matches using ").appendValue(value);
    }

    @Override
    public boolean matchesSafely(final Match<T> match, final Description description) {
      if (!match.match(value)) {
        description.appendText("it does not match");
        return false;
      }
      return true;
    }
  }

  private static <T> Matcher<Match<T>> notMatches(final @Nullable T value) {
    return new IsNotMatch<>(value);
  }

  private static final class IsNotMatch<T> extends TypeSafeDiagnosingMatcher<Match<T>> {
    private final @Nullable T value;

    private IsNotMatch(final @Nullable T value) {
      this.value = value;
    }

    @Override
    public void describeTo(final Description description) {
      description.appendText("matcher does not match using ").appendValue(value);
    }

    @Override
    public boolean matchesSafely(final Match<T> match, final Description description) {
      if (match.match(value)) {
        description.appendText("it matches");
        return false;
      }
      return true;
    }
  }
}
