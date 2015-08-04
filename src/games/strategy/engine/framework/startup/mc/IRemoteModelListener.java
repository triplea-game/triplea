package games.strategy.engine.framework.startup.mc;

public interface IRemoteModelListener {
  /**
   * The players available have changed.
   */
  public void playerListChanged();

  /**
   * The players taken have changed
   */
  public void playersTakenChanged();

  public static IRemoteModelListener NULL_LISTENER = new IRemoteModelListener() {
    @Override
    public void playerListChanged() {}

    @Override
    public void playersTakenChanged() {}
  };
}
