package games.strategy.net;

public interface IMessengerErrorListener {
  /**
   * The messenger is no longer able to send or receive messages.
   * This signals that an error has occured, will not be sent if the
   * node was shutdown.
   */
  void messengerInvalid(IMessenger messenger, Exception reason);
}
