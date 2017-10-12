package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.ImprovedShipyardsAdvance;

/**
 * A serializable proxy for the {@link ImprovedShipyardsAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link ImprovedShipyardsAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class ImprovedShipyardsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -46130524050751414L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ImprovedShipyardsAdvance.class, ImprovedShipyardsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public ImprovedShipyardsAdvanceProxy(final ImprovedShipyardsAdvance improvedShipyardsAdvance) {
    checkNotNull(improvedShipyardsAdvance);

    attachments = improvedShipyardsAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final ImprovedShipyardsAdvance improvedShipyardsAdvance = new ImprovedShipyardsAdvance(null);
    attachments.forEach(improvedShipyardsAdvance::addAttachment);
    return improvedShipyardsAdvance;
  }
}
