package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Superclass for all game data components (i.e. any domain object contained in an instance of {@link GameData}).
 */
public class GameDataComponent implements Serializable {
  private static final long serialVersionUID = -2066504666509851740L;

  private GameData m_data;

  /**
   * Creates new GameDataComponent.
   */
  public GameDataComponent(final GameData gameData) {
    m_data = gameData;
  }

  public GameData getData() {
    return m_data;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    // if were writing to a game object stream
    // then we get the game data from the context
    // else we write it.
    if (stream instanceof GameObjectOutputStream) {
      return;
    }
    stream.writeObject(m_data);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    if (stream instanceof GameObjectInputStream) {
      final GameObjectInputStream in = (GameObjectInputStream) stream;
      m_data = in.getData();
    } else {
      m_data = (GameData) stream.readObject();
    }
  }
}
