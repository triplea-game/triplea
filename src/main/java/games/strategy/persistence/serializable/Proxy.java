package games.strategy.persistence.serializable;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A serializable proxy for a non-serializable principal.
 *
 * <p>
 * In addition to implementing the methods of this interface, a proxy must provide an associated implementation of
 * {@link ProxyFactory}. This is typically achieved via the following:
 * </p>
 *
 * <ul>
 * <li>
 * Define a copy constructor that accepts an instance of the principal and initializes all proxy fields from that
 * instance.
 * </li>
 * <li>
 * Define a class field that provides an instance of {@link ProxyFactory} for the proxy implementation using
 * {@link ProxyFactory#newInstance(Class, java.util.function.Function)}, where the first argument is the principal
 * type and the second argument is the copy constructor defined in the previous item.
 * </li>
 * </ul>
 *
 * <p>
 * For example, consider a principal type named {@code Foo} with the following public API:
 * </p>
 *
 * <pre>
 * <code>
 * public class Foo {
 *   public Foo(int bar, double baz);
 *   public int getBar();
 *   public double getBaz();
 * }
 * </code>
 * </pre>
 *
 * <p>
 * Then an acceptable proxy implementation for {@code Foo} would be:
 * </p>
 *
 * <pre>
 * <code>
 * public final class FooProxy implements Proxy {
 *   private static final long serialVersionUID = 6092507250760560736L;
 *
 *   public static final ProxyFactory FACTORY = ProxyFactory.newInstance(Foo.class, FooProxy::new);
 *
 *   private final int bar;
 *   private final double baz;
 *
 *   public FooProxy(final Foo foo) {
 *     checkNotNull(foo);
 *     bar = foo.getBar();
 *     baz = foo.getBaz();
 *   }
 *
 *   &#64;Override
 *   public Object readResolve() {
 *     return new Foo(bar, baz);
 *   }
 * }
 * </code>
 * </pre>
 */
public interface Proxy extends Serializable {
  /**
   * Creates a new principal from the state of this proxy.
   *
   * @return A new principal.
   *
   * @throws ObjectStreamException If the principal cannot be created.
   */
  Object readResolve() throws ObjectStreamException;
}
