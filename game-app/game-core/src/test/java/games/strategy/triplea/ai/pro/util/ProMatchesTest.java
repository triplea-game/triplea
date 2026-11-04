package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.addTo;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.assertj.core.api.Assertions.assertThat;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProMatchesTest {
  private final GameData data = TestMapGameData.DELEGATE_TEST.getGameData();
  // West Canada is a marsh, whose territory effect bars armour.
  private final Territory westCanada = data.getMap().getTerritoryOrThrow("West Canada");
  private final Territory eastCanada = data.getMap().getTerritoryOrThrow("East Canada");

  @Test
  void routeIntoTerritoryThatBarsTheUnitTypeIsNotAllowed() {
    final Unit tank = armour(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(eastCanada, westCanada), tank)).isFalse();
  }

  @Test
  void routeIntoTerritoryThatBarsOtherUnitTypesIsAllowed() {
    final Unit infantry = infantry(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(eastCanada, westCanada), infantry)).isTrue();
  }

  @Test
  void routeOutOfTerritoryThatBarsTheUnitTypeIsAllowed() {
    final Unit tank = armour(data).create(1, british(data)).get(0);

    assertThat(ProMatches.routeAllowsUnit(new Route(westCanada, eastCanada), tank)).isTrue();
  }

  @Test
  void trainCannotMoveBetweenTerritoriesWithoutRail() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin, easternGermany), train))
        .isFalse();
  }

  @Test
  void trainCanMoveBetweenTerritoriesThatBothHoldRail() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Territory easternGermany = tww.getMap().getTerritoryOrThrow("Eastern Germany");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);
    addTo(berlin, GameDataTestUtil.germanRail(tww).create(1, germans));
    addTo(easternGermany, GameDataTestUtil.germanRail(tww).create(1, germans));

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin, easternGermany), train))
        .isTrue();
  }

  @Test
  void trainThatStaysWhereThereIsNoRailIsNotBlocked() {
    final GameData tww = TestMapGameData.TWW.getGameData();
    final GamePlayer germans = GameDataTestUtil.germany(tww);
    final Territory berlin = tww.getMap().getTerritoryOrThrow("Berlin");
    final Unit train = GameDataTestUtil.germanTrain(tww).create(1, germans).get(0);

    assertThat(ProMatches.routeHasRequiredUnitsToMove(new Route(berlin), train)).isTrue();
  }

  @Test
  void unitDamagedPastItsOperationalLimitCannotBeMoved() throws Exception {
    final GameData ww2v3 = TestMapGameData.WW2V3_1941.getGameData();
    ww2v3
        .getProperties()
        .set(Constants.DAMAGE_FROM_BOMBING_DONE_TO_UNITS_INSTEAD_OF_TERRITORIES, true);
    final var attachment = GameDataTestUtil.battleship(ww2v3).getUnitAttachment();
    attachment.getPropertyOrThrow("canBeDamaged").setValue(true);
    attachment.getPropertyOrThrow("maxDamage").setValue(4);
    attachment.getPropertyOrThrow("maxOperationalDamage").setValue(2);
    final GamePlayer british = GameDataTestUtil.british(ww2v3);
    final Unit battleship = GameDataTestUtil.battleship(ww2v3).create(1, british).get(0);

    battleship.setUnitDamage(2);
    assertThat(ProMatches.unitCanBeMovedAndIsOwned(british).test(battleship)).isTrue();

    battleship.setUnitDamage(3);
    assertThat(ProMatches.unitCanBeMovedAndIsOwned(british).test(battleship)).isFalse();
  }

  private final GameData transportData = TestMapGameData.WW2V3_1941.getGameData();
  private final Unit transport = transport(transportData).create(germans(transportData));

  private void startStep(final String step) {
    transportData.getSequence().setRoundAndStep(1, step, germans(transportData));
  }

  private void markFoughtInCombat() throws MutableProperty.InvalidValueException {
    transport.getProperty(Unit.PropertyName.WAS_IN_COMBAT).orElseThrow().setValue(true);
  }

  @Test
  void transportThatFoughtCannotLoadInNonCombat() throws MutableProperty.InvalidValueException {
    startStep("Non Combat Move");
    markFoughtInCombat();

    assertThat(ProMatches.transportCanLoad().test(transport)).isFalse();
  }

  @Test
  void transportThatDidNotFightCanLoadInNonCombat() {
    startStep("Non Combat Move");

    assertThat(ProMatches.transportCanLoad().test(transport)).isTrue();
  }

  @Test
  void transportCanLoadInCombatMoveEvenIfFlaggedAsFought()
      throws MutableProperty.InvalidValueException {
    startStep("germanCombatMove");
    markFoughtInCombat();

    assertThat(ProMatches.transportCanLoad().test(transport)).isTrue();
  }
}
