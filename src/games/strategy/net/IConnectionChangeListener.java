package games.strategy.net;

public interface IConnectionChangeListener {
  /**
   * A connection has been added.
   */
  public void connectionAdded(INode to);

  /**
   * A connection has been removed.
   */
  public void connectionRemoved(INode to);
}
