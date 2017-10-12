package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.RepairFrontier;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Unit;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.util.IntegerMap;

/**
 * A serializable proxy for the {@link PlayerID} class.
 *
 * <p>
 * This proxy does not serialize the game data owner to avoid a circular reference. Instances of {@link PlayerID}
 * created from this proxy will always have their game data set to {@code null}. Proxies that may compose instances of
 * this proxy are required to manually restore the game data in their {@code readResolve()} method via a
 * context-dependent mechanism.
 * </p>
 */
@Immutable
public final class PlayerIdProxy implements Proxy {
  private static final long serialVersionUID = -8737466123516860123L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(PlayerID.class, PlayerIdProxy::new);

  private final Map<String, IAttachment> attachments;
  private final String defaultType;
  private final boolean disableable;
  private final boolean disabled;
  private final boolean hidden;
  private final String name;
  private final boolean optional;
  private final ProductionFrontier productionFrontier;
  private final RepairFrontier repairFrontier;
  private final IntegerMap<Resource> resources;
  private final Collection<TechnologyFrontier> technologyFrontiers;
  private final Collection<Unit> units;
  private final String whoAmI;

  public PlayerIdProxy(final PlayerID playerId) {
    checkNotNull(playerId);

    attachments = playerId.getAttachments();
    defaultType = playerId.getDefaultType();
    disableable = playerId.getCanBeDisabled();
    disabled = playerId.getIsDisabled();
    hidden = playerId.isHidden();
    name = playerId.getName();
    optional = playerId.getOptional();
    productionFrontier = playerId.getProductionFrontier();
    repairFrontier = playerId.getRepairFrontier();
    resources = playerId.getResources().getResourcesCopy();
    technologyFrontiers = playerId.getTechnologyFrontierList().getFrontiers();
    units = playerId.getUnits().getUnits();
    whoAmI = playerId.getWhoAmI();
  }

  @Override
  public Object readResolve() {
    final PlayerID playerId = new PlayerID(name, optional, disableable, defaultType, hidden, null);
    playerId.setIsDisabled(disabled);
    playerId.setProductionFrontier(productionFrontier);
    playerId.setRepairFrontier(repairFrontier);
    playerId.setWhoAmI(whoAmI);
    attachments.forEach(playerId::addAttachment);
    playerId.getResources().add(resources);
    playerId.getTechnologyFrontierList().addTechnologyFrontiers(technologyFrontiers);
    units.forEach(unit -> {
      unit.setOwner(playerId);
      playerId.getUnits().add(unit);
    });
    return playerId;
  }
}
