package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.Objects;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.SerializationProxySupport;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;

/**
 * The Purpose of this class is to record various information about combat,
 * in order to use it for conditions and other things later.
 */
public class BattleRecord implements Serializable {

  public enum BattleResultDescription {
    /** conquered without a fight. */
    BLITZED,

    /** fought, won, and took over territory if land or convoy. */
    CONQUERED,

    /** fought, won, did not take over territory (could be water, or could be air attackers). */
    WON_WITHOUT_CONQUERING,

    /**
     * fought, enemy either submerged or the battle is over with our objectives successful even
     * though enemies are left.
     */
    WON_WITH_ENEMY_LEFT,

    /** have units left in the territory beside enemy defenders (like both sides have transports left). */
    STALEMATE,

    /** either lost the battle, or retreated. */
    LOST,

    /** Successfully bombed something. */
    BOMBED,

    /** Won an Air Battle with units surviving. */
    AIR_BATTLE_WON,

    /** Lost an Air Battle with enemy units surviving. */
    AIR_BATTLE_LOST,

    /** Neither side has air units left. */
    AIR_BATTLE_STALEMATE,

    /**
     * No battle was fought, possibly because the territory you were about to bomb was conquered before the
     * bombing could begin,
     * etc.
     */
    NO_BATTLE
  }


  private static final long serialVersionUID = 3642216371483289106L;
  private Territory battleSite;
  private PlayerID attacker;
  private PlayerID defender;
  private int attackerLostTUV = 0;
  private int defenderLostTUV = 0;
  private BattleResultDescription battleResultDescription;
  private final BattleType battleType;
  private BattleResults battleResults;


  @SerializationProxySupport
  private BattleRecord(final Territory battleSite, final PlayerID attacker, final PlayerID defender,
      final int attackerLostTuv,
      final int defenderLostTuv, final BattleResultDescription battleResultDescription, final BattleType battleType,
      final BattleResults battleResults) {
    this.battleSite = battleSite;
    this.attacker = attacker;
    this.defender = defender;
    this.attackerLostTUV = attackerLostTuv;
    this.defenderLostTUV = defenderLostTuv;
    this.battleResultDescription = battleResultDescription;
    this.battleType = battleType;
    this.battleResults = battleResults;
  }

  @SerializationProxySupport
  public Object writeReplace(final Object write) {
    return new SerializationProxy(this);
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 355188139820567143L;
    private final Territory battleSite;
    private final PlayerID attacker;
    private final PlayerID defender;
    private final int attackerLostTUV;
    private final int defenderLostTUV;
    private final BattleResultDescription battleResultDescription;
    private final BattleType battleType;
    private final BattleResults battleResults;

    public SerializationProxy(final BattleRecord battleRecord) {
      battleSite = battleRecord.battleSite;
      attacker = battleRecord.attacker;
      defender = battleRecord.defender;
      attackerLostTUV = battleRecord.attackerLostTUV;
      defenderLostTUV = battleRecord.defenderLostTUV;
      battleResultDescription = battleRecord.battleResultDescription;
      battleType = battleRecord.battleType;
      battleResults = battleRecord.battleResults;
    }

    protected Object readResolve() {
      return new BattleRecord(battleSite, attacker, defender, attackerLostTUV, defenderLostTUV, battleResultDescription,
          battleType, battleResults);
    }
  }

  /**
   * Convenience copy constructor.
   */
  protected BattleRecord(final BattleRecord record) {
    battleSite = record.battleSite;
    attacker = record.attacker;
    defender = record.defender;
    attackerLostTUV = record.attackerLostTUV;
    defenderLostTUV = record.defenderLostTUV;
    battleResultDescription = record.battleResultDescription;
    battleType = record.battleType;
    battleResults = record.battleResults;
  }

  protected BattleRecord(final Territory battleSite, final PlayerID attacker, final BattleType battleType) {
    this.battleSite = battleSite;
    this.attacker = attacker;
    this.battleType = battleType;
  }

  protected void setResult(final PlayerID defender, final int attackerLostTuv, final int defenderLostTuv,
      final BattleResultDescription battleResultDescription, final BattleResults battleResults) {
    this.defender = defender;
    this.attackerLostTUV = attackerLostTuv;
    this.defenderLostTUV = defenderLostTuv;
    this.battleResultDescription = battleResultDescription;
    this.battleResults = battleResults;
  }

  protected Territory getBattleSite() {
    return battleSite;
  }

  protected void setBattleSite(final Territory battleSite) {
    this.battleSite = battleSite;
  }

  protected PlayerID getAttacker() {
    return attacker;
  }

  protected void setAttacker(final PlayerID attacker) {
    this.attacker = attacker;
  }

  protected PlayerID getDefender() {
    return defender;
  }

  protected void setDefenders(final PlayerID defender) {
    this.defender = defender;
  }

  int getAttackerLostTuv() {
    return attackerLostTUV;
  }

  int getDefenderLostTuv() {
    return defenderLostTUV;
  }

  protected BattleType getBattleType() {
    return battleType;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(battleSite);
  }

  @Override
  public boolean equals(final Object o) {
    if ((o == null) || !(o instanceof BattleRecord)) {
      return false;
    }
    final BattleRecord other = (BattleRecord) o;
    return other.battleSite.equals(this.battleSite) && (other.battleType == this.battleType)
        && other.attacker.equals(this.attacker);
  }

  @Override
  public String toString() {
    return battleType + " battle in " + battleSite;
  }
}
