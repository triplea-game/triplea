package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.util.PropertyUtil;


public class GameDataComponent implements java.io.Serializable {
  static final long serialVersionUID = -2066504666509851740L;
  @InternalDoNotExport
  private GameData m_data;

  /**
   * Creates new GameDataComponent
   *
   * @param data
   *        game data
   */
  public GameDataComponent(final GameData data) {
    m_data = data;
  }

  public Object getRawPropertyObject(final String property) {
    return PropertyUtil.getPropertyFieldObject(property, this);
  }

  public GameData getData() {
    return m_data;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    writeInternal(stream);
  }

  protected final void writeInternal(final ObjectOutput stream) throws IOException {
    // if were writing to a game object stream
    // then we get the game data from the context
    // else we write it.
    if (stream instanceof GameObjectOutputStream) {
      return;
    }
    stream.writeObject(m_data);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    readInternal(stream);
  }

  protected final void readInternal(final ObjectInput stream) throws IOException, ClassNotFoundException {
    if (stream instanceof GameObjectInputStream) {
      final GameObjectInputStream in = (GameObjectInputStream) stream;
      m_data = in.getData();
    } else {
      m_data = (GameData) stream.readObject();
    }
  }
}
