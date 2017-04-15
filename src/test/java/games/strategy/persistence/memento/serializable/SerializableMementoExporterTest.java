package games.strategy.persistence.memento.serializable;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.persistence.serializable.DefaultPersistenceDelegateRegistry;
import games.strategy.util.memento.Memento;

@RunWith(MockitoJUnitRunner.class)
public final class SerializableMementoExporterTest {
  private final Memento memento = newMemento();

  private final SerializableMementoExporter mementoExporter = newMementoExporter();

  @Mock
  private OutputStream os;

  private static Memento newMemento() {
    return new FakeMemento("field");
  }

  private static SerializableMementoExporter newMementoExporter() {
    return new SerializableMementoExporter(new DefaultPersistenceDelegateRegistry());
  }

  @Test
  public void exportMemento_ShouldNotCloseOutputStream() throws Exception {
    mementoExporter.exportMemento(memento, os);

    verify(os, never()).close();
  }
}
