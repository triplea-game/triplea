package games.strategy.net;

public interface IConnectionChangeListener {
  /**
   * A connection has been added.
   */
  void connectionAdded(INode to);

  /**
   * A connection has been removed.
   */
  void connectionRemoved(INode to);
}
