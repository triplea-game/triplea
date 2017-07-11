package games.strategy.persistence.memento.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.apache.commons.io.input.CloseShieldInputStream;

import games.strategy.util.memento.Memento;

/**
 * A memento importer for the Java object serialization format.
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 */
public final class SerializableMementoImporter {
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
  @SuppressWarnings("static-method")
  public Memento importMemento(final InputStream is) throws SerializableMementoImportException {
    checkNotNull(is);

    try (final ObjectInputStream ois = newObjectInputStream(is)) {
      return (Memento) ois.readObject();
    } catch (final IOException | ClassNotFoundException e) {
      throw new SerializableMementoImportException(e);
    }
  }

  private static ObjectInputStream newObjectInputStream(final InputStream is) throws IOException {
    return new ObjectInputStream(new CloseShieldInputStream(is));
  }
}
