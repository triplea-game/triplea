package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.WarBondsAdvance;

/**
 * A serializable proxy for the {@link WarBondsAdvance} class.
 */
@Immutable
public final class WarBondsAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -204344267560116349L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(WarBondsAdvance.class, WarBondsAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public WarBondsAdvanceProxy(final WarBondsAdvance warBondsAdvance) {
    checkNotNull(warBondsAdvance);

    attachments = warBondsAdvance.getAttachments();
    gameData = warBondsAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final WarBondsAdvance warBondsAdvance = new WarBondsAdvance(gameData);
    attachments.forEach(warBondsAdvance::addAttachment);
    return warBondsAdvance;
  }
}
