package games.strategy.util.memento;

/**
 * Exports a memento from its originator.
 *
 * @param <T> The type of the memento originator.
 */
public interface MementoExporter<T> {
  /**
   * Exports a memento from the specified originator.
   *
   * @param originator The memento originator; must not be {@code null}.
   *
   * @return The exported memento; never {@code null}.
   *
   * @throws MementoExportException If an error occurs while exporting the memento.
   */
  Memento exportMemento(T originator) throws MementoExportException;
}
