package games.strategy.util.memento;

/**
 * Imports a memento to its originator.
 *
 * @param <T> The type of the memento originator.
 */
public interface MementoImporter<T> {
  /**
   * Imports the specified memento and returns the originator.
   *
   * @param memento The memento to be imported.
   *
   * @return The memento originator.
   *
   * @throws MementoImportException If an error occurs while importing the memento.
   */
  T importMemento(Memento memento) throws MementoImportException;
}
