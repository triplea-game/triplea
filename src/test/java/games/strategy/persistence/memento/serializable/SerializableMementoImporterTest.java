package games.strategy.persistence.memento.serializable;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.persistence.serializable.DefaultPersistenceDelegateRegistry;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.memento.Memento;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public final class SerializableMementoImporterTest {
  private final PersistenceDelegateRegistry persistenceDelegateRegistry = new DefaultPersistenceDelegateRegistry();

  private final Memento memento = newMemento();

  private final SerializableMementoImporter mementoImporter = newMementoImporter();

  private static Memento newMemento() {
    return new FakeMemento("field");
  }

  private SerializableMementoImporter newMementoImporter() {
    return new SerializableMementoImporter(persistenceDelegateRegistry);
  }

  @Test
  public void importMemento_ShouldNotCloseInputStream() throws Exception {
    try (final InputStream is = spy(newInputStreamWithValidContent())) {
      mementoImporter.importMemento(is);

      verify(is, never()).close();
    }
  }

  private InputStream newInputStreamWithValidContent() throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      newMementoExporter().exportMemento(memento, baos);
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }

  private SerializableMementoExporter newMementoExporter() {
    return new SerializableMementoExporter(persistenceDelegateRegistry);
  }
}
