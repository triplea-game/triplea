package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;

/**
 * A serializable proxy for the {@link IncreasedFactoryProductionAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link IncreasedFactoryProductionAdvance} created from this proxy will always have their game data set to
 * {@code null}. Proxies that may compose instances of this proxy are required to manually restore the game data in
 * their {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class IncreasedFactoryProductionAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 8022882353266223010L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(IncreasedFactoryProductionAdvance.class, IncreasedFactoryProductionAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public IncreasedFactoryProductionAdvanceProxy(
      final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance) {
    checkNotNull(increasedFactoryProductionAdvance);

    attachments = increasedFactoryProductionAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance =
        new IncreasedFactoryProductionAdvance(null);
    attachments.forEach(increasedFactoryProductionAdvance::addAttachment);
    return increasedFactoryProductionAdvance;
  }
}
