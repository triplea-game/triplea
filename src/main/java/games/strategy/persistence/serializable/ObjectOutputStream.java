package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A stream used for serializing objects.
 *
 * <p>
 * Before serializing an object, the stream will query the proxy registry for a proxy associated with the principal. If
 * a proxy is available, the proxy will be written to the stream instead of the principal.
 * </p>
 */
public final class ObjectOutputStream extends java.io.ObjectOutputStream {
  private final ProxyRegistry proxyRegistry;

  /**
   * @param out The output stream on which to write; must not be {@code null}.
   * @param proxyRegistry The proxy registry; must not be {@code null}.
   *
   * @throws IOException If an I/O error occurs while writing the stream header.
   */
  public ObjectOutputStream(final OutputStream out, final ProxyRegistry proxyRegistry) throws IOException {
    super(out);

    checkNotNull(proxyRegistry);

    this.proxyRegistry = proxyRegistry;

    enableReplaceObject(true);
  }

  @Override
  protected Object replaceObject(final Object obj) {
    return (obj != null) ? proxyRegistry.getProxyFor(obj) : null;
  }
}
