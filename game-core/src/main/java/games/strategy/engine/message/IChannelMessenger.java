package games.strategy.engine.message;

import games.strategy.net.INode;

/**
 * A simple way to multicast method calls over several machines and possibly several objects on each
 * machine.
 *
 * <p>A channel can be created such that all channel subscribers must implement the same interface.
 * Channel subscribers can be on multiple machines.
 *
 * <p>On VM A
 *
 * <pre>
 * RemoteName FOO = new RemoteName(&quot;Foo&quot;, IFoo.class);
 * IFoo aFoo = new Foo();
 * someChannelMessenger.registerChannelSubscriber(aFoo, FOO);
 * </pre>
 *
 * <p>On VM B
 *
 * <pre>
 * IFoo anotherFoo = new Foo();
 * anotherChannelMessenger.registerChannelSubscriber(anotherFoo, FOO);
 * IFoo multicastFoo = (IFoo) anotherChannelMessenger.getChannelBroadcaster(FOO);
 * multicastFoo.fee();
 * </pre>
 *
 * <p>The <code>multicast.fee()</code> line results in two method calls, one on VM B to anotherFoo,
 * and another call on VM A to aFoo.
 *
 * <p>The magic is done using reflection and dynamic proxies (java.lang.reflect.Proxy)
 *
 * <p>To avoid naming conflicts, it is advised to use the fully qualified java name and and a
 * constant as channel names. For example channel names should be in the form
 *
 * <pre>
 * "foo.fee.Fi.SomeConstant"
 * </pre>
 *
 * <p>where SomeConstant is defined in the class foo.fee.Fi
 *
 * <p><b>Channels and threading</b>
 *
 * <p>There will only be one thread calling methods in a channel at one time. Methods will be called
 * on subscribers in the order that they are called on broadcasters. This means that if you block
 * the current thread during a client invocation, no further methods can be called on that channel.
 */
public interface IChannelMessenger {
  /**
   * Get a reference such that methods called on it will be multicast to all subscribers of the
   * channel.
   */
  IChannelSubscriber getChannelBroadcaster(RemoteName channelName);

  /** register a subscriber to a channel. */
  void registerChannelSubscriber(Object implementor, RemoteName channelName);

  /** unregister a subscriber to a channel. */
  void unregisterChannelSubscriber(Object implementor, RemoteName channelName);

  INode getLocalNode();
}
