package games.strategy.engine.framework.startup.mc;

public interface IRemoteModelListener {
  /**
   * The players available have changed.
   */
  void playerListChanged();

  /**
   * The players taken have changed
   */
  void playersTakenChanged();

  IRemoteModelListener NULL_LISTENER = new IRemoteModelListener() {
    @Override
    public void playerListChanged() {}

    @Override
    public void playersTakenChanged() {}
  };
}
