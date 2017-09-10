package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javax.annotation.Nullable;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.data.annotations.InternalDoNotExport;

public class GameDataComponent implements Serializable {
  static final long serialVersionUID = -2066504666509851740L;
  @InternalDoNotExport
  private GameData m_data;

  /**
   * Creates new GameDataComponent.
   *
   * @param data
   *        game data
   */
  public GameDataComponent(final GameData data) {
    m_data = data;
  }

  public GameData getData() {
    return m_data;
  }

  /**
   * Sets the game data that owns this component.
   *
   * <p>
   * Subclasses may override and are required to call the superclass implementation first. Subclasses should override to
   * set the specified game data on any game data components that they may contain.
   * </p>
   *
   * @param gameData The game data that owns this component.
   */
  @InternalDoNotExport
  @VisibleForTesting
  public void setGameData(final @Nullable GameData gameData) {
    m_data = gameData;
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
