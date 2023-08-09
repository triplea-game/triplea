package games.strategy.triplea.delegate.data;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CasualtyDetailsTest {

  private final GameData gameData = givenGameData().build();
  private final GamePlayer player1 = new GamePlayer("player1", gameData);
  private final GamePlayer player2 = new GamePlayer("player2", gameData);

  private UnitType givenUnitType(final String name) {
    final UnitType unitType = new UnitType(name, gameData);
    final UnitAttachment unitAttachment = new UnitAttachment(name, unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  @Test
  void ignoreNonAirUnitsAlreadyKilled() {
    final UnitType infantry = givenUnitType("infantry");

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player1));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(killed, List.of(), true);
    casualtyDetails.ensureUnitsAreKilledFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft));
    assertThat(
        "The infantry should still be killed",
        casualtyDetails.getKilled(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void ignoreNonAirUnitsAlreadyDamaged() {
    final UnitType infantry = givenUnitType("infantry");

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player1));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(List.of(), damaged, true);
    casualtyDetails.ensureUnitsAreDamagedFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft).reversed());
    assertThat(
        "The infantry should still be damaged",
        casualtyDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void killLowestMovementAirUnitsWhenOnlyOneTypeIsAvailable() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setMovement(4);
    fighter.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(killed, List.of(), true);
    casualtyDetails.ensureUnitsAreKilledFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft));
    assertThat(
        "The second air unit has no movement left so it should be killed",
        casualtyDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }

  @Test
  void damageHighestMovementAirUnitsWhenOnlyOneTypeIsAvailable() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setHitPoints(2);
    fighter.getUnitAttachment().setMovement(4);
    fighter.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(List.of(), damaged, true);
    casualtyDetails.ensureUnitsAreDamagedFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft).reversed());

    assertThat(
        "The first air unit has one movement left so it should be damaged",
        casualtyDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void killLowestMovementAirUnitsInTwoTypes() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setMovement(4);
    fighter.getUnitAttachment().setIsAir(true);
    final UnitType bomber = givenUnitType("bomber");
    bomber.getUnitAttachment().setMovement(6);
    bomber.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));
    units.addAll(bomber.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    units.get(2).setAlreadyMoved(BigDecimal.ONE);
    units.get(3).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));
    killed.add(units.get(2));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(killed, List.of(), true);
    casualtyDetails.ensureUnitsAreKilledFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft));

    assertThat(
        "The second and fourth air unit have no movement left so it should be killed",
        casualtyDetails.getKilled(),
        is(containsInAnyOrder(units.get(1), units.get(3))));
  }

  @Test
  void damageHighestMovementAirUnitsInTwoTypes() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setHitPoints(2);
    fighter.getUnitAttachment().setMovement(4);
    fighter.getUnitAttachment().setIsAir(true);
    final UnitType bomber = givenUnitType("bomber");
    bomber.getUnitAttachment().setHitPoints(3);
    bomber.getUnitAttachment().setMovement(6);
    bomber.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));
    units.addAll(bomber.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    units.get(2).setAlreadyMoved(BigDecimal.ONE);
    units.get(3).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));
    damaged.add(units.get(2));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(List.of(), damaged, true);
    casualtyDetails.ensureUnitsAreDamagedFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft).reversed());

    assertThat(
        "The first and third air unit have one movement left so they should be damaged",
        casualtyDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0), units.get(2))));
  }

  @Test
  void damageHighestMovementAirUnitsWithTwoOwners() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setHitPoints(2);
    fighter.getUnitAttachment().setMovement(3);
    fighter.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));
    units.addAll(fighter.createTemp(1, player2));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));
    units.get(2).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));
    damaged.add(units.get(1));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(List.of(), damaged, true);
    casualtyDetails.ensureUnitsAreDamagedFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft).reversed());

    assertThat(
        "Damage is not distributed to a unit of another owner",
        casualtyDetails.getDamaged(),
        is(containsInAnyOrder(units.get(1), units.get(0))));
  }

  @Test
  void keepDamageAtUnitsAlreadyDamaged() {
    final UnitType fighter = givenUnitType("fighter");
    fighter.getUnitAttachment().setHitPoints(2);
    fighter.getUnitAttachment().setMovement(4);
    fighter.getUnitAttachment().setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player1));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));
    units.get(0).setHits(1);

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(1));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(List.of(), damaged, true);
    casualtyDetails.ensureUnitsAreDamagedFirst(
        units, Matches.unitIsAir(), Comparator.comparing(Unit::getMovementLeft).reversed());

    assertThat(
        "The first and third air unit have one movement left so they should be damaged",
        casualtyDetails.getDamaged().size() == 1
            && casualtyDetails.getDamaged().contains(units.get(1))
            && units.get(0).getHits() == 1
            && units.get(1).getHits() == 0);
  }

  @Test
  void killPositiveMarineBonusLastIfAmphibious() {
    final UnitType infantry = givenUnitType("infantry");
    infantry.getUnitAttachment().setIsMarine(1);

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player1));

    units.get(0).setWasAmphibious(true);
    units.get(1).setWasAmphibious(false);

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(killed, List.of(), true);
    casualtyDetails.ensureUnitsWithPositiveMarineBonusAreKilledLast(units);
    assertThat(
        "The second unit was not amphibious, so it doesn't have the positive marine bonus "
            + "and should be taken first.",
        casualtyDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }

  @Test
  void killNegativeMarineBonusFirstIfAmphibious() {
    final UnitType infantry = givenUnitType("infantry");
    infantry.getUnitAttachment().setIsMarine(-1);

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player1));

    units.get(0).setWasAmphibious(false);
    units.get(1).setWasAmphibious(true);

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails casualtyDetails = new CasualtyDetails(killed, List.of(), true);
    casualtyDetails.ensureUnitsWithPositiveMarineBonusAreKilledLast(units);
    assertThat(
        "The second unit was amphibious, so it has the negative marine bonus "
            + "and should be taken first.",
        casualtyDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }
}
