package games.strategy.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public interface IObjectStreamFactory {
  ObjectInputStream create(InputStream stream) throws IOException;

  ObjectOutputStream create(OutputStream stream) throws IOException;
}
