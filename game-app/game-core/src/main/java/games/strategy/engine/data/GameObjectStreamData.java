package games.strategy.engine.data;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A handle to a {@link Named} object within a game object stream.
 *
 * <p>Used to serialize an object's identity without serializing the object itself. Typically used
 * by network game peers to identify the target of a game operation, where the target object already
 * exists in each game process.
 */
public class GameObjectStreamData implements Externalizable {
  private static final long serialVersionUID = 740501183336843321L;

  private String name;
  private GameType type;

  public GameObjectStreamData() {}

  public GameObjectStreamData(final Named named) {
    name = named.getName();
    if (named instanceof GamePlayer) {
      type = GameType.PLAYERID;
    } else if (named instanceof Territory) {
      type = GameType.TERRITORY;
    } else if (named instanceof UnitType) {
      type = GameType.UNITTYPE;
    } else if (named instanceof ProductionRule) {
      type = GameType.PRODUCTIONRULE;
    } else if (named instanceof ProductionFrontier) {
      type = GameType.PRODUCTIONFRONTIER;
    } else {
      throw new IllegalArgumentException("Wrong type: " + named);
    }
  }

  enum GameType {
    PLAYERID,
    UNITTYPE,
    TERRITORY,
    PRODUCTIONRULE,
    PRODUCTIONFRONTIER
  }

  public static boolean canSerialize(final Named obj) {
    return obj instanceof GamePlayer
        || obj instanceof UnitType
        || obj instanceof Territory
        || obj instanceof ProductionRule
        || obj instanceof IAttachment
        || obj instanceof ProductionFrontier;
  }

  Named getReference(final GameData data) {
    checkNotNull(data);

    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      switch (type) {
        case PLAYERID:
          return data.getPlayerList().getPlayerId(name);
        case TERRITORY:
          return data.getMap().getTerritory(name);
        case UNITTYPE:
          return data.getUnitTypeList().getUnitType(name);
        case PRODUCTIONRULE:
          return data.getProductionRuleList().getProductionRule(name);
        case PRODUCTIONFRONTIER:
          return data.getProductionFrontierList().getProductionFrontier(name);
        default:
          throw new IllegalStateException("Unknown type: " + type);
      }
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    name = (String) in.readObject();
    type = GameType.values()[in.readByte()];
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(name);
    out.writeByte((byte) type.ordinal());
  }
}
