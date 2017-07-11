package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * A stream used for serializing objects.
 *
 * <p>
 * Before serializing an object, the stream will query its proxy factory registry for an associated proxy factory. If a
 * proxy factory exists, a serializable proxy will be created from the principal, and the proxy will be written to the
 * stream instead of the principal.
 * </p>
 *
 * <p>
 * To contribute a proxy factory for a specific class, register it with the {@link ProxyFactoryRegistry} passed to the
 * output stream.
 * </p>
 *
 * <p>
 * Instances of this class are not thread safe.
 * </p>
 */
public final class ObjectOutputStream extends java.io.ObjectOutputStream {
  private final ProxyFactoryRegistry proxyFactoryRegistry;

  /**
   * @param out The output stream on which to write; must not be {@code null}.
   * @param proxyFactoryRegistry The proxy factory registry; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs while writing the stream header.
   */
  public ObjectOutputStream(final OutputStream out, final ProxyFactoryRegistry proxyFactoryRegistry)
      throws IOException {
    super(out);

    checkNotNull(proxyFactoryRegistry);

    this.proxyFactoryRegistry = proxyFactoryRegistry;

    enableReplaceObject(true);
  }

  @Override
  protected Object replaceObject(final Object obj) {
    if (obj == null) {
      return null;
    }

    final Optional<ProxyFactory> proxyFactory = proxyFactoryRegistry.getProxyFactory(obj.getClass());
    return proxyFactory.map(it -> it.newProxyFor(obj)).orElse(obj);
  }
}
