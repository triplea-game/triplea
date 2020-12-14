package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
    assertThat(
        "Before the flattening, there are only two composite changes in the top change object",
        change.getChanges(),
        hasSize(2));
    assertThat(
        "After the flattening, there should be four actual changes: one for the removeUnits, "
            + "one for the addUnits, and two for the moveUnits (since it created a composite "
            + "change with a remove and add unit)",
        flattenedChange.getChanges(),
        hasSize(4));
  }

  @Test
  void flattenChangesWithSingleChange() {
    final Territory territory = mock(Territory.class);
    final UnitCollection unitCollection = mock(UnitCollection.class);
    when(territory.getUnitCollection()).thenReturn(unitCollection);
    when(unitCollection.getHolder()).thenReturn(territory);

    final CompositeChange change =
        new CompositeChange(
            ChangeFactory.addUnits(territory, List.of()),
            ChangeFactory.removeUnits(territory, List.of()));

    final CompositeChange flattenedChange = change.flatten();
    assertThat(
        "Composite change with no child composite changes should have the same list of "
            + "changes after flattening",
        flattenedChange.getChanges(),
        is(change.getChanges()));
  }
}
