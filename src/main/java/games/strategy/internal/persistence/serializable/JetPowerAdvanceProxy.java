package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.JetPowerAdvance;

/**
 * A serializable proxy for the {@link JetPowerAdvance} class.
 */
@Immutable
public final class JetPowerAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 3192236142399543887L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(JetPowerAdvance.class, JetPowerAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public JetPowerAdvanceProxy(final JetPowerAdvance jetPowerAdvance) {
    checkNotNull(jetPowerAdvance);

    attachments = jetPowerAdvance.getAttachments();
    gameData = jetPowerAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final JetPowerAdvance jetPowerAdvance = new JetPowerAdvance(gameData);
    attachments.forEach(jetPowerAdvance::addAttachment);
    return jetPowerAdvance;
  }
}
