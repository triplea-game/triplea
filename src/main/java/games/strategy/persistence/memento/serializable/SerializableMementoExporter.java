package games.strategy.persistence.memento.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.CloseShieldOutputStream;

import games.strategy.persistence.serializable.ObjectOutputStream;
import games.strategy.persistence.serializable.ProxyRegistry;
import games.strategy.util.memento.Memento;

/**
 * A memento exporter for the Java object serialization format.
 */
public final class SerializableMementoExporter {
  private final ProxyRegistry proxyRegistry;

  /**
   * @param proxyRegistry The proxy registry to use during exports.
   */
  public SerializableMementoExporter(final ProxyRegistry proxyRegistry) {
    checkNotNull(proxyRegistry);

    this.proxyRegistry = proxyRegistry;
  }

  /**
   * Exports the specified memento to the specified stream.
   *
   * @param memento The memento to be exported.
   * @param os The stream to which the memento will be exported. The caller is responsible for closing this stream; it
   *        will not be closed when this method returns.
   *
   * @throws SerializableMementoExportException If an error occurs while exporting the memento.
   */
  public void exportMemento(final Memento memento, final OutputStream os) throws SerializableMementoExportException {
    checkNotNull(memento);
    checkNotNull(os);

    try (final ObjectOutputStream oos = newObjectOutputStream(os)) {
      oos.writeObject(memento);
    } catch (final IOException e) {
      throw new SerializableMementoExportException(e);
    }
  }

  private ObjectOutputStream newObjectOutputStream(final OutputStream os) throws IOException {
    return new ObjectOutputStream(new CloseShieldOutputStream(os), proxyRegistry);
  }
}
