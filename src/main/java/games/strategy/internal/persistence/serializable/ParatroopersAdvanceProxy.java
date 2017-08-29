package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.ParatroopersAdvance;

/**
 * A serializable proxy for the {@link ParatroopersAdvance} class.
 */
@Immutable
public final class ParatroopersAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -1462954863526418801L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(ParatroopersAdvance.class, ParatroopersAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public ParatroopersAdvanceProxy(final ParatroopersAdvance paratroopersAdvance) {
    checkNotNull(paratroopersAdvance);

    attachments = paratroopersAdvance.getAttachments();
    gameData = paratroopersAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final ParatroopersAdvance paratroopersAdvance = new ParatroopersAdvance(gameData);
    attachments.forEach(paratroopersAdvance::addAttachment);
    return paratroopersAdvance;
  }
}
