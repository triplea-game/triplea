package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;

/**
 * A serializable proxy for the {@link IndustrialTechnologyAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link IndustrialTechnologyAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class IndustrialTechnologyAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -2023831912614845534L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(IndustrialTechnologyAdvance.class, IndustrialTechnologyAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public IndustrialTechnologyAdvanceProxy(final IndustrialTechnologyAdvance industrialTechnologyAdvance) {
    checkNotNull(industrialTechnologyAdvance);

    attachments = industrialTechnologyAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final IndustrialTechnologyAdvance industrialTechnologyAdvance = new IndustrialTechnologyAdvance(null);
    attachments.forEach(industrialTechnologyAdvance::addAttachment);
    return industrialTechnologyAdvance;
  }
}
