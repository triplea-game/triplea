package games.strategy.triplea.delegate.battle.steps.fire;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.attachments.UnitAttachment;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FiringGroupTest {

  @Mock GameData gameData;
  @Mock GamePlayer player;

  @Test
  public void onlyOneGroupIfNoSuicideOnHit() {
    final List<Unit> units =
        List.of(
            givenUnitWithSuicideOnHit("type1", false), givenUnitWithSuicideOnHit("type2", false));
    final List<FiringGroup> groups = FiringGroup.groupBySuicideOnHit("test", units, List.of());
    assertThat("There should only be one group", groups, hasSize(1));
    assertThat("The group name should have no prefix", groups.get(0).getDisplayName(), is("test"));
    assertThat(
        "The group should have all of the units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.toArray()));
    assertThat("The group should not be suicide on hit", groups.get(0).isSuicideOnHit(), is(false));
  }

  private Unit givenUnitWithSuicideOnHit(final String typeName, final boolean suicideOnHit) {
    final UnitType unitType = new UnitType(typeName, gameData);
    final Unit unit = unitType.create(1, player, true).get(0);
    final UnitAttachment unitAttachment = new UnitAttachment("attachment", unitType, gameData);
    unitType.addAttachment(UNIT_ATTACHMENT_NAME, unitAttachment);
    unitAttachment
        .getProperty("isSuicideOnHit")
        .ifPresent(
            property -> {
              try {
                property.setValue(suicideOnHit);
              } catch (final MutableProperty.InvalidValueException e) {
                // should not happen
              }
            });
    return unit;
  }

  @Test
  public void onlyOneGroupIfSameTypeAndSuicide() {
    final List<Unit> units =
        List.of(givenUnitWithSuicideOnHit("type1", true), givenUnitWithSuicideOnHit("type1", true));
    final List<FiringGroup> groups = FiringGroup.groupBySuicideOnHit("test", units, List.of());
    assertThat("There should only be one group", groups, hasSize(1));
    assertThat("The group name should have no prefix", groups.get(0).getDisplayName(), is("test"));
    assertThat(
        "The group should have all of the units",
        groups.get(0).getFiringUnits().toArray(),
        is(units.toArray()));
    assertThat("The group should be suicide on hit", groups.get(0).isSuicideOnHit(), is(true));
  }

  @Test
  public void onlyTwoGroupsIfSomeUnitsAreNotSuicideOnHitAndSomeAreOfTheSameType() {
    final List<Unit> units =
        List.of(
            givenUnitWithSuicideOnHit("type1", true),
            givenUnitWithSuicideOnHit("type1", true),
            givenUnitWithSuicideOnHit("type2", false),
            givenUnitWithSuicideOnHit("type3", false));
    final List<FiringGroup> groups = FiringGroup.groupBySuicideOnHit("test", units, List.of());
    assertThat("There should only be two groups", groups, hasSize(2));
    // ensure the suicide group is the last entry
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(
        "The non suicide group name should have no prefix",
        groups.get(0).getDisplayName(),
        is("test"));
    assertThat(
        "The suicide group name should have a prefix",
        groups.get(1).getDisplayName(),
        is("test suicide"));
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
    final List<Unit> units =
        List.of(givenUnitWithSuicideOnHit("type1", true), givenUnitWithSuicideOnHit("type2", true));
    final List<FiringGroup> groups = FiringGroup.groupBySuicideOnHit("test", units, List.of());
    assertThat("There should be two groups", groups, hasSize(2));
    // ensure the suicide group is the last entry
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    assertThat(groups.get(0).isSuicideOnHit(), is(true));
    assertThat(groups.get(1).isSuicideOnHit(), is(true));

    assertThat(groups.get(0).getDisplayName(), is("test suicide type1"));
    assertThat(groups.get(1).getDisplayName(), is("test suicide type2"));
    assertThat(groups.get(0).getFiringUnits().toArray(), is(units.subList(0, 1).toArray()));
    assertThat(groups.get(1).getFiringUnits().toArray(), is(units.subList(1, 2).toArray()));
  }

  @Test
  public void multipleGroupsIfSomeAreNotSuicideAndOthersAreSuicideButDifferentType() {
    final List<Unit> units =
        List.of(
            givenUnitWithSuicideOnHit("type1", true),
            givenUnitWithSuicideOnHit("type2", true),
            givenUnitWithSuicideOnHit("type3", false));
    final List<FiringGroup> groups = FiringGroup.groupBySuicideOnHit("test", units, List.of());
    assertThat("There should be three groups", groups, hasSize(3));
    // ensure the suicide group is the last entry
    groups.sort(Comparator.comparing(FiringGroup::getDisplayName));

    // first one is the non-suicide group
    assertThat(groups.get(0).isSuicideOnHit(), is(false));
    assertThat(groups.get(0).getDisplayName(), is("test"));
    assertThat(groups.get(0).getFiringUnits().toArray(), is(units.subList(2, 3).toArray()));

    // the other two are suicide groups
    assertThat(groups.get(1).isSuicideOnHit(), is(true));
    assertThat(groups.get(1).getDisplayName(), is("test suicide type1"));
    assertThat(groups.get(1).getFiringUnits().toArray(), is(units.subList(0, 1).toArray()));

    assertThat(groups.get(2).isSuicideOnHit(), is(true));
    assertThat(groups.get(2).getDisplayName(), is("test suicide type2"));
    assertThat(groups.get(2).getFiringUnits().toArray(), is(units.subList(1, 2).toArray()));
  }
}
