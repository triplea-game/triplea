package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.UnitType;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;

/**
 * A serializable proxy for the {@link UnitType} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link UnitType}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class UnitTypeProxy implements Proxy {
  private static final long serialVersionUID = 1588408878724586338L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(UnitType.class, UnitTypeProxy::new);

  private final Map<String, IAttachment> attachments;
  private final String name;

  public UnitTypeProxy(final UnitType unitType) {
    checkNotNull(unitType);

    attachments = unitType.getAttachments();
    name = unitType.getName();
  }

  @Override
  public Object readResolve() {
    final UnitType unitType = new UnitType(name, null);
    attachments.forEach(unitType::addAttachment);
    return unitType;
  }
}
