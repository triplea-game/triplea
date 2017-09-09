package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.RocketsAdvance;

/**
 * A serializable proxy for the {@link RocketsAdvance} class.
 */
@Immutable
public final class RocketsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 4534720875109845955L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(RocketsAdvance.class, RocketsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public RocketsAdvanceProxy(final RocketsAdvance rocketsAdvance) {
    checkNotNull(rocketsAdvance);

    attachments = rocketsAdvance.getAttachments();
    gameData = rocketsAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final RocketsAdvance rocketsAdvance = new RocketsAdvance(gameData);
    attachments.forEach(rocketsAdvance::addAttachment);
    return rocketsAdvance;
  }
}
