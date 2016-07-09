package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.SerializationProxySupport;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;

/**
 * The Purpose of this class is to record various information about combat,
 * in order to use it for conditions and other things later.
 */
public class BattleRecord implements Serializable {

  /**
   * BLITZED = conquered without a fight <br>
   * CONQUERED = fought, won, and took over territory if land or convoy <br>
   * WON_WITHOUT_CONQUERING = fought, won, did not take over territory (could be water, or could be air attackers) <br>
   * WON_WITH_ENEMY_LEFT = fought, enemy either submerged or the battle is over with our objectives successful even
   * though enemies are left
   * <br>
   * STALEMATE = have units left in the territory beside enemy defenders (like both sides have transports left) <br>
   * LOST = either lost the battle, or retreated <br>
   * BOMBED = Successfully bombed something <br>
   * AIR_BATTLE_WON = Won an Air Battle with units surviving <br>
   * AIR_BATTLE_LOST = Lost an Air Battle with enemy units surviving <br>
   * AIR_BATTLE_STALEMATE = Neither side has air units left <br>
   * NO_BATTLE = No battle was fought, possibly because the territory you were about to bomb was conquered before the
   * bombing could begin,
   * etc.<br>
   */
  public enum BattleResultDescription {
    BLITZED, CONQUERED, WON_WITHOUT_CONQUERING, WON_WITH_ENEMY_LEFT, STALEMATE, LOST, BOMBED, AIR_BATTLE_WON, AIR_BATTLE_LOST, AIR_BATTLE_STALEMATE, NO_BATTLE
  }


  private static final long serialVersionUID = 3642216371483289106L;
  private Territory m_battleSite;
  private PlayerID m_attacker;
  private PlayerID m_defender;
  private int m_attackerLostTUV = 0;
  private int m_defenderLostTUV = 0;
  private BattleResultDescription m_battleResultDescription;
  private BattleType m_battleType;
  private BattleResults m_battleResults;


  @SerializationProxySupport
  public Object writeReplace(Object write) {
    return new SerializationProxy(this);
  }

  @SerializationProxySupport
  private BattleRecord(Territory battleSite, PlayerID attacker, PlayerID defender, int attackerLostTUV,
      int defenderLostTUV, BattleResultDescription battleResultDescription, BattleType battleType,
      BattleResults battleResults) {
    this.m_battleSite = battleSite;
    this.m_attacker = attacker;
    this.m_defender = defender;
    this.m_attackerLostTUV = attackerLostTUV;
    this.m_defenderLostTUV = defenderLostTUV;
    this.m_battleResultDescription = battleResultDescription;
    this.m_battleType = battleType;
    this.m_battleResults = battleResults;
  }


  private static class SerializationProxy implements Serializable {
    private final Territory battleSite;
    private final PlayerID attacker;
    private final PlayerID defender;
    private final int attackerLostTUV;
    private final int defenderLostTUV;
    private final BattleResultDescription battleResultDescription;
    private final BattleType battleType;
    private final BattleResults battleResults;

    public SerializationProxy(BattleRecord battleRecord) {
      battleSite = battleRecord.m_battleSite;
      attacker = battleRecord.m_attacker;
      defender = battleRecord.m_defender;
      attackerLostTUV = battleRecord.m_attackerLostTUV;
      defenderLostTUV = battleRecord.m_defenderLostTUV;
      battleResultDescription = battleRecord.m_battleResultDescription;
      battleType = battleRecord.m_battleType;
      battleResults = battleRecord.m_battleResults;
    }

    private Object readResolve() {
      return new BattleRecord(battleSite, attacker, defender, attackerLostTUV, defenderLostTUV, battleResultDescription,
          battleType, battleResults);
    }
  }


  protected BattleRecord(final BattleRecord record) {
    m_battleSite = record.m_battleSite;
    m_attacker = record.m_attacker;
    m_defender = record.m_defender;
    m_attackerLostTUV = record.m_attackerLostTUV;
    m_defenderLostTUV = record.m_defenderLostTUV;
    m_battleResultDescription = record.m_battleResultDescription;
    m_battleType = record.m_battleType;
    m_battleResults = record.m_battleResults;
  }

  protected BattleRecord(final Territory battleSite, final PlayerID attacker, final BattleType battleType,
      final GameData data) {
    m_battleSite = battleSite;
    m_attacker = attacker;
    m_battleType = battleType;
  }

  protected void setResult(final PlayerID defender, final int attackerLostTUV, final int defenderLostTUV,
      final BattleResultDescription battleResultDescription, final BattleResults battleResults) {
    m_defender = defender;
    m_attackerLostTUV = attackerLostTUV;
    m_defenderLostTUV = defenderLostTUV;
    m_battleResultDescription = battleResultDescription;
    m_battleResults = battleResults;
  }

  protected Territory getBattleSite() {
    return m_battleSite;
  }

  protected void setBattleSite(final Territory battleSite) {
    this.m_battleSite = battleSite;
  }

  protected PlayerID getAttacker() {
    return m_attacker;
  }

  protected void setAttacker(final PlayerID attacker) {
    this.m_attacker = attacker;
  }

  protected PlayerID getDefender() {
    return m_defender;
  }

  protected void setDefenders(final PlayerID defender) {
    this.m_defender = defender;
  }

  protected int getAttackerLostTUV() {
    return m_attackerLostTUV;
  }

  protected int getDefenderLostTUV() {
    return m_defenderLostTUV;
  }

  protected BattleType getBattleType() {
    return m_battleType;
  }

  @Override
  public int hashCode() {
    return m_battleSite.hashCode();
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || !(o instanceof BattleRecord)) {
      return false;
    }
    final BattleRecord other = (BattleRecord) o;
    return other.m_battleSite.equals(this.m_battleSite) && other.m_battleType.equals(this.m_battleType)
        && other.m_attacker.equals(this.m_attacker);
  }

  @Override
  public String toString() {
    return m_battleType + " battle in " + m_battleSite;
  }
}
