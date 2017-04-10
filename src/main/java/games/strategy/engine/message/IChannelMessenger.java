package games.strategy.engine.message;

import games.strategy.net.INode;

/**
 * A simple way to multicast method calls over several machines and possibly several objects on each machine.
 *
 * <p>
 * A channel can be created such that all channel subscribers must implement the same
 * interface. Channel subscribors can be on multiple machines.
 * </p>
 *
 * <p>
 * On VM A
 * </p>
 *
 * <pre>
 * RemoteName FOO = new RemoteName(&quot;Foo&quot;, IFoo.class);
 * IFoo aFoo = new Foo();
 * someChannelMessenger.registerChannelSubscribor(aFoo, FOO);
 * </pre>
 *
 * <p>
 * On VM B
 * </p>
 *
 * <pre>
 * IFoo anotherFoo = new Foo();
 * anotherChannelMessenger.registerChannelSubscribor(anotherFoo, FOO);
 * IFoo multicastFoo = (IFoo) anotherChannelMessenger.getChannelBroadcastor(FOO);
 * multicastFoo.fee();
 * </pre>
 *
 * <p>
 * The <code>multicast.fee()</code> line results in two method calls, one on VM B to anotherFoo, and
 * another call on VM A to aFoo.
 * </p>
 *
 * <p>
 * The magic is done using reflection and dynamic proxies (java.lang.reflect.Proxy)
 * </p>
 *
 * <p>
 * To avoid naming conflicts, it is advised to use the fully qualified java name and
 * and a constant as channel names. For example channel names should be in the form
 * </p>
 *
 * <pre>
 * "foo.fee.Fi.SomeConstant"
 * </pre>
 *
 * <p>
 * where SomeConstant is defined in the class foo.fee.Fi
 * </p>
 *
 * <p>
 * <b>Channels and threading</b>
 * </p>
 *
 * <p>
 * There will only be one thread calling methods in a channel at one time. Methods will be called on subscribors in the
 * order that they are
 * called on broadcasters. This means that if you block the current thread during a client invocation, no further
 * methods can be called on
 * that channel.
 * </p>
 */
public interface IChannelMessenger {
  /**
   * Get a reference such that methods called on it will be multicast
   * to all subscribers of the channel.
   */
  IChannelSubscribor getChannelBroadcastor(RemoteName channelName);

  /**
   * register a subscriber to a channel.
   */
  void registerChannelSubscriber(Object implementor, RemoteName channelName);

  /**
   * unregister a subscriber to a channel.
   */
  void unregisterChannelSubscriber(Object implementor, RemoteName channelName);

  INode getLocalNode();

  /**
   * Indicates the underlying messenger is a server.
   */
  boolean isServer();
}
