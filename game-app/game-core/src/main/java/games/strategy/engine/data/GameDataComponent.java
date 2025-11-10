package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import javax.annotation.Nullable;

/**
 * Superclass for all game data components (i.e. any domain object contained in an instance of
 * {@link GameData}).
 */
public class GameDataComponent implements Serializable {
  private static final long serialVersionUID = -2066504666509851740L;

  private GameData gameData;

  public GameDataComponent(final GameData gameData) {
    this.gameData = gameData;
  }

  @Nullable
  public GameData getData() {
    return gameData;
  }

  public GameData getDataOrThrow() {
    if (gameData == null)
      throw new IllegalStateException("GameData reference is not expected to be null");
    return gameData;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    // if were writing to a game object stream
    // then we get the game data from the context
    // else we write it.
    if (stream instanceof GameObjectOutputStream) {
      return;
    }
    stream.writeObject(gameData);
  }

  private void readObject(final ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    if (stream instanceof GameObjectInputStream in) {
      gameData = in.getData();
    } else {
      gameData = (GameData) stream.readObject();
    }
  }
}
