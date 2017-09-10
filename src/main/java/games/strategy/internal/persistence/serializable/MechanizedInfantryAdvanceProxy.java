package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

/**
 * A serializable proxy for the {@link MechanizedInfantryAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of
 * {@link MechanizedInfantryAdvance} created from this proxy will always have their game data set to {@code null}.
 * Proxies that may compose instances of this proxy are required to manually restore the game data in their
 * {@code readResolve()} method via a context-dependent mechanism.
 * </p>
 */
@Immutable
public final class MechanizedInfantryAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 577877043854230281L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(MechanizedInfantryAdvance.class, MechanizedInfantryAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public MechanizedInfantryAdvanceProxy(final MechanizedInfantryAdvance mechanizedInfantryAdvance) {
    checkNotNull(mechanizedInfantryAdvance);

    attachments = mechanizedInfantryAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final MechanizedInfantryAdvance mechanizedInfantryAdvance = new MechanizedInfantryAdvance(null);
    attachments.forEach(mechanizedInfantryAdvance::addAttachment);
    return mechanizedInfantryAdvance;
  }
}
