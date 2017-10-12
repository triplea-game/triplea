package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.JetPowerAdvance;

/**
 * A serializable proxy for the {@link JetPowerAdvance} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link JetPowerAdvance}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class JetPowerAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 3192236142399543887L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(JetPowerAdvance.class, JetPowerAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;

  public JetPowerAdvanceProxy(final JetPowerAdvance jetPowerAdvance) {
    checkNotNull(jetPowerAdvance);

    attachments = jetPowerAdvance.getAttachments();
  }

  @Override
  public Object readResolve() {
    final JetPowerAdvance jetPowerAdvance = new JetPowerAdvance(null);
    attachments.forEach(jetPowerAdvance::addAttachment);
    return jetPowerAdvance;
  }
}
