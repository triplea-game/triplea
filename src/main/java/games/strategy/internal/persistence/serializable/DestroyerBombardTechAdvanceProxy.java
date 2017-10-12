package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

/**
 * A serializable proxy for the {@link DestroyerBombardTechAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link DestroyerBombardTechAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class DestroyerBombardTechAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 6277384939381379724L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(DestroyerBombardTechAdvance.class, DestroyerBombardTechAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public DestroyerBombardTechAdvanceProxy(final DestroyerBombardTechAdvance destroyerBombardTechAdvance) {
    checkNotNull(destroyerBombardTechAdvance);

    attachments = destroyerBombardTechAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final DestroyerBombardTechAdvance destroyerBombardTechAdvance = new DestroyerBombardTechAdvance(null);
    attachments.forEach(destroyerBombardTechAdvance::addAttachment);
    return destroyerBombardTechAdvance;
  }
}
