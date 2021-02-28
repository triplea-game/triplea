package games.strategy.engine.framework.startup.mc;

/** A listener that receives network game setup events from a remote node. */
public interface IRemoteModelListener {
  IRemoteModelListener NULL_LISTENER =
      new IRemoteModelListener() {
        @Override
        public void playerListChanged() {}

        @Override
        public void playersTakenChanged() {}
      };

  /** The players available have changed. */
  void playerListChanged();

  /** The players taken have changed. */
  void playersTakenChanged();
}
