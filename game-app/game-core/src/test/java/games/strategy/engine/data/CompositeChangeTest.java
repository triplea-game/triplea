package games.strategy.engine.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.changefactory.ChangeFactory;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompositeChangeTest {

  @Test
  void flattenChanges() {
    final Territory territory = mock(Territory.class);
    final UnitCollection unitCollection = mock(UnitCollection.class);
    when(territory.getUnitCollection()).thenReturn(unitCollection);
    when(unitCollection.getHolder()).thenReturn(territory);

    final CompositeChange change =
        new CompositeChange(
            new CompositeChange(
                new CompositeChange(
                    new CompositeChange(ChangeFactory.removeUnits(territory, List.of()))),
                new CompositeChange(ChangeFactory.moveUnits(territory, territory, List.of()))),
            new CompositeChange(ChangeFactory.addUnits(territory, List.of())));

    final CompositeChange flattenedChange = change.flatten();
    assertThat(change.getChanges())
        .as("Before the flattening, there are only two composite changes in the top change object")
        .hasSize(2);
    assertThat(flattenedChange.getChanges())
        .as(
            "After the flattening, there should be four actual changes: one for the removeUnits, "
                + "one for the addUnits, and two for the moveUnits (since it created a composite "
                + "change with a remove and add unit)")
        .hasSize(4);
  }

  @Test
  void flattenChangesWithNoNesting() {
    final Territory territory = mock(Territory.class);
    final UnitCollection unitCollection = mock(UnitCollection.class);
    when(territory.getUnitCollection()).thenReturn(unitCollection);
    when(unitCollection.getHolder()).thenReturn(territory);

    final CompositeChange change =
        new CompositeChange(
            ChangeFactory.addUnits(territory, List.of()),
            ChangeFactory.removeUnits(territory, List.of()));

    final CompositeChange flattenedChange = change.flatten();
    assertThat(flattenedChange.getChanges())
        .as(
            "Composite change with no child composite changes should have the same list of "
                + "changes after flattening")
        .isEqualTo(change.getChanges());
  }

  @Test
  void flattenChangesWithNestedSingleChange() {
    final Territory territory = mock(Territory.class);
    final UnitCollection unitCollection = mock(UnitCollection.class);
    when(territory.getUnitCollection()).thenReturn(unitCollection);
    when(unitCollection.getHolder()).thenReturn(territory);

    final Change nestedChange = ChangeFactory.removeUnits(territory, List.of());

    final CompositeChange change =
        new CompositeChange(
            new CompositeChange(new CompositeChange(new CompositeChange(nestedChange))));

    final CompositeChange flattenedChange = change.flatten();
    assertThat(flattenedChange.getChanges())
        .as(
            "After flattening, the one real change at the bottom of the nested CompositeChanges "
                + "should be the only change")
        .isEqualTo(List.of(nestedChange));
  }
}
