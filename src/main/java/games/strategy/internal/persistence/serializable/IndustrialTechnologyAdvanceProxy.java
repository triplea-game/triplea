package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.IndustrialTechnologyAdvance;

/**
 * A serializable proxy for the {@link IndustrialTechnologyAdvance} class.
 */
@Immutable
public final class IndustrialTechnologyAdvanceProxy implements Proxy {
  private static final long serialVersionUID = -2023831912614845534L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(IndustrialTechnologyAdvance.class, IndustrialTechnologyAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public IndustrialTechnologyAdvanceProxy(final IndustrialTechnologyAdvance industrialTechnologyAdvance) {
    checkNotNull(industrialTechnologyAdvance);

    attachments = industrialTechnologyAdvance.getAttachments();
    gameData = industrialTechnologyAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final IndustrialTechnologyAdvance industrialTechnologyAdvance = new IndustrialTechnologyAdvance(gameData);
    attachments.forEach(industrialTechnologyAdvance::addAttachment);
    return industrialTechnologyAdvance;
  }
}
