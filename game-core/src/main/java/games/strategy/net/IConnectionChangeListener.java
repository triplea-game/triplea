package games.strategy.net;

/**
 * A listener that receives notifications when the connection list maintained by a {@link
 * IServerMessenger} changes.
 */
public interface IConnectionChangeListener {
  /** A connection has been added. */
  void connectionAdded(INode to);

  /** A connection has been removed. */
  void connectionRemoved(INode to);
}
