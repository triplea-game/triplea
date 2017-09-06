package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.UnitType;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link UnitType} class.
 */
@Immutable
public final class UnitTypeProxy implements Proxy {
  private static final long serialVersionUID = 1588408878724586338L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(UnitType.class, UnitTypeProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;
  private final String name;

  public UnitTypeProxy(final UnitType unitType) {
    checkNotNull(unitType);

    attachments = unitType.getAttachments();
    gameData = unitType.getData();
    name = unitType.getName();
  }

  @Override
  public Object readResolve() {
    final UnitType unitType = new UnitType(name, gameData);
    attachments.forEach(unitType::addAttachment);
    return unitType;
  }
}
