package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.WarBondsAdvance;

/**
 * A serializable proxy for the {@link WarBondsAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link WarBondsAdvance}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class WarBondsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -204344267560116349L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(WarBondsAdvance.class, WarBondsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public WarBondsAdvanceProxy(final WarBondsAdvance warBondsAdvance) {
    checkNotNull(warBondsAdvance);

    attachments = warBondsAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final WarBondsAdvance warBondsAdvance = new WarBondsAdvance(null);
    attachments.forEach(warBondsAdvance::addAttachment);
    return warBondsAdvance;
  }
}
