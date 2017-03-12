package games.strategy.engine.message;

/**
 * A marker interface, used to indicate that the interface
 * can be used by IChannelMessenger
 * All arguments to all methods of an IChannelSubscriber
 * must be serializable, since the methods
 * may be called by a remote VM.
 * Return values of an IChannelSubscriber will be ignored.
 * Exceptions thrown by methods of an IChannelSubscriber will
 * be printed to standard error, but otherwise ignored.
 * Arguments to the methods of IChannelSubscribor should not be modified
 * in any way. The values may be used in method calls to other
 * subscribors.
 */
public interface IChannelSubscribor {
}
