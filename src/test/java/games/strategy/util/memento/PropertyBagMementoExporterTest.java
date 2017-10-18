package games.strategy.util.memento;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public final class PropertyBagMementoExporterTest {
  private static final String SCHEMA_ID = "schema-id";

  private final FakeOriginator originator = new FakeOriginator(42, "2112");

  @Test
  public void exportMemento_ShouldReturnMementoWithSchemaIdAndOriginatorProperties() throws Exception {
    final PropertyBagMemento expected = newMemento();
    final PropertyBagMementoExporter<FakeOriginator> mementoExporter = newMementoExporter();

    final PropertyBagMemento memento = mementoExporter.exportMemento(originator);

    assertThat(memento.getSchemaId(), is(SCHEMA_ID));
    assertThat(memento.getPropertiesByName(), is(expected.getPropertiesByName()));
  }

  private PropertyBagMemento newMemento() {
    return new PropertyBagMemento(SCHEMA_ID, ImmutableMap.<String, Object>of(
        "field1", originator.field1,
        "field2", originator.field2));
  }

  private static PropertyBagMementoExporter<FakeOriginator> newMementoExporter() {
    return new PropertyBagMementoExporter<>(SCHEMA_ID, (originator, propertiesByName) -> {
      propertiesByName.put("field1", originator.field1);
      propertiesByName.put("field2", originator.field2);
    });
  }
}
