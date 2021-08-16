package games.strategy.triplea.delegate.data;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CasualtyDetailsTest {

  @Mock private GameData gameData;
  @Mock private GamePlayer player;

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
    units.addAll(infantry.createTemp(2, player));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(killed, List.of(), true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);
    assertThat(
        "The infantry should still be killed",
        updatedDetails.getKilled(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void ignoreNonAirUnitsAlreadyDamaged() {
    final UnitType infantry = givenUnitType("infantry");

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(List.of(), damaged, true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);
    assertThat(
        "The infantry should still be damaged",
        updatedDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void killLowestMovementAirUnitsWhenOnlyOneTypeIsAvailable() {
    final UnitType fighter = givenUnitType("fighter");
    UnitAttachment.get(fighter).setMovement(4);
    UnitAttachment.get(fighter).setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(killed, List.of(), true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);
    assertThat(
        "The second air unit has no movement left so it should be killed",
        updatedDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }

  @Test
  void damageHighestMovementAirUnitsWhenOnlyOneTypeIsAvailable() {
    final UnitType fighter = givenUnitType("fighter");
    UnitAttachment.get(fighter).setHitPoints(2);
    UnitAttachment.get(fighter).setMovement(4);
    UnitAttachment.get(fighter).setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(List.of(), damaged, true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);

    assertThat(
        "The first air unit has one movement left so it should be damaged",
        updatedDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0))));
  }

  @Test
  void killLowestMovementAirUnitsInTwoTypes() {
    final UnitType fighter = givenUnitType("fighter");
    UnitAttachment.get(fighter).setMovement(4);
    UnitAttachment.get(fighter).setIsAir(true);
    final UnitType bomber = givenUnitType("bomber");
    UnitAttachment.get(bomber).setMovement(6);
    UnitAttachment.get(bomber).setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player));
    units.addAll(bomber.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    units.get(2).setAlreadyMoved(BigDecimal.ONE);
    units.get(3).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));
    killed.add(units.get(2));

    final CasualtyDetails originalDetails = new CasualtyDetails(killed, List.of(), true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);

    assertThat(
        "The second and fourth air unit have no movement left so it should be killed",
        updatedDetails.getKilled(),
        is(containsInAnyOrder(units.get(1), units.get(3))));
  }

  @Test
  void damageHighestMovementAirUnitsInTwoTypes() {
    final UnitType fighter = givenUnitType("fighter");
    UnitAttachment.get(fighter).setHitPoints(2);
    UnitAttachment.get(fighter).setMovement(4);
    UnitAttachment.get(fighter).setIsAir(true);
    final UnitType bomber = givenUnitType("bomber");
    UnitAttachment.get(bomber).setHitPoints(3);
    UnitAttachment.get(bomber).setMovement(6);
    UnitAttachment.get(bomber).setIsAir(true);

    final List<Unit> units = new ArrayList<>();
    units.addAll(fighter.createTemp(2, player));
    units.addAll(bomber.createTemp(2, player));

    units.get(0).setAlreadyMoved(BigDecimal.ONE);
    units.get(1).setAlreadyMoved(BigDecimal.valueOf(2));

    units.get(2).setAlreadyMoved(BigDecimal.ONE);
    units.get(3).setAlreadyMoved(BigDecimal.valueOf(2));

    final List<Unit> damaged = new ArrayList<>();
    damaged.add(units.get(0));
    damaged.add(units.get(2));

    final CasualtyDetails originalDetails = new CasualtyDetails(List.of(), damaged, true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureAirUnitsWithLessMovementAreTakenFirst(units);

    assertThat(
        "The first and third air unit have one movement left so they should be damaged",
        updatedDetails.getDamaged(),
        is(containsInAnyOrder(units.get(0), units.get(2))));
  }

  @Test
  void killPositiveMarineBonusLastIfAmphibious() {
    final UnitType infantry = givenUnitType("infantry");
    UnitAttachment.get(infantry).setIsMarine(1);

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player));

    units.get(0).setWasAmphibious(true);
    units.get(1).setWasAmphibious(false);

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(killed, List.of(), true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureUnitsWithPositiveMarineBonusAreTakenLast(units);
    assertThat(
        "The second unit was not amphibious, so it doesn't have the positive marine bonus "
            + "and should be taken first.",
        updatedDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }

  @Test
  void killNegativeMarineBonusFirstIfAmphibious() {
    final UnitType infantry = givenUnitType("infantry");
    UnitAttachment.get(infantry).setIsMarine(-1);

    final List<Unit> units = new ArrayList<>();
    units.addAll(infantry.createTemp(2, player));

    units.get(0).setWasAmphibious(false);
    units.get(1).setWasAmphibious(true);

    final List<Unit> killed = new ArrayList<>();
    killed.add(units.get(0));

    final CasualtyDetails originalDetails = new CasualtyDetails(killed, List.of(), true);
    final CasualtyDetails updatedDetails =
        originalDetails.ensureUnitsWithPositiveMarineBonusAreTakenLast(units);
    assertThat(
        "The second unit was amphibious, so it has the negative marine bonus "
            + "and should be taken first.",
        updatedDetails.getKilled(),
        is(containsInAnyOrder(units.get(1))));
  }
}
