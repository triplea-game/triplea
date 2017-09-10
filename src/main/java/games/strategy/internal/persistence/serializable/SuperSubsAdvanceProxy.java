package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.SuperSubsAdvance;

/**
 * A serializable proxy for the {@link SuperSubsAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link SuperSubsAdvance} created from this proxy will always have their game data set to {@code null}. Proxies that
 * may compose instances of this proxy are required to manually restore the game data in their {@code readResolve()}
 * method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class SuperSubsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 6085537658742849644L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(SuperSubsAdvance.class, SuperSubsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public SuperSubsAdvanceProxy(final SuperSubsAdvance superSubsAdvance) {
    checkNotNull(superSubsAdvance);

    attachments = superSubsAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final SuperSubsAdvance superSubsAdvance = new SuperSubsAdvance(null);
    attachments.forEach(superSubsAdvance::addAttachment);
    return superSubsAdvance;
  }
}
