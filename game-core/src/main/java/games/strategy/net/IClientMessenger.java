package games.strategy.net;

/** A client messenger. */
public interface IClientMessenger extends IMessenger {
  /** Listen for errors. */
  void addErrorListener(IMessengerErrorListener listener);

  /** Stop listening for errors. */
  void removeErrorListener(IMessengerErrorListener listener);
}
