package games.strategy.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Factory for creating matching pairs of {@link ObjectInputStream} and {@link ObjectOutputStream}.
 * The extra layer of indirection permits customizing the serialization and deserialization process
 * for a particular object graph.
 */
public interface IObjectStreamFactory {
  ObjectInputStream create(InputStream stream) throws IOException;

  ObjectOutputStream create(OutputStream stream) throws IOException;
}
