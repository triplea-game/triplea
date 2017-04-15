package games.strategy.persistence.memento.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.CloseShieldInputStream;

import games.strategy.persistence.serializable.ObjectInputStream;
import games.strategy.persistence.serializable.PersistenceDelegateRegistry;
import games.strategy.util.memento.Memento;

/**
 * A memento importer for the Java object serialization format.
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class SerializableMementoImporter {
  private final PersistenceDelegateRegistry persistenceDelegateRegistry;

  /**
   * Initializes a new instance of the {@code SerializableMementoImporter} class.
   *
   * @param persistenceDelegateRegistry The persistence delegate registry to use during imports; must not be
   *        {@code null}.
   */
  public SerializableMementoImporter(final PersistenceDelegateRegistry persistenceDelegateRegistry) {
    checkNotNull(persistenceDelegateRegistry);

    this.persistenceDelegateRegistry = persistenceDelegateRegistry;
  }

  /**
   * Imports a memento from the specified stream.
   *
   * @param is The stream from which the memento will be imported; must not be {@code null}. The caller is responsible
   *        for closing this stream; it will not be closed when this method returns.
   *
   * @return The imported memento; never {@code null}.
   *
   * @throws SerializableMementoImportException If an error occurs while importing the memento.
   */
  public Memento importMemento(final InputStream is) throws SerializableMementoImportException {
    checkNotNull(is);

    try (final ObjectInputStream ois = newObjectInputStream(is)) {
      readMetadata(ois);
      return readMemento(ois);
    } catch (final IOException | ClassNotFoundException e) {
      throw new SerializableMementoImportException(e);
    }
  }

  private ObjectInputStream newObjectInputStream(final InputStream is) throws IOException {
    return new ObjectInputStream(new CloseShieldInputStream(is), persistenceDelegateRegistry);
  }

  private static void readMetadata(final ObjectInputStream ois) throws SerializableMementoImportException, IOException {
    final String mimeType = ois.readUTF();
    if (!SerializableFormatConstants.MIME_TYPE.equals(mimeType)) {
      throw new SerializableMementoImportException("an illegal MIME type was specified in the stream");
    }

    final long version = ois.readLong();
    if (version != SerializableFormatConstants.CURRENT_VERSION) {
      throw new SerializableMementoImportException("an incompatible version was specified in the stream");
    }
  }

  private static Memento readMemento(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
    return (Memento) ois.readObject();
  }
}
