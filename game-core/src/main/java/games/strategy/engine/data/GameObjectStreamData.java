package games.strategy.engine.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GameObjectStreamData implements Externalizable {
  private static final long serialVersionUID = 740501183336843321L;

  enum GameType {
    PLAYERID, UNITTYPE, TERRITORY, PRODUCTIONRULE, PRODUCTIONFRONTIER
  }

  public static boolean canSerialize(final Named obj) {
    return (obj instanceof PlayerID) || (obj instanceof UnitType) || (obj instanceof Territory)
        || (obj instanceof ProductionRule) || (obj instanceof IAttachment) || (obj instanceof ProductionFrontier);
  }

  private String name;
  private GameType type;

  public GameObjectStreamData() {}

  /**
   * Creates a new instance of GameObjectStreamData.
   *
   * @param named
   *        named entity
   */
  public GameObjectStreamData(final Named named) {
    name = named.getName();
    if (named instanceof PlayerID) {
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
      throw new IllegalArgumentException("Wrong type:" + named);
    }
  }

  Named getReference(final GameData data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cant be null");
    }
    data.acquireReadLock();
    try {
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
    } finally {
      data.releaseReadLock();
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
