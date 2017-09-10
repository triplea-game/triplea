package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.AARadarAdvance;

/**
 * A serializable proxy for the {@link AARadarAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link AARadarAdvance}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class AaRadarAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 4097155483091551144L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(AARadarAdvance.class, AaRadarAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public AaRadarAdvanceProxy(final AARadarAdvance aaRadarAdvance) {
    checkNotNull(aaRadarAdvance);

    attachments = aaRadarAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final AARadarAdvance aaRadarAdvance = new AARadarAdvance(null);
    attachments.forEach(aaRadarAdvance::addAttachment);
    return aaRadarAdvance;
  }
}
