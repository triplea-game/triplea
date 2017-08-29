package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.HeavyBomberAdvance;

/**
 * A serializable proxy for the {@link HeavyBomberAdvance} class.
 */
@Immutable
public final class HeavyBomberAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1868161097462691906L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(HeavyBomberAdvance.class, HeavyBomberAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public HeavyBomberAdvanceProxy(final HeavyBomberAdvance heavyBomberAdvance) {
    checkNotNull(heavyBomberAdvance);

    attachments = heavyBomberAdvance.getAttachments();
    gameData = heavyBomberAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final HeavyBomberAdvance heavyBomberAdvance = new HeavyBomberAdvance(gameData);
    attachments.forEach(heavyBomberAdvance::addAttachment);
    return heavyBomberAdvance;
  }
}
