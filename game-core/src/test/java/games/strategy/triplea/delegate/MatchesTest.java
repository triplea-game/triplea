package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.xml.TestMapGameData;

public final class MatchesTest {
  private static final Object VALUE = new Object();

  private static <T> Matcher<Predicate<T>> matches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<Predicate<T>>() {
      @Override
      public void describeTo(final Description description) {
        description.appendText("matcher matches using ").appendValue(value);
      }

      @Override
      public boolean matchesSafely(final Predicate<T> match, final Description description) {
        if (!match.test(value)) {
          description.appendText("it does not match");
          return false;
        }
        return true;
      }
    };
  }

  private static <T> Matcher<Predicate<T>> notMatches(final @Nullable T value) {
    return new TypeSafeDiagnosingMatcher<Predicate<T>>() {
      @Override
      public void describeTo(final Description description) {
        description.appendText("matcher does not match using ").appendValue(value);
      }

      @Override
      public boolean matchesSafely(final Predicate<T> match, final Description description) {
        if (match.test(value)) {
          description.appendText("it matches");
          return false;
        }
        return true;
      }
    };
  }

  @Test
  public void testAlways() {
    assertTrue(Matches.always().test(VALUE));
  }

  @Test
  public void testNever() {
    assertFalse(Matches.never().test(VALUE));
  }

  @Nested
  public final class TerritoryHasEnemyUnitsThatCanCaptureItAndIsOwnedByTheirEnemyTest {
    private GameData gameData;
    private PlayerID player;
    private PlayerID alliedPlayer;
    private PlayerID enemyPlayer;
    private Territory territory;

    private Predicate<Territory> newMatch() {
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

    private Unit newSeaUnitFor(final PlayerID player) {
      return GameDataTestUtil.battleship(gameData).create(player);
    }

    @BeforeEach
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
    public void shouldMatchWhenTerritoryContainsEnemyLandUnits() {
      territory.getUnits().addAll(Arrays.asList(
          newLandUnitFor(player),
          newLandUnitFor(enemyPlayer),
          newAirUnitFor(enemyPlayer),
          newInfrastructureUnitFor(enemyPlayer)));

      assertThat(newMatch(), matches(territory));
    }

    @Test
    public void shouldMatchWhenTerritoryContainsEnemySeaUnits() {
      territory.getUnits().addAll(Arrays.asList(
          newSeaUnitFor(player),
          newSeaUnitFor(enemyPlayer),
          newAirUnitFor(enemyPlayer),
          newInfrastructureUnitFor(enemyPlayer)));

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

  @Nested
  public final class TerritoryIsNotUnownedWaterTest {
    private GameData gameData;
    private PlayerID player;
    private Territory landTerritory;
    private Territory seaTerritory;

    private Predicate<Territory> newMatch() {
      return Matches.territoryIsNotUnownedWater();
    }

    @BeforeEach
    public void setUp() throws Exception {
      gameData = TestMapGameData.DELEGATE_TEST.getGameData();

      player = GameDataTestUtil.germans(gameData);

      landTerritory = gameData.getMap().getTerritory("Germany");
      landTerritory.setOwner(player);
      assertThat(TerritoryAttachment.get(landTerritory), is(notNullValue()));

      seaTerritory = gameData.getMap().getTerritory("Baltic Sea Zone");
      seaTerritory.setOwner(player);
      assertThat(TerritoryAttachment.get(seaTerritory), is(nullValue()));
      TerritoryAttachment.add(seaTerritory, new TerritoryAttachment("name", seaTerritory, gameData));
      assertThat(TerritoryAttachment.get(seaTerritory), is(notNullValue()));
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsUnownedAndHasTerritoryAttachment() {
      landTerritory.setOwner(PlayerID.NULL_PLAYERID);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenLandTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      landTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(landTerritory);

      assertThat(newMatch(), matches(landTerritory));
    }

    @Test
    public void shouldMatchWhenSeaTerritoryIsOwnedAndHasTerritoryAttachment() {
      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    public void shouldMatchWhenSeaTerritoryIsOwnedAndDoesNotHaveTerritoryAttachment() {
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    public void shouldMatchWhenSeaTerritoryIsUnownedAndHasTerritoryAttachment() {
      seaTerritory.setOwner(PlayerID.NULL_PLAYERID);

      assertThat(newMatch(), matches(seaTerritory));
    }

    @Test
    public void shouldNotMatchWhenSeaTerritoryIsUnownedAndDoesNotHaveTerritoryAttachment() {
      seaTerritory.setOwner(PlayerID.NULL_PLAYERID);
      TerritoryAttachment.remove(seaTerritory);

      assertThat(newMatch(), notMatches(seaTerritory));
    }
  }
}
