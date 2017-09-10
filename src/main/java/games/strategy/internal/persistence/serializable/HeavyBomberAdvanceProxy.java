package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.HeavyBomberAdvance;

/**
 * A serializable proxy for the {@link HeavyBomberAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link HeavyBomberAdvance} created from this proxy will always have their game data set to {@code null}. Proxies that
 * may compose instances of this proxy are required to manually restore the game data in their {@code readResolve()}
 * method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class HeavyBomberAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1868161097462691906L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(HeavyBomberAdvance.class, HeavyBomberAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public HeavyBomberAdvanceProxy(final HeavyBomberAdvance heavyBomberAdvance) {
    checkNotNull(heavyBomberAdvance);

    attachments = heavyBomberAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final HeavyBomberAdvance heavyBomberAdvance = new HeavyBomberAdvance(null);
    attachments.forEach(heavyBomberAdvance::addAttachment);
    return heavyBomberAdvance;
  }
}
