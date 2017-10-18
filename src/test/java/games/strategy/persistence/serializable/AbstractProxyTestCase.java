package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.test.Matchers.equalTo;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import games.strategy.io.IoUtils;
import games.strategy.test.EqualityComparator;
import games.strategy.test.EqualityComparatorRegistry;

/**
 * A fixture for testing the basic aspects of proxy classes.
 *
 * @param <T> The type of the principal to be proxied.
 */
public abstract class AbstractProxyTestCase<T> {
  private EqualityComparatorRegistry equalityComparatorRegistry;
  private final Class<T> principalType;
  private ProxyRegistry proxyRegistry;

  protected AbstractProxyTestCase(final Class<T> principalType) {
    checkNotNull(principalType);

    this.principalType = principalType;
  }

  /**
   * Asserts that the specified principals are equal.
   *
   * <p>
   * This implementation compares the two objects using the {@link games.strategy.test.Matchers#equalTo(Object)} matcher
   * with an equality comparator registry composed of the items from {@link #getEqualityComparators()}. Subclasses may
   * override and are not required to call the superclass implementation.
   * </p>
   *
   * @param expected The expected principal.
   * @param actual The actual principal.
   *
   * @throws AssertionError If the two principals are not equal.
   */
  protected void assertPrincipalEquals(final T expected, final T actual) {
    assertThat(actual, equalTo(expected).withEqualityComparatorRegistry(equalityComparatorRegistry));
  }

  /**
   * Creates a collection of principals whose capability to be persisted via a proxy will be tested.
   *
   * @return The collection of principals to be tested.
   */
  protected abstract Collection<T> createPrincipals();

  /**
   * Gets the collection of equality comparators required to compare two instances of the principal type for equality.
   *
   * <p>
   * This implementation returns an empty collection. Subclasses may override and are not required to call the
   * superclass implementation.
   * </p>
   *
   * @return The collection of equality comparators required to compare two instances of the principal type for
   *         equality.
   */
  protected Collection<EqualityComparator> getEqualityComparators() {
    return Arrays.asList();
  }

  /**
   * Gets the collection of proxy factories required for the principal to be persisted.
   *
   * @return The collection of proxy factories required for the principal to be persisted.
   */
  protected abstract Collection<ProxyFactory> getProxyFactories();

  /**
   * Allows subclasses to modify the actual principal immediately after it is deserialized but before it is compared to
   * the expected principal.
   *
   * <p>
   * This method may be used by subclasses to initialize the state of the principal that is not serialized by the proxy
   * but is considered as part of the equality comparison. Typically, such state would be back references to objects
   * that contain a reference to the principal. Such circular references are not supported by proxy serialization and
   * must be re-established post-deserialization.
   * </p>
   *
   * <p>
   * This implementation does nothing. Subclasses may override and are not required to call the superclass
   * implementation.
   * </p>
   *
   * @param actual The actual principal.
   */
  protected void prepareDeserializedPrincipal(final T actual) {}

  private static Object readObject(final byte[] bytes) throws Exception {
    return IoUtils.readFromMemory(bytes, is -> {
      try (ObjectInputStream ois = new ObjectInputStream(is)) {
        return ois.readObject();
      } catch (final ClassNotFoundException e) {
        throw new IOException(e);
      }
    });
  }

  private byte[] writeObject(final T obj) throws Exception {
    return IoUtils.writeToMemory(os -> {
      try (ObjectOutputStream oos = new ProxyableObjectOutputStream(os, proxyRegistry)) {
        oos.writeObject(obj);
      }
    });
  }

  /**
   * Subclasses may override and are required to call the superclass implementation first.
   */
  @Before
  public void setUp() {
    equalityComparatorRegistry = EqualityComparatorRegistry.newInstance(getEqualityComparators());
    proxyRegistry = ProxyRegistry.newInstance(getProxyFactories());
  }

  @Test
  public final void shouldBeAbleToRoundTripPrincipal() throws Exception {
    final Collection<T> principals = createPrincipals();
    assertThat(principals, is(not(empty())));
    for (final T expected : principals) {
      assertThat(expected, is(not(nullValue())));

      final byte[] bytes = writeObject(expected);
      final Object untypedActual = readObject(bytes);

      assertThat(untypedActual, is(not(nullValue())));
      assertThat(untypedActual, is(instanceOf(principalType)));
      final T actual = principalType.cast(untypedActual);
      prepareDeserializedPrincipal(actual);
      assertPrincipalEquals(expected, actual);
    }
  }
}
