package games.strategy.engine.framework;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameObjectInputStream;
import games.strategy.engine.data.GameObjectOutputStream;
import games.strategy.net.IObjectStreamFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Implementation of {@link IObjectStreamFactory} that uses {@link GameObjectOutputStream} and
 * {@link GameObjectInputStream} for serialization and deserialization, respectively. These streams
 * ensure referential identity among various game data components to prevent unwanted copies from
 * being made during the serialization and deserialization process.
 */
public class GameObjectStreamFactory implements IObjectStreamFactory {
  private GameData gameData;

  public GameObjectStreamFactory(final GameData data) {
    gameData = data;
  }

  @Override
  public ObjectInputStream create(final InputStream stream) throws IOException {
    return new GameObjectInputStream(this, stream);
  }

  @Override
  public ObjectOutputStream create(final OutputStream stream) throws IOException {
    return new GameObjectOutputStream(stream);
  }

  public void setData(final GameData data) {
    gameData = data;
  }

  public GameData getData() {
    return gameData;
  }
}
