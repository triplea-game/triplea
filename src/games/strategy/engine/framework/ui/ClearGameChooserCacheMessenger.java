package games.strategy.engine.framework.ui;

public class ClearGameChooserCacheMessenger {
  private volatile boolean canceled = false;

  public ClearGameChooserCacheMessenger() {
  }

  public void sendCancel() {
    canceled = true;
  }
  public boolean isCancelled() {
    return canceled;
  }
}
