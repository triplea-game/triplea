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
    return obj instanceof PlayerID || obj instanceof UnitType || obj instanceof Territory
        || obj instanceof ProductionRule || obj instanceof IAttachment || obj instanceof ProductionFrontier;
  }

  private String m_name;
  private GameType m_type;

  public GameObjectStreamData() {}

  /**
   * Creates a new instance of GameObjectStreamData.
   *
   * @param named
   *        named entity
   */
  public GameObjectStreamData(final Named named) {
    m_name = named.getName();
    if (named instanceof PlayerID) {
      m_type = GameType.PLAYERID;
    } else if (named instanceof Territory) {
      m_type = GameType.TERRITORY;
    } else if (named instanceof UnitType) {
      m_type = GameType.UNITTYPE;
    } else if (named instanceof ProductionRule) {
      m_type = GameType.PRODUCTIONRULE;
    } else if (named instanceof ProductionFrontier) {
      m_type = GameType.PRODUCTIONFRONTIER;
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
      switch (m_type) {
        case PLAYERID:
          return data.getPlayerList().getPlayerID(m_name);
        case TERRITORY:
          return data.getMap().getTerritory(m_name);
        case UNITTYPE:
          return data.getUnitTypeList().getUnitType(m_name);
        case PRODUCTIONRULE:
          return data.getProductionRuleList().getProductionRule(m_name);
        case PRODUCTIONFRONTIER:
          return data.getProductionFrontierList().getProductionFrontier(m_name);
      }
      throw new IllegalStateException("Unknown type" + this);
    } finally {
      data.releaseReadLock();
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    m_name = (String) in.readObject();
    m_type = GameType.values()[in.readByte()];
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(m_name);
    out.writeByte((byte) m_type.ordinal());
  }
}
