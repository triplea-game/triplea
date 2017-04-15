package games.strategy.util.memento;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import games.strategy.util.memento.PropertyBagMementoImporter.HandlerSupplier;

public final class PropertyBagMementoImporterTest {
  private static final String SCHEMA_ID = "schema-id";

  private static final long SCHEMA_VERSION = 1L;

  private final FakeOriginator originator = new FakeOriginator(42, "2112");

  @Test
  public void importMemento_ShouldThrowExceptionIfMementoHasWrongType() {
    final Memento memento = mock(Memento.class);
    final PropertyBagMementoImporter<?> mementoImporter = newMementoImporterForAllSchemaVersions();

    catchException(() -> mementoImporter.importMemento(memento));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoImportException.class)),
        hasMessageThat(containsString("wrong type"))));
  }

  private static PropertyBagMementoImporter<FakeOriginator> newMementoImporterForAllSchemaVersions() {
    return newMementoImporter(schemaVersion -> Optional.of(propertiesByName -> {
      return new FakeOriginator(
          (Integer) propertiesByName.get("field1"),
          (String) propertiesByName.get("field2"));
    }));
  }

  private static PropertyBagMementoImporter<FakeOriginator> newMementoImporter(
      final HandlerSupplier<FakeOriginator> handlerSupplier) {
    return new PropertyBagMementoImporter<>(SCHEMA_ID, handlerSupplier);
  }

  @Test
  public void importMementoWithPropertyBagMemento_ShouldReturnOriginatorWithMementoProperties() throws Exception {
    final PropertyBagMemento memento = newMemento(SCHEMA_ID, SCHEMA_VERSION);
    final PropertyBagMementoImporter<FakeOriginator> mementoImporter = newMementoImporterForAllSchemaVersions();

    final FakeOriginator actual = mementoImporter.importMemento(memento);

    assertThat(actual, is(originator));
  }

  private PropertyBagMemento newMemento(final String schemaId, final long schemaVersion) {
    return new PropertyBagMemento(schemaId, schemaVersion, ImmutableMap.<String, Object>of(
        "field1", originator.field1,
        "field2", originator.field2));
  }

  @Test
  public void importMementoWithPropertyBagMemento_ShouldThrowExceptionIfSchemaIdIsUnsupported() {
    final String schemaId = "other-schema-id";
    final PropertyBagMemento memento = newMemento(schemaId, SCHEMA_VERSION);
    final PropertyBagMementoImporter<?> mementoImporter = newMementoImporterForAllSchemaVersions();

    catchException(() -> mementoImporter.importMemento(memento));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoImportException.class)),
        hasMessageThat(containsString(String.format("schema ID '%s' is unsupported", schemaId)))));
  }

  @Test
  public void importMementoWithPropertyBagMemento_ShouldThrowExceptionIfSchemaVersionIsUnsupported() {
    final long schemaVersion = 2112L;
    final PropertyBagMemento memento = newMemento(SCHEMA_ID, schemaVersion);
    final PropertyBagMementoImporter<?> mementoImporter = newMementoImporterForNoSchemaVersions();

    catchException(() -> mementoImporter.importMemento(memento));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoImportException.class)),
        hasMessageThat(containsString(String.format("schema version %d is unsupported", schemaVersion)))));
  }

  private static PropertyBagMementoImporter<FakeOriginator> newMementoImporterForNoSchemaVersions() {
    return newMementoImporter(schemaVersion -> Optional.empty());
  }
}
