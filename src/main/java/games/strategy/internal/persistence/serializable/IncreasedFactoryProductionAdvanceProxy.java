package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.IncreasedFactoryProductionAdvance;

/**
 * A serializable proxy for the {@link IncreasedFactoryProductionAdvance} class.
 */
@Immutable
public final class IncreasedFactoryProductionAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 8022882353266223010L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(IncreasedFactoryProductionAdvance.class, IncreasedFactoryProductionAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public IncreasedFactoryProductionAdvanceProxy(
      final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance) {
    checkNotNull(increasedFactoryProductionAdvance);

    attachments = increasedFactoryProductionAdvance.getAttachments();
    gameData = increasedFactoryProductionAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final IncreasedFactoryProductionAdvance increasedFactoryProductionAdvance =
        new IncreasedFactoryProductionAdvance(gameData);
    attachments.forEach(increasedFactoryProductionAdvance::addAttachment);
    return increasedFactoryProductionAdvance;
  }
}
