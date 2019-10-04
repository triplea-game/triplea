package games.strategy.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Default implementation of {@link IObjectStreamFactory} that uses {@link ObjectOutputStream} and
 * {@link ObjectInputStream} for serialization and deserialization, respectively.
 */
public class DefaultObjectStreamFactory implements IObjectStreamFactory {
  @Override
  public ObjectInputStream create(final InputStream stream) throws IOException {
    return new ObjectInputStream(stream);
  }

  @Override
  public ObjectOutputStream create(final OutputStream stream) throws IOException {
    return new ObjectOutputStream(stream);
  }
}
