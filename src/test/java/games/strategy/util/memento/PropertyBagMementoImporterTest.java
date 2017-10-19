package games.strategy.util.memento;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

public final class PropertyBagMementoImporterTest {
  private static final String SCHEMA_ID = "schema-id";

  private final FakeOriginator originator = new FakeOriginator(42, "2112");

  @Test
  public void importMemento_ShouldThrowExceptionIfMementoHasWrongType() {
    final Memento memento = mock(Memento.class);
    final PropertyBagMementoImporter<?> mementoImporter = newMementoImporter();

    catchException(() -> mementoImporter.importMemento(memento));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoImportException.class)),
        hasMessageThat(containsString("wrong type"))));
  }

  private static PropertyBagMementoImporter<FakeOriginator> newMementoImporter() {
    return new PropertyBagMementoImporter<>(SCHEMA_ID, propertiesByName -> {
      return new FakeOriginator(
          (Integer) propertiesByName.get("field1"),
          (String) propertiesByName.get("field2"));
    });
  }

  @Test
  public void importMementoWithPropertyBagMemento_ShouldReturnOriginatorWithMementoProperties() throws Exception {
    final PropertyBagMemento memento = newMemento(SCHEMA_ID);
    final PropertyBagMementoImporter<FakeOriginator> mementoImporter = newMementoImporter();

    final FakeOriginator actual = mementoImporter.importMemento(memento);

    assertThat(actual, is(originator));
  }

  private PropertyBagMemento newMemento(final String schemaId) {
    return new PropertyBagMemento(schemaId, ImmutableMap.<String, Object>of(
        "field1", originator.field1,
        "field2", originator.field2));
  }

  @Test
  public void importMementoWithPropertyBagMemento_ShouldThrowExceptionIfSchemaIdIsUnsupported() {
    final String schemaId = "other-schema-id";
    final PropertyBagMemento memento = newMemento(schemaId);
    final PropertyBagMementoImporter<?> mementoImporter = newMementoImporter();

    catchException(() -> mementoImporter.importMemento(memento));

    assertThat(caughtException(), allOf(
        is(instanceOf(MementoImportException.class)),
        hasMessageThat(containsString(String.format("schema ID '%s' is unsupported", schemaId)))));
  }
}
