package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.DestroyerBombardTechAdvance;

/**
 * A serializable proxy for the {@link DestroyerBombardTechAdvance} class.
 */
@Immutable
public final class DestroyerBombardTechAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 6277384939381379724L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(DestroyerBombardTechAdvance.class, DestroyerBombardTechAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public DestroyerBombardTechAdvanceProxy(final DestroyerBombardTechAdvance destroyerBombardTechAdvance) {
    checkNotNull(destroyerBombardTechAdvance);

    attachments = destroyerBombardTechAdvance.getAttachments();
    gameData = destroyerBombardTechAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final DestroyerBombardTechAdvance destroyerBombardTechAdvance = new DestroyerBombardTechAdvance(gameData);
    attachments.forEach(destroyerBombardTechAdvance::addAttachment);
    return destroyerBombardTechAdvance;
  }
}
