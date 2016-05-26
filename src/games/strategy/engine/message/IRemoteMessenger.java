package games.strategy.engine.message;

/**
 * Very similar to RMI<br>
 * <br>
 * Objects can be registered with a remote messenger under a public name and
 * declared to implement a particular interface. Methods on this registered
 * object can then be called on different vms.<br>
 * <br>
 * To call a method on the registered object, you get a remote reference to it using the
 * getRemote(...) method. This returns an object that can be used and called
 * as if it were the original implementor. <br>
 * <br>
 * The getRemote(...) method though may be called on a different vm than
 * the registerRemote(...) method. In that case the calls on the return value of
 * getRemote(...) will be routed to the implementor object passed to registerRemote(...)
 * On VM A you have code like
 *
 * <pre>
 * RemoteName FOO = new RemoteName(&quot;Foo&quot;, IFoo.class);
 * IFoo aFoo = new Foo();
 * aRemoteMessenger.registerRemote(IFoo.class, aFoo, FOO);
 * </pre>
 *
 * After this runs, on VM B you can do
 *
 * <pre>
 *
 * IFoo someFoo = anotherRemoteMessenger.getRemote(FOO);
 * boolean rVal = someFoo.fee();
 * if(rVal)
 *   ...
 * </pre>
 *
 * What will happen is that the fee() call in VM B will be invoked on aFoo in VM A.<br>
 * This is done using reflection and dynamic proxies (java.lang.reflect.Proxy)<br>
 * To avoid naming conflicts, it is advised to use the fully qualified java name and
 * and a constant be used as the channel name. For example names should be in the form<br>
 * <br>
 * <br>
 * "foo.fee.Fi.SomeConstant" <br>
 * <br>
 * where SomeConstant is defined in the class foo.fee.Fi<br>
 * <br>
 * <br>
 * <p>
 * <b>Remotes and threading</b>
 * <p>
 * Remotes are multithreaded. Method calls may arrive out of order that methods were called.
 */
public interface IRemoteMessenger {
  /**
   * @param name
   *        the name the remote is registered under.
   * @return a remote reference to the registered remote.
   */
  IRemote getRemote(RemoteName name);

  /**
   * @param name
   *        the name the remote is registered under.
   * @param ignoreResults
   *        whether we need to wait for the results or not
   * @return a remote reference to the registered remote.
   */
  IRemote getRemote(RemoteName name, boolean ignoreResults);

  /**
   * @param remoteInterface
   *        - the remote interface that implementor implements,
   *        must be a subclass of IRemote
   * @param implementor
   *        - an object that implements remoteInterface
   * @param name
   *        - the name that implementor will be registered under
   */
  void registerRemote(Object implementor, RemoteName name);

  /**
   * Remove the remote registered under name.
   */
  void unregisterRemote(String name);

  /**
   * Remove the remote registered under name.
   */
  void unregisterRemote(RemoteName name);

  boolean hasLocalImplementor(RemoteName name);

  boolean isServer();
}
