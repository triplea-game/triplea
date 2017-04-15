package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * A stream used for serializing objects.
 *
 * <p>
 * Before serializing an object, the stream will query its persistence delegate registry for an associated persistence
 * delegate and give it an opportunity to substitute the object with a compatible serializable object.
 * </p>
 *
 * <p>
 * To contribute a persistence delegate for a specific class, register it with the {@code PersistenceDelegateRegistry}
 * passed to the output stream.
 * </p>
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class ObjectOutputStream extends java.io.ObjectOutputStream {
  private final PersistenceDelegateRegistry persistenceDelegateRegistry;

  /**
   * Initializes a new instance of the {@code ObjectOutputStream} class.
   *
   * @param out The output stream on which to write; must not be {@code null}.
   * @param persistenceDelegateRegistry The persistence delegate registry; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs while writing the stream header.
   */
  public ObjectOutputStream(final OutputStream out, final PersistenceDelegateRegistry persistenceDelegateRegistry)
      throws IOException {
    super(out);

    checkNotNull(persistenceDelegateRegistry);

    this.persistenceDelegateRegistry = persistenceDelegateRegistry;

    enableReplaceObject(true);
  }

  @Override
  protected void annotateClass(final Class<?> cl) throws IOException {
    if (cl != null) {
      final Optional<PersistenceDelegate> persistenceDelegate = getPersistenceDelegate(cl);
      if (persistenceDelegate.isPresent()) {
        persistenceDelegate.get().annotateClass(this, cl);
      } else {
        super.annotateClass(cl);
      }
    } else {
      super.annotateClass(cl);
    }
  }

  private Optional<PersistenceDelegate> getPersistenceDelegate(final Class<?> type) {
    return persistenceDelegateRegistry.getPersistenceDelegate(type.getName());
  }

  @Override
  protected Object replaceObject(final Object obj) throws IOException {
    Object object = obj;
    while (object != null) {
      final Optional<PersistenceDelegate> persistenceDelegate = getPersistenceDelegate(object.getClass());
      final Object replacedObject = persistenceDelegate.isPresent()
          ? persistenceDelegate.get().replaceObject(object)
          : super.replaceObject(object);
      if (object != replacedObject) {
        object = replacedObject;
      } else {
        break;
      }
    }

    return object;
  }
}
