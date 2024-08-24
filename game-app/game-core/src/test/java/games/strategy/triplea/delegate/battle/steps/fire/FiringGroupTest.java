package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
    assertThat("All non-suicide units are in a single group", groups, hasSize(1));
    assertThat(
        "The group name should have no prefix", groups.get(0).getDisplayName(), is(GROUP_NAME));
    assertThat(
        "The group should have all of the units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.toArray()));
    assertThat("The group should not be suicide on hit", groups.get(0).isSuicideOnHit(), is(false));
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
    assertThat("Same unit type should create only one group", groups, hasSize(1));
    assertThat(
        "The group name should have no prefix", groups.get(0).getDisplayName(), is(GROUP_NAME));
    assertThat(
        "The group should have all of the units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.toArray()));
    assertThat("The group should be suicide on hit", groups.get(0).isSuicideOnHit(), is(true));
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
    assertThat("There should only be two groups", groups, hasSize(2));
    // ensure the suicide group is last
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(
        "The non suicide group name should have no prefix",
        groups.get(0).getDisplayName(),
        is(GROUP_NAME));
    assertThat(
        "The suicide group name should have a prefix",
        groups.get(1).getDisplayName(),
        is(GROUP_NAME + " suicide"));
    assertThat(
        "The non suicide group should have the non suicide units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.subList(2, 4).toArray()));
    assertThat(
        "The suicide group should have the suicide units",
        groups.get(1).getFiringUnits().toArray(),
        is(units.subList(0, 2).toArray()));
    assertThat(
        "The non suicide group should not be suicide on hit",
        groups.get(0).isSuicideOnHit(),
        is(false));
    assertThat(
        "The suicide group should be suicide on hit", groups.get(1).isSuicideOnHit(), is(true));
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
    assertThat("There should be two groups", groups, hasSize(2));
    // ensure the type2 group is last
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(groups.get(0).isSuicideOnHit(), is(true));
    assertThat(groups.get(1).isSuicideOnHit(), is(true));

    assertThat(
        "Type1 group should have a unique name",
        groups.get(0).getDisplayName(),
        is(GROUP_NAME + " suicide type1"));
    assertThat(
        "Type2 group should have a unique name",
        groups.get(1).getDisplayName(),
        is(GROUP_NAME + " suicide type2"));
    assertThat(
        "Type1 units should be in a separate group",
        groups.get(0).getFiringUnits().toArray(),
        is(units.subList(0, 1).toArray()));
    assertThat(
        "Type2 units should be in a separate group",
        groups.get(1).getFiringUnits().toArray(),
        is(units.subList(1, 2).toArray()));
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
    assertThat("There should be three groups", groups, hasSize(3));
    // ensure the order is non-suicide -> type1 -> type2
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    // first one is the non-suicide group
    assertThat(groups.get(0).isSuicideOnHit(), is(false));
    assertThat(groups.get(0).getDisplayName(), is(GROUP_NAME));
    assertThat(
        "Non suicide group should have all non suicide units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.subList(4, 6).toArray()));

    // the other two are suicide groups
    assertThat(groups.get(1).isSuicideOnHit(), is(true));
    assertThat(groups.get(1).getDisplayName(), is(GROUP_NAME + " suicide type1"));
    assertThat(
        "All type1 units are in their own group",
        groups.get(1).getFiringUnits().toArray(),
        is(units.subList(0, 2).toArray()));

    assertThat(groups.get(2).isSuicideOnHit(), is(true));
    assertThat(groups.get(2).getDisplayName(), is(GROUP_NAME + " suicide type2"));
    assertThat(
        "All type2 units are in their own group",
        groups.get(2).getFiringUnits().toArray(),
        is(units.subList(2, 4).toArray()));
  }
}
