package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.MechanizedInfantryAdvance;

/**
 * A serializable proxy for the {@link MechanizedInfantryAdvance} class.
 */
@Immutable
public final class MechanizedInfantryAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 577877043854230281L;

  public static final ProxyFactory FACTORY =
      ProxyFactory.newInstance(MechanizedInfantryAdvance.class, MechanizedInfantryAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public MechanizedInfantryAdvanceProxy(final MechanizedInfantryAdvance mechanizedInfantryAdvance) {
    checkNotNull(mechanizedInfantryAdvance);

    attachments = mechanizedInfantryAdvance.getAttachments();
    gameData = mechanizedInfantryAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final MechanizedInfantryAdvance mechanizedInfantryAdvance = new MechanizedInfantryAdvance(gameData);
    attachments.forEach(mechanizedInfantryAdvance::addAttachment);
    return mechanizedInfantryAdvance;
  }
}
