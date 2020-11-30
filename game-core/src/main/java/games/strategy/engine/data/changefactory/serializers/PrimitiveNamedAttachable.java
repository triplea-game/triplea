package games.strategy.engine.data.changefactory.serializers;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Named;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.TechAdvance;
import java.io.Serializable;

/**
 * Serializes NamedAttachments into a primitive form that can be deserialized with access to
 * GameData
 */
public class PrimitiveNamedAttachable implements Serializable {
  private static final long serialVersionUID = 740501183336843321L;

  private final String name;
  private final GameType type;

  public PrimitiveNamedAttachable(final NamedAttachable named) {
    name = named.getName();
    if (named instanceof GamePlayer) {
      type = GameType.PLAYERID;
    } else if (named instanceof Territory) {
      type = GameType.TERRITORY;
    } else if (named instanceof UnitType) {
      type = GameType.UNITTYPE;
    } else if (named instanceof RelationshipType) {
      type = GameType.RELATIONSHIPTYPE;
    } else if (named instanceof Resource) {
      type = GameType.RESOURCE;
    } else if (named instanceof TerritoryEffect) {
      type = GameType.TERRITORYEFFECT;
    } else if (named instanceof TechAdvance) {
      type = GameType.TECHADVANCE;
    } else {
      throw new IllegalArgumentException("Wrong type:" + named);
    }
  }

  enum GameType {
    PLAYERID,
    UNITTYPE,
    TERRITORY,
    RELATIONSHIPTYPE,
    RESOURCE,
    TERRITORYEFFECT,
    TECHADVANCE
  }

  public Named getReference(final GameData data) {
    checkNotNull(data);

    data.acquireReadLock();
    try {
      switch (type) {
        case PLAYERID:
          return data.getPlayerList().getPlayerId(name);
        case TERRITORY:
          return data.getMap().getTerritory(name);
        case UNITTYPE:
          return data.getUnitTypeList().getUnitType(name);
        case RELATIONSHIPTYPE:
          return data.getRelationshipTypeList().getRelationshipType(name);
        case RESOURCE:
          return data.getResourceList().getResource(name);
        case TERRITORYEFFECT:
          return data.getTerritoryEffectList().get(name);
        case TECHADVANCE:
          return data.getTechnologyFrontier().getAdvanceByName(name);
        default:
          throw new IllegalStateException("Unknown type: " + type);
      }
    } finally {
      data.releaseReadLock();
    }
  }
}
