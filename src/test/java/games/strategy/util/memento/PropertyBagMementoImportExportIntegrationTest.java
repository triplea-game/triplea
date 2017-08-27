package games.strategy.util.memento;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public final class PropertyBagMementoImportExportIntegrationTest {
  private static final String SCHEMA_ID = "schema-id";

  private final FakeOriginator originator = new FakeOriginator(42, "2112");

  @Test
  public void shouldBeAbleToRoundTripOriginator() throws Exception {
    final PropertyBagMemento memento = newMementoExporter().exportMemento(originator);
    final FakeOriginator actual = newMementoImporter().importMemento(memento);

    assertThat(actual, is(originator));
  }

  private static PropertyBagMementoExporter<FakeOriginator> newMementoExporter() {
    return new PropertyBagMementoExporter<>(SCHEMA_ID, (originator, propertiesByName) -> {
      propertiesByName.put("field1", originator.field1);
      propertiesByName.put("field2", originator.field2);
    });
  }

  private static PropertyBagMementoImporter<FakeOriginator> newMementoImporter() {
    return new PropertyBagMementoImporter<>(SCHEMA_ID, propertiesByName -> {
      final int field1 = (Integer) propertiesByName.get("field1");
      final String field2 = (String) propertiesByName.get("field2");
      return new FakeOriginator(field1, field2);
    });
  }
}
