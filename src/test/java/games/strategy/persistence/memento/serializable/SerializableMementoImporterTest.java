package games.strategy.persistence.memento.serializable;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static com.googlecode.catchexception.apis.CatchExceptionHamcrestMatchers.hasMessageThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
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
import games.strategy.persistence.serializable.ObjectOutputStream;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.memento.Memento;

@RunWith(MockitoJUnitRunner.class)
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

  @Test
  public void importMemento_ShouldThrowExceptionWhenMetadataMimeTypeIllegal() throws Exception {
    try (final InputStream is = spy(newInputStreamWithIllegalMetadataMimeType())) {
      catchException(() -> mementoImporter.importMemento(is));

      assertThat(caughtException(), allOf(
          is(instanceOf(SerializableMementoImportException.class)),
          hasMessageThat(containsString("illegal MIME type"))));
    }
  }

  private InputStream newInputStreamWithIllegalMetadataMimeType() throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (final ObjectOutputStream oos = new ObjectOutputStream(baos, persistenceDelegateRegistry)) {
        oos.writeUTF("application/illegal-mime-type");
        oos.writeLong(SerializableFormatConstants.CURRENT_VERSION);
        oos.writeObject(memento);
      }
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }

  @Test
  public void importMemento_ShouldThrowExceptionWhenMetadataVersionIncompatible() throws Exception {
    try (final InputStream is = spy(newInputStreamWithIncompatibleMetadataVersion())) {
      catchException(() -> mementoImporter.importMemento(is));

      assertThat(caughtException(), allOf(
          is(instanceOf(SerializableMementoImportException.class)),
          hasMessageThat(containsString("incompatible version"))));
    }
  }

  private InputStream newInputStreamWithIncompatibleMetadataVersion() throws Exception {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (final ObjectOutputStream oos = new ObjectOutputStream(baos, persistenceDelegateRegistry)) {
        oos.writeUTF(SerializableFormatConstants.MIME_TYPE);
        oos.writeLong(-1L);
        oos.writeObject(memento);
      }
      return new ByteArrayInputStream(baos.toByteArray());
    }
  }
}
