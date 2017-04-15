package games.strategy.persistence.serializable;

import java.io.IOException;
import java.io.ObjectStreamClass;

/**
 * Represents an object that takes responsibility for persisting objects of a specific class within the Java object
 * serialization framework.
 *
 * <p>
 * The TripleA serialization streams use registered persistence delegates to assist with serializing and deserializing
 * objects, especially those that are not naturally serializable (i.e. they do not implement {@code Serializable}). A
 * persistence delegate registered for a specific class will always be given the first opportunity to serialize or
 * deserialize a given object.
 * </p>
 *
 * <p>
 * To contribute a persistence delegate for a specific class, register it with the platform's
 * {@code PersistenceDelegateRegistry}.
 * </p>
 */
public interface PersistenceDelegate {
  /**
   * Allows the persistence delegate to annotate the specified class with whatever data it deems necessary.
   *
   * @param stream The object output stream; must not be {@code null}.
   * @param cl The class to be annotated; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   */
  void annotateClass(ObjectOutputStream stream, Class<?> cl) throws IOException;

  /**
   * Allows the persistence delegate to replace the specified object being serialized with an equivalent serializable
   * object.
   *
   * @param obj The object being serialized; may be {@code null}.
   *
   * @return The alternate object that replaces the specified object; may be {@code null}. The original object may be
   *         returned if no replacement should occur.
   *
   * @throws IOException If an I/O error occurs.
   */
  Object replaceObject(Object obj) throws IOException;

  /**
   * Allows the persistence delegate to resolve the class associated with the specified object stream class description.
   *
   * @param stream The object input stream; must not be {@code null}.
   * @param desc An object stream class description; must not be {@code null}.
   *
   * @return The class associated with the specified object stream class descriptor; never {@code null}.
   *
   * @throws IOException If an I/O error occurs.
   * @throws ClassNotFoundException If the class cannot be found.
   */
  Class<?> resolveClass(ObjectInputStream stream, ObjectStreamClass desc) throws IOException, ClassNotFoundException;

  /**
   * Allows the persistence delegate to resolve the specified serializable object being deserialized with an equivalent
   * object.
   *
   * @param obj The object being deserialized; may be {@code null}.
   *
   * @return The alternate object that resolves the specified object; may be {@code null}. The original object may be
   *         returned if no resolution should occur.
   *
   * @throws IOException If an I/O error occurs.
   */
  Object resolveObject(Object obj) throws IOException;
}
