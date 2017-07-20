package games.strategy.util.memento;

/**
 * A memento.
 *
 * <p>
 * Mementos are intentionally opaque. Thus, this interface provides absolutely no details about the underlying
 * implementation.
 * </p>
 *
 * <p>
 * Creating a new memento implementation requires three steps:
 * </p>
 *
 * <ol>
 * <li>
 * Define an implementation of {@code Memento} itself. There are no restrictions on the memento implementation as long
 * as it can be round-tripped without losing any originator data using the exporter and importer created in the
 * following steps.
 * </li>
 * <li>
 * Define an implementation of {@link MementoExporter} that can export an instance of the memento type created in step
 * (1) from an existing instance of the associated originator type.
 * </li>
 * <li>
 * Define an implementation of {@link MementoImporter} that can import an instance of the memento type created in step
 * (1) to create a new instance of the associated originator type.
 * </li>
 * </ol>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Memento_pattern">Memento design pattern</a>
 * @see MementoExporter
 * @see MementoImporter
 */
public interface Memento {
}
