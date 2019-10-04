package games.strategy.triplea;

/**
 * Indicates a class is serialized over network and has compatibility considerations. With such
 * classes there is a magic call to 'readObject' and 'writeObject'. Unless these methods are
 * overridden (see: serialization proxy pattern), then the non-transient variable names and types
 * must be kept the same.
 *
 * <p>NetworkData annotated classes do not have a method constraints, they are not necessarily
 * called by reflection or RMI, so we just need to be sure these classes can be serialized and
 * deserialized between different game versions.
 */
public @interface NetworkData {}
