package games.strategy.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

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
