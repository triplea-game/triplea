package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.SerializationProxySupport;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import java.io.Serializable;
import java.util.Objects;

/**
 * The Purpose of this class is to record various information about combat, in order to use it for
 * conditions and other things later.
 */
public class BattleRecord implements Serializable {
  /** A summary description of the possible results of a battle. */
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

    /**
     * have units left in the territory beside enemy defenders (like both sides have transports
     * left).
     */
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
     * No battle was fought, possibly because the territory you were about to bomb was conquered
     * before the bombing could begin, etc.
     */
    NO_BATTLE
  }

  private static final long serialVersionUID = 3642216371483289106L;
  private Territory battleSite;
  private PlayerId attacker;
  private PlayerId defender;
  private int attackerLostTuv = 0;
  private int defenderLostTuv = 0;
  private BattleResultDescription battleResultDescription;
  private final BattleType battleType;
  private BattleResults battleResults;

  /** Convenience copy constructor. */
  protected BattleRecord(final BattleRecord record) {
    battleSite = record.battleSite;
    attacker = record.attacker;
    defender = record.defender;
    attackerLostTuv = record.attackerLostTuv;
    defenderLostTuv = record.defenderLostTuv;
    battleResultDescription = record.battleResultDescription;
    battleType = record.battleType;
    battleResults = record.battleResults;
  }

  protected BattleRecord(
      final Territory battleSite, final PlayerId attacker, final BattleType battleType) {
    this.battleSite = battleSite;
    this.attacker = attacker;
    this.battleType = battleType;
  }

  @SerializationProxySupport
  private BattleRecord(
      final Territory battleSite,
      final PlayerId attacker,
      final PlayerId defender,
      final int attackerLostTuv,
      final int defenderLostTuv,
      final BattleResultDescription battleResultDescription,
      final BattleType battleType,
      final BattleResults battleResults) {
    this.battleSite = battleSite;
    this.attacker = attacker;
    this.defender = defender;
    this.attackerLostTuv = attackerLostTuv;
    this.defenderLostTuv = defenderLostTuv;
    this.battleResultDescription = battleResultDescription;
    this.battleType = battleType;
    this.battleResults = battleResults;
  }

  @SerializationProxySupport
  public Object writeReplace(@SuppressWarnings("unused") final Object write) {
    return new SerializationProxy(this);
  }

  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 355188139820567143L;
    private final Territory battleSite;
    private final PlayerId attacker;
    private final PlayerId defender;
    private final int attackerLostTuv;
    private final int defenderLostTuv;
    private final BattleResultDescription battleResultDescription;
    private final BattleType battleType;
    private final BattleResults battleResults;

    SerializationProxy(final BattleRecord battleRecord) {
      battleSite = battleRecord.battleSite;
      attacker = battleRecord.attacker;
      defender = battleRecord.defender;
      attackerLostTuv = battleRecord.attackerLostTuv;
      defenderLostTuv = battleRecord.defenderLostTuv;
      battleResultDescription = battleRecord.battleResultDescription;
      battleType = battleRecord.battleType;
      battleResults = battleRecord.battleResults;
    }

    protected Object readResolve() {
      return new BattleRecord(
          battleSite,
          attacker,
          defender,
          attackerLostTuv,
          defenderLostTuv,
          battleResultDescription,
          battleType,
          battleResults);
    }
  }

  protected void setResult(
      final PlayerId defender,
      final int attackerLostTuv,
      final int defenderLostTuv,
      final BattleResultDescription battleResultDescription,
      final BattleResults battleResults) {
    this.defender = defender;
    this.attackerLostTuv = attackerLostTuv;
    this.defenderLostTuv = defenderLostTuv;
    this.battleResultDescription = battleResultDescription;
    this.battleResults = battleResults;
  }

  protected Territory getBattleSite() {
    return battleSite;
  }

  protected void setBattleSite(final Territory battleSite) {
    this.battleSite = battleSite;
  }

  protected PlayerId getAttacker() {
    return attacker;
  }

  protected void setAttacker(final PlayerId attacker) {
    this.attacker = attacker;
  }

  protected PlayerId getDefender() {
    return defender;
  }

  protected void setDefenders(final PlayerId defender) {
    this.defender = defender;
  }

  int getAttackerLostTuv() {
    return attackerLostTuv;
  }

  int getDefenderLostTuv() {
    return defenderLostTuv;
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
    if (!(o instanceof BattleRecord)) {
      return false;
    }
    final BattleRecord other = (BattleRecord) o;
    return other.battleSite.equals(this.battleSite)
        && other.battleType == this.battleType
        && other.attacker.equals(this.attacker);
  }

  @Override
  public String toString() {
    return battleType + " battle in " + battleSite;
  }
}
