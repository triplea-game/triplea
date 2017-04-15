package games.strategy.persistence.serializable;

import java.io.IOException;
import java.io.ObjectStreamClass;

/**
 * Superclass for all implementations of {@link PersistenceDelegate}.
 *
 * <p>
 * The state of this non-final class is immutable.
 * </p>
 */
public abstract class AbstractPersistenceDelegate implements PersistenceDelegate {
  /**
   * Initializes a new instance of the {@code AbstractPersistenceDelegate} class.
   */
  protected AbstractPersistenceDelegate() {}

  /**
   * This implementation does nothing.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation.
   * </p>
   *
   * @see games.strategy.persistence.serializable.PersistenceDelegate#annotateClass(
   *      games.strategy.persistence.serializable.ObjectOutputStream, java.lang.Class)
   */
  @Override
  public void annotateClass(final ObjectOutputStream stream, final Class<?> cl) throws IOException {
    // do nothing
  }

  /**
   * This implementation does not attempt to replace the object and returns the same instance without modification.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation.
   * </p>
   *
   * @see games.strategy.persistence.serializable.PersistenceDelegate#replaceObject(java.lang.Object)
   */
  @Override
  public Object replaceObject(final Object obj) throws IOException {
    return obj;
  }

  /**
   * This implementation returns the result of calling
   *
   * <pre>
   * Class.forName(desc.getName(), false, getClass().getClassLoader())
   * </pre>
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation.
   * </p>
   *
   * @see games.strategy.persistence.serializable.PersistenceDelegate#resolveClass(
   *      games.strategy.persistence.serializable.ObjectInputStream, java.io.ObjectStreamClass)
   */
  @Override
  public Class<?> resolveClass(final ObjectInputStream stream, final ObjectStreamClass desc)
      throws IOException, ClassNotFoundException {
    return Class.forName(desc.getName(), false, getClass().getClassLoader());
  }

  /**
   * This implementation does not attempt to resolve the object and returns the same instance without modification.
   *
   * <p>
   * Subclasses may override and are not required to call the superclass implementation.
   * </p>
   *
   * @see games.strategy.persistence.serializable.PersistenceDelegate#resolveObject(java.lang.Object)
   */
  @Override
  public Object resolveObject(final Object obj) throws IOException {
    return obj;
  }
}
