package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.ImprovedArtillerySupportAdvance;

/**
 * A serializable proxy for the {@link ImprovedArtillerySupportAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link ImprovedArtillerySupportAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class ImprovedArtillerySupportAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1231680617895343722L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ImprovedArtillerySupportAdvance.class, ImprovedArtillerySupportAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public ImprovedArtillerySupportAdvanceProxy(final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance) {
    checkNotNull(improvedArtillerySupportAdvance);

    attachments = improvedArtillerySupportAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final ImprovedArtillerySupportAdvance improvedArtillerySupportAdvance = new ImprovedArtillerySupportAdvance(null);
    attachments.forEach(improvedArtillerySupportAdvance::addAttachment);
    return improvedArtillerySupportAdvance;
  }
}
