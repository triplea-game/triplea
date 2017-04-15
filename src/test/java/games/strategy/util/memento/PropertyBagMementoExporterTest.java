package games.strategy.util.memento;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import games.strategy.util.memento.PropertyBagMementoExporter.HandlerSupplier;

public final class PropertyBagMementoExporterTest {
  private static final long DEFAULT_SCHEMA_VERSION = 42L;

  private static final String SCHEMA_ID = "schema-id";

  private final FakeOriginator originator = new FakeOriginator(42, "2112");

  @Test
  public void exportMemento_ShouldReturnMementoWithDefaultSchemaVersion() throws Exception {
    final PropertyBagMementoExporter<FakeOriginator> mementoExporter = newMementoExporterForAllSchemaVersions();

    final PropertyBagMemento memento = mementoExporter.exportMemento(originator);

    assertThat(memento.getSchemaVersion(), is(DEFAULT_SCHEMA_VERSION));
  }

  private static PropertyBagMementoExporter<FakeOriginator> newMementoExporterForAllSchemaVersions() {
    return newMementoExporter(schemaVersion -> Optional.of((originator, propertiesByName) -> {
      propertiesByName.put("field1", originator.field1);
      propertiesByName.put("field2", originator.field2);
    }));
  }

  private static PropertyBagMementoExporter<FakeOriginator> newMementoExporter(
      final HandlerSupplier<FakeOriginator> handlerSupplier) {
    return new PropertyBagMementoExporter<>(SCHEMA_ID, DEFAULT_SCHEMA_VERSION, handlerSupplier);
  }

  @Test
  public void exportMementoWithSchemaVersion_ShouldReturnMementoWithSpecifiedSchemaIdAndVersion() throws Exception {
    final long schemaVersion = 2112L;
    final PropertyBagMementoExporter<FakeOriginator> mementoExporter = newMementoExporterForAllSchemaVersions();

    final PropertyBagMemento memento = mementoExporter.exportMemento(originator, schemaVersion);

    assertThat(memento.getSchemaId(), is(SCHEMA_ID));
    assertThat(memento.getSchemaVersion(), is(schemaVersion));
  }

  @Test
  public void exportMementoWithSchemaVersion_ShouldReturnMementoWithOriginatorProperties() throws Exception {
    final PropertyBagMemento expected = newMemento();
    final PropertyBagMementoExporter<FakeOriginator> mementoExporter = newMementoExporterForAllSchemaVersions();

    final PropertyBagMemento memento = mementoExporter.exportMemento(originator, 1L);

    assertThat(memento.getPropertiesByName(), is(expected.getPropertiesByName()));
  }

  private PropertyBagMemento newMemento() {
    return new PropertyBagMemento(SCHEMA_ID, DEFAULT_SCHEMA_VERSION, ImmutableMap.<String, Object>of(
        "field1", originator.field1,
        "field2", originator.field2));
  }

  @Test
  public void exportMementoWithSchemaVersion_ShouldThrowExceptionIfSchemaVersionIsUnsupported() {
    final long schemaVersion = 2112L;
    final PropertyBagMementoExporter<FakeOriginator> mementoExporter = newMementoExporterForNoSchemaVersions();

    catchException(() -> mementoExporter.exportMemento(originator, schemaVersion));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoExportException.class)),
        hasMessageThat(containsString(String.format("schema version %d is unsupported", schemaVersion)))));
  }

  private static PropertyBagMementoExporter<FakeOriginator> newMementoExporterForNoSchemaVersions() {
    return newMementoExporter(schemaVersion -> Optional.empty());
  }
}
