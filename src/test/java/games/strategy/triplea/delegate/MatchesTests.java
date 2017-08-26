package games.strategy.triplea.delegate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import javax.annotation.Nullable;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
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
  private static <T> Matcher<Match<T>> matches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<Match<T>>() {
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
    };
  }

  private static <T> Matcher<Match<T>> notMatches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<Match<T>>() {
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
    };
  }

  public static final class TerritoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemyTest {
    private GameData gameData;
    private PlayerID player;
    private PlayerID alliedPlayer;
    private PlayerID enemyPlayer;
    private Territory territory;

    private Match<Territory> newMatch() {
      return Matches.territoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemy(player, gameData);
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

    @Before
    public void setUp() throws Exception {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);
      alliedPlayer = GameDataTestUtil.japanese(gameData);
      assertThat(gameData.getRelationshipTracker().isAtWar(player, alliedPlayer), is(false));
      enemyPlayer = GameDataTestUtil.russians(gameData);
      assertThat(gameData.getRelationshipTracker().isAtWar(player, enemyPlayer), is(true));

      territory = gameData.getMap().getTerritory("Germany");
      territory.setOwner(player);
      territory.getUnits().clear();
    }

    @Test
    public void shouldNotMatchWhenTerritoryContainsOnlyAlliedLandUnits() {
      territory.getUnits().add(newLandUnitFor(alliedPlayer));

      assertThat(newMatch(), notMatches(territory));
    }

    @Test
    public void shouldMatchWhenTerritoryContainsOnlyEnemyLandUnits() {
      territory.getUnits().add(newLandUnitFor(enemyPlayer));

      assertThat(newMatch(), matches(territory));
    }

    @Test
    public void shouldNotMatchWhenTerritoryContainsOnlyEnemyAirUnits() {
      territory.getUnits().add(newAirUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(territory));
    }

    @Test
    public void shouldNotMatchWhenTerritoryContainsOnlyEnemyInfrastructureUnits() {
      territory.getUnits().add(newInfrastructureUnitFor(enemyPlayer));

      assertThat(newMatch(), notMatches(territory));
    }
  }

  public static final class TerritoryIsNotUnownedWaterTest {
    private GameData gameData;
    private PlayerID player;
    private Territory landTerritory;
    private Territory seaTerritory;

    private static Match<Territory> newMatch() {
      return Matches.territoryIsNotUnownedWater();
    }

    @Before
    public void setUp() throws Exception {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);

      landTerritory = gameData.getMap().getTerritory("Germany");
      seaTerritory = gameData.getMap().getTerritory("Baltic Sea Zone");
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsOwned() {
      landTerritory.setOwner(player);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsUnowned() {
      landTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenSeaTerritoryIsOwned() {
      seaTerritory.setOwner(player);

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryIsUnowned() {
      seaTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch(), notMatches(seaTerritory));
    }
  }
}
