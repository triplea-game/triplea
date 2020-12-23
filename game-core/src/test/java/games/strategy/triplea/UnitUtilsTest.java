package games.strategy.triplea;

import static games.strategy.triplea.delegate.GameDataTestUtil.germans;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnitUtilsTest {

  private final GameData gameData = TestMapGameData.WW2V3_1942.getGameData();
  private final GamePlayer player = germans(gameData);
  private final Territory seaZone = territory("13 Sea Zone", gameData);
  private final Territory landZone = territory("Germany", gameData);
  private final UnitType battleship = GameDataTestUtil.battleship(gameData);
  private final UnitType factory = GameDataTestUtil.factory(gameData);
  private final UnitType transport = GameDataTestUtil.transport(gameData);
  private final UnitType infantry = GameDataTestUtil.infantry(gameData);

  @Nested
  class TranslateAttributesToOtherUnits {

    @Test
    void noChangesIfNothingToTransfer() {
      final Unit oldUnit = battleship.create(1, player).get(0);
      final Collection<Unit> newUnits = battleship.create(1, player);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, seaZone);

      assertThat(changes.isEmpty(), is(true));
    }

    @Test
    void hitsAreTransferred() {
      final Unit oldUnit = battleship.create(1, player).get(0);
      final List<Unit> newUnits = battleship.create(1, player);
      oldUnit.setHits(1);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, seaZone);
      gameData.performChange(changes);

      assertThat(newUnits.get(0).getHits(), is(1));
    }

    @Test
    void hitsAreTransferredToAllUnits() {
      final Unit oldUnit = battleship.create(1, player).get(0);
      final List<Unit> newUnits = battleship.create(2, player);
      oldUnit.setHits(1);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, seaZone);
      gameData.performChange(changes);

      assertThat(newUnits.get(0).getHits(), is(1));
      assertThat(newUnits.get(1).getHits(), is(1));
    }

    @Test
    void newUnitHasAtLeast1HP() {
      final Unit oldUnit = battleship.create(1, player).get(0);
      final List<Unit> newUnits = battleship.create(1, player);
      oldUnit.setHits(2);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, seaZone);
      gameData.performChange(changes);

      assertThat(
          "The new unit will always have at least 1 HP, even if the number of hits to transfer "
              + "would have set it to 0 HP.",
          newUnits.get(0).getHits(),
          is(1));
    }

    @Test
    void bombingDamageIsTransferred() {
      final Unit oldUnit = factory.create(1, player).get(0);
      final List<Unit> newUnits = factory.create(1, player);
      oldUnit.setUnitDamage(5);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, landZone);
      gameData.performChange(changes);

      assertThat(newUnits.get(0).getUnitDamage(), is(5));
    }

    @Test
    void bombingDamageIsLimited() {
      final Unit oldUnit = factory.create(1, player).get(0);
      final List<Unit> newUnits = factory.create(1, player);
      // force a really large amount of damage
      oldUnit.setUnitDamage(100);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, landZone);
      gameData.performChange(changes);

      assertThat(
          "Factory can only have 20 damage in Germany", newUnits.get(0).getUnitDamage(), is(20));
    }

    @Test
    void bombingDamageIsNotTransferredIfDamageAllowedIs0() {
      final Unit oldUnit = factory.create(1, player).get(0);
      final List<Unit> newUnits = factory.create(1, player);
      oldUnit.setUnitDamage(5);

      final Change changes = UnitUtils.translateAttributesToOtherUnits(oldUnit, newUnits, seaZone);
      gameData.performChange(changes);

      assertThat(
          "A factory in a sea zone can't produce anything so it can't be damaged. "
              + "This is a forced situation.",
          newUnits.get(0).getUnitDamage(),
          is(0));
    }

    @Test
    void unloadedUnitsAreTransferredFromOldUnitToNewUnit() {
      final Unit oldTransport = transport.create(1, player).get(0);
      final Unit newTransport = transport.create(1, player).get(0);

      final List<Unit> unloadedInfantry = infantry.create(1, player);
      oldTransport.setUnloaded(unloadedInfantry);

      final Change changes =
          UnitUtils.translateAttributesToOtherUnits(oldTransport, List.of(newTransport), seaZone);
      gameData.performChange(changes);

      assertThat(newTransport.getUnloaded(), is(unloadedInfantry));
    }

    @Test
    void unloadedUnitsAreTransferredToOnlyOneOfTheNewUnits() {
      final Unit oldTransport = transport.create(1, player).get(0);
      final Unit newTransport1 = transport.create(1, player).get(0);
      final Unit newTransport2 = transport.create(1, player).get(0);

      final List<Unit> unloadedInfantry = infantry.create(2, player);
      oldTransport.setUnloaded(unloadedInfantry);

      final Change changes =
          UnitUtils.translateAttributesToOtherUnits(
              oldTransport, List.of(newTransport1, newTransport2), seaZone);
      gameData.performChange(changes);

      assertThat(
          "Units can only be unloaded by one unit at a time. So the unloaded unit "
              + "should be transferred to one of the new units. Since the new units is a list, the "
              + "first one will be selected and the first one is 'newTransport1'",
          newTransport1.getUnloaded(),
          is(unloadedInfantry));
      assertThat(
          "newTransport2 should have no unloaded units because all of the units were assigned to "
              + "newTransport1",
          newTransport2.getUnloaded(),
          is(empty()));
    }

    @Test
    void multipleUnloadedUnitsAreTransferredFromOldUnitToNewUnit() {
      final Unit oldTransport = transport.create(1, player).get(0);
      final Unit newTransport = transport.create(1, player).get(0);

      final List<Unit> unloadedInfantry = infantry.create(5, player);
      oldTransport.setUnloaded(unloadedInfantry);

      final Change changes =
          UnitUtils.translateAttributesToOtherUnits(oldTransport, List.of(newTransport), seaZone);
      gameData.performChange(changes);

      assertThat(
          "All unloaded infantry should be translated to the new transport",
          newTransport.getUnloaded(),
          is(unloadedInfantry));
    }

    @Test
    void transportedUnitsAreTransferred() {
      final Unit oldTransport = transport.create(1, player).get(0);
      seaZone.getUnitCollection().add(oldTransport);
      final Unit newTransport = transport.create(1, player).get(0);

      final List<Unit> transportedUnits = infantry.create(1, player);
      seaZone.getUnitCollection().addAll(transportedUnits);
      transportedUnits.get(0).setTransportedBy(oldTransport);

      final Change changes =
          UnitUtils.translateAttributesToOtherUnits(oldTransport, List.of(newTransport), seaZone);
      gameData.performChange(changes);

      assertThat(transportedUnits.get(0).getTransportedBy(), is(newTransport));
    }

    @Test
    void transportedUnitsAreTransferredToTheFirstNewUnit() {
      final Unit oldTransport = transport.create(1, player).get(0);
      seaZone.getUnitCollection().add(oldTransport);
      final Unit newTransport1 = transport.create(1, player).get(0);
      final Unit newTransport2 = transport.create(1, player).get(0);

      final List<Unit> transportedUnits = infantry.create(1, player);
      seaZone.getUnitCollection().addAll(transportedUnits);
      transportedUnits.get(0).setTransportedBy(oldTransport);

      final Change changes =
          UnitUtils.translateAttributesToOtherUnits(
              oldTransport, List.of(newTransport1, newTransport2), seaZone);
      gameData.performChange(changes);

      assertThat(
          "Units can only be transported by one unit at a time. So the transported unit "
              + "should be transferred to one of the new units. Since the new units is a list, the "
              + "first one will be selected and the first one is 'newTransport1'",
          transportedUnits.get(0).getTransportedBy(),
          is(newTransport1));
    }
  }

  @Test
  void multipleTransportedUnitsAreTransferred() {
    final Unit oldTransport = transport.create(1, player).get(0);
    seaZone.getUnitCollection().add(oldTransport);
    final Unit newTransport = transport.create(1, player).get(0);

    final List<Unit> transportedUnits = infantry.create(5, player);
    seaZone.getUnitCollection().addAll(transportedUnits);
    transportedUnits.forEach(unit -> unit.setTransportedBy(oldTransport));

    final Change changes =
        UnitUtils.translateAttributesToOtherUnits(oldTransport, List.of(newTransport), seaZone);
    gameData.performChange(changes);

    transportedUnits.forEach(unit -> assertThat(unit.getTransportedBy(), is(newTransport)));
  }
}
