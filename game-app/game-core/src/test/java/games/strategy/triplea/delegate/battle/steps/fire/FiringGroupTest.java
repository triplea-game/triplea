package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.Comparator;
import java.util.List;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiringGroupTest {

  @NonNls private static final String GROUP_NAME = "test";

  final GameData gameData = givenGameData().build();
  @Mock GamePlayer player;

  @Test
  public void onlyOneGroupIfNoSuicideOnHit() {
    final List<Unit> units =
        List.of(givenUnit(givenUnitType("type1")), givenUnit(givenUnitType("type2")));
    final List<FiringGroup> groups =
        FiringGroup.groupBySuicideOnHit(GROUP_NAME, units, List.of(mock(Unit.class)));
    assertThat(groups).as("All non-suicide units are in a single group").hasSize(1);
    assertThat(groups.get(0).getDisplayName())
        .as("The group name should have no prefix")
        .isEqualTo(GROUP_NAME);
    assertThat(groups.get(0).getFiringUnits().toArray())
        .as("The group should have all of the units")
        .isEqualTo(units.toArray());
    assertThat(groups.get(0).isSuicideOnHit())
        .as("The group should not be suicide on hit")
        .isFalse();
  }

  private Unit givenUnit(final UnitType unitType) {
    return unitType.createTemp(1, player).get(0);
  }

  private UnitType givenUnitType(final String typeName) {
    final UnitType unitType = new UnitType(typeName, gameData);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    return unitType;
  }

  @Test
  public void onlyOneGroupIfSameTypeAndSuicide() {
    final UnitType unitType = givenUnitType("type");
    ((UnitAttachment) unitType.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final List<Unit> units = List.of(givenUnit(unitType), givenUnit(unitType));
    final List<FiringGroup> groups =
        FiringGroup.groupBySuicideOnHit(GROUP_NAME, units, List.of(mock(Unit.class)));
    assertThat(groups).as("Same unit type should create only one group").hasSize(1);
    assertThat(groups.get(0).getDisplayName())
        .as("The group name should have no prefix")
        .isEqualTo(GROUP_NAME);
    assertThat(groups.get(0).getFiringUnits().toArray())
        .as("The group should have all of the units")
        .isEqualTo(units.toArray());
    assertThat(groups.get(0).isSuicideOnHit()).as("The group should be suicide on hit").isTrue();
  }

  @Test
  public void onlyTwoGroupsIfSomeUnitsAreNotSuicideOnHitAndSomeAreOfTheSameType() {
    final UnitType unitType = givenUnitType("type1");
    ((UnitAttachment) unitType.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final List<Unit> units =
        List.of(
            givenUnit(unitType),
            givenUnit(unitType),
            givenUnit(givenUnitType("type2")),
            givenUnit(givenUnitType("type3")));
    final List<FiringGroup> groups =
        FiringGroup.groupBySuicideOnHit(GROUP_NAME, units, List.of(mock(Unit.class)));
    assertThat(groups).as("There should only be two groups").hasSize(2);
    // ensure the suicide group is last
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(groups.get(0).getDisplayName())
        .as("The non suicide group name should have no prefix")
        .isEqualTo(GROUP_NAME);
    assertThat(groups.get(1).getDisplayName())
        .as("The suicide group name should have a prefix")
        .isEqualTo(GROUP_NAME + " suicide");
    assertThat(groups.get(0).getFiringUnits().toArray())
        .as("The non suicide group should have the non suicide units")
        .isEqualTo(units.subList(2, 4).toArray());
    assertThat(groups.get(1).getFiringUnits().toArray())
        .as("The suicide group should have the suicide units")
        .isEqualTo(units.subList(0, 2).toArray());
    assertThat(groups.get(0).isSuicideOnHit())
        .as("The non suicide group should not be suicide on hit")
        .isFalse();
    assertThat(groups.get(1).isSuicideOnHit())
        .as("The suicide group should be suicide on hit")
        .isTrue();
  }

  @Test
  public void multipleGroupsIfAllSuicideButDifferentType() {
    final UnitType unitType1 = givenUnitType("type1");
    ((UnitAttachment) unitType1.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final UnitType unitType2 = givenUnitType("type2");
    ((UnitAttachment) unitType2.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final List<Unit> units = List.of(givenUnit(unitType1), givenUnit(unitType2));
    final List<FiringGroup> groups =
        FiringGroup.groupBySuicideOnHit(GROUP_NAME, units, List.of(mock(Unit.class)));
    assertThat(groups).as("There should be two groups").hasSize(2);
    // ensure the type2 group is last
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(groups.get(0).isSuicideOnHit()).isTrue();
    assertThat(groups.get(1).isSuicideOnHit()).isTrue();

    assertThat(groups.get(0).getDisplayName())
        .as("Type1 group should have a unique name")
        .isEqualTo(GROUP_NAME + " suicide type1");
    assertThat(groups.get(1).getDisplayName())
        .as("Type2 group should have a unique name")
        .isEqualTo(GROUP_NAME + " suicide type2");
    assertThat(groups.get(0).getFiringUnits().toArray())
        .as("Type1 units should be in a separate group")
        .isEqualTo(units.subList(0, 1).toArray());
    assertThat(groups.get(1).getFiringUnits().toArray())
        .as("Type2 units should be in a separate group")
        .isEqualTo(units.subList(1, 2).toArray());
  }

  @Test
  public void multipleGroupsIfSomeAreNotSuicideAndOthersAreSuicideButDifferentType() {
    final UnitType unitType1 = givenUnitType("type1");
    ((UnitAttachment) unitType1.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final UnitType unitType2 = givenUnitType("type2");
    ((UnitAttachment) unitType2.getAttachment(UNIT_ATTACHMENT_NAME)).setIsSuicideOnHit(true);
    final List<Unit> units =
        List.of(
            givenUnit(unitType1),
            givenUnit(unitType1),
            givenUnit(unitType2),
            givenUnit(unitType2),
            givenUnit(givenUnitType("type3")),
            givenUnit(givenUnitType("type4")));
    final List<FiringGroup> groups =
        FiringGroup.groupBySuicideOnHit(GROUP_NAME, units, List.of(mock(Unit.class)));
    assertThat(groups).as("There should be three groups").hasSize(3);
    // ensure the order is non-suicide -> type1 -> type2
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    // first one is the non-suicide group
    assertThat(groups.get(0).isSuicideOnHit()).isFalse();
    assertThat(groups.get(0).getDisplayName()).isEqualTo(GROUP_NAME);
    assertThat(groups.get(0).getFiringUnits().toArray())
        .as("Non suicide group should have all non suicide units")
        .isEqualTo(units.subList(4, 6).toArray());

    // the other two are suicide groups
    assertThat(groups.get(1).isSuicideOnHit()).isTrue();
    assertThat(groups.get(1).getDisplayName()).isEqualTo(GROUP_NAME + " suicide type1");
    assertThat(groups.get(1).getFiringUnits().toArray())
        .as("All type1 units are in their own group")
        .isEqualTo(units.subList(0, 2).toArray());

    assertThat(groups.get(2).isSuicideOnHit()).isTrue();
    assertThat(groups.get(2).getDisplayName()).isEqualTo(GROUP_NAME + " suicide type2");
    assertThat(groups.get(2).getFiringUnits().toArray())
        .as("All type2 units are in their own group")
        .isEqualTo(units.subList(2, 4).toArray());
  }
}
