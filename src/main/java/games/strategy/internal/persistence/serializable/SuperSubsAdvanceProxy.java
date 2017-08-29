package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.SuperSubsAdvance;

/**
 * A serializable proxy for the {@link SuperSubsAdvance} class.
 */
@Immutable
public final class SuperSubsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 6085537658742849644L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(SuperSubsAdvance.class, SuperSubsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public SuperSubsAdvanceProxy(final SuperSubsAdvance superSubsAdvance) {
    checkNotNull(superSubsAdvance);

    attachments = superSubsAdvance.getAttachments();
    gameData = superSubsAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final SuperSubsAdvance superSubsAdvance = new SuperSubsAdvance(gameData);
    attachments.forEach(superSubsAdvance::addAttachment);
    return superSubsAdvance;
  }
}
