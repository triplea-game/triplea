package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamClass;
import java.util.Optional;

/**
 * A stream used for deserializing objects previously serialized using an {@code ObjectOutputStream}.
 *
 * <p>
 * After deserializing an object, the stream will query its persistence delegate registry for an associated persistence
 * delegate and give it an opportunity to substitute the deserialized object with a compatible object.
 * </p>
 *
 * <p>
 * To contribute a persistence delegate for a specific class, register it with the {@code PersistenceDelegateRegistry}
 * passed to the input stream.
 * </p>
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class ObjectInputStream extends java.io.ObjectInputStream {
  private final PersistenceDelegateRegistry persistenceDelegateRegistry;

  /**
   * Initializes a new instance of the {@code ObjectInputStream} class.
   *
   * @param in The input stream from which to read; must not be {@code null}.
   * @param persistenceDelegateRegistry The persistence delegate registry; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs while reading the stream header.
   * @throws java.io.StreamCorruptedException If the stream header is incorrect.
   */
  public ObjectInputStream(final InputStream in, final PersistenceDelegateRegistry persistenceDelegateRegistry)
      throws IOException {
    super(in);

    checkNotNull(persistenceDelegateRegistry);

    this.persistenceDelegateRegistry = persistenceDelegateRegistry;

    enableResolveObject(true);
  }

  @Override
  protected Class<?> resolveClass(final ObjectStreamClass desc) throws IOException, ClassNotFoundException {
    if (desc != null) {
      final Optional<PersistenceDelegate> persistenceDelegate = getPersistenceDelegate(desc.getName());
      if (persistenceDelegate.isPresent()) {
        return persistenceDelegate.get().resolveClass(this, desc);
      }
    }

    return super.resolveClass(desc);
  }

  private Optional<PersistenceDelegate> getPersistenceDelegate(final String typeName) {
    return persistenceDelegateRegistry.getPersistenceDelegate(typeName);
  }

  @Override
  protected Object resolveObject(final Object obj) throws IOException {
    Object object = obj;
    while (object != null) {
      final Optional<PersistenceDelegate> persistenceDelegate = getPersistenceDelegate(object.getClass().getName());
      final Object resolvedObject = persistenceDelegate.isPresent()
          ? persistenceDelegate.get().resolveObject(object)
          : super.resolveObject(object);
      if (object != resolvedObject) {
        object = resolvedObject;
      } else {
        break;
      }
    }

    return object;
  }
}
