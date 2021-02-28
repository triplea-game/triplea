package games.strategy.net;

/** A listener that receives error notifications from a {@link IMessenger}. */
public interface IMessengerErrorListener {
  /**
   * The messenger is no longer able to send or receive messages. This signals that an error has
   * occurred, will not be sent if the node was shutdown.
   */
  void messengerInvalid(Throwable cause);
}
