package games.strategy.engine.data;

import java.io.Serializable;

/**
 * Not an interface because we want the perform() method to be protected.
 * A Change encapsulates something that can be done to GameData. We use changes so
 * that we can serialize and track change to GameData as they occur.
 * When a change is performed on a GameData in a game, the change is serialized and sent
 * across the network to all the clients. Since all changes to GameData are done through changes
 * and all changes are serialized to all clients, all clients should always be in sync.
 * A Change can be inverted to create an equal but opposite change.
 * Use ChangeFactory to create Changes.
 */
public abstract class Change implements Serializable {
  static final long serialVersionUID = -5563487769423328606L;

  protected abstract void perform(GameData data);

  public abstract Change invert();

  public boolean isEmpty() {
    return false;
  }
}
