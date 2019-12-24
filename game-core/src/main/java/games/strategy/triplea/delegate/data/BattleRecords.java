package games.strategy.triplea.delegate.data;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.SerializationProxySupport;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.delegate.battle.IBattle.BattleType;
import games.strategy.triplea.delegate.data.BattleRecord.BattleResultDescription;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * The Purpose of this class is to record various information about combat, in order to use it for
 * conditions and other things later.
 */
public class BattleRecords implements Serializable {
  private static final long serialVersionUID = 1473664374777905497L;

  private final Map<PlayerId, Map<UUID, BattleRecord>> records;

  public BattleRecords() {
    this.records = new HashMap<>();
  }

  public BattleRecords(final Map<PlayerId, Map<UUID, BattleRecord>> records) {
    this.records = records;
  }

  // Create copy
  public BattleRecords(final BattleRecords records) {
    this.records = new HashMap<>();
    for (final Entry<PlayerId, Map<UUID, BattleRecord>> entry : records.records.entrySet()) {
      final PlayerId p = entry.getKey();
      final Map<UUID, BattleRecord> record = entry.getValue();
      final Map<UUID, BattleRecord> map = new HashMap<>();
      for (final Entry<UUID, BattleRecord> entry2 : record.entrySet()) {
        map.put(entry2.getKey(), new BattleRecord(entry2.getValue()));
      }
      this.records.put(p, map);
    }
  }

  @SerializationProxySupport
  public Object writeReplace() {
    return new SerializationProxy(this);
  }

  @SerializationProxySupport
  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 3837818110273155404L;
    private final Map<PlayerId, Map<UUID, BattleRecord>> records;

    SerializationProxy(final BattleRecords battleRecords) {
      this.records = battleRecords.records;
    }

    private Object readResolve() {
      return new BattleRecords(records);
    }
  }

  public static Collection<BattleRecord> getAllRecords(final BattleRecords brs) {
    final Collection<BattleRecord> records = new ArrayList<>();
    for (final Map<UUID, BattleRecord> playerMap : brs.records.values()) {
      records.addAll(playerMap.values());
    }
    return records;
  }

  public static Collection<BattleRecord> getRecordsForPlayerId(
      final PlayerId player, final BattleRecords brs) {
    final Collection<BattleRecord> playerRecords = new ArrayList<>();
    if (brs.records.get(player) == null) {
      return playerRecords;
    }
    for (final Entry<UUID, BattleRecord> entry : brs.records.get(player).entrySet()) {
      playerRecords.add(entry.getValue());
    }
    return playerRecords;
  }

  /**
   * Returns the amount of TUV lost by either the attacker or defender for all the specified
   * battles.
   */
  public static int getLostTuvForBattleRecords(
      final Collection<BattleRecord> brs,
      final boolean attackerLostTuv,
      final boolean includeNullPlayer) {
    int lostTuv = 0;
    for (final BattleRecord br : brs) {
      if (!includeNullPlayer
          && (br.getDefender() == null
              || br.getAttacker() == null
              || br.getDefender().isNull()
              || br.getAttacker().isNull())) {
        continue;
      }
      if (attackerLostTuv) {
        lostTuv += br.getAttackerLostTuv();
      } else {
        lostTuv += br.getDefenderLostTuv();
      }
    }
    return lostTuv;
  }

  /**
   * Indicates there was a battle in any of the specified territories matching the specified
   * criteria.
   */
  public static boolean getWereThereBattlesInTerritoriesMatching(
      final Collection<BattleRecord> brs,
      final PlayerId attacker,
      final PlayerId defender,
      final String battleType,
      final Collection<Territory> anyOfTheseTerritories) {
    for (final BattleRecord br : brs) {
      if (anyOfTheseTerritories.contains(br.getBattleSite())) {
        if (attacker != null && !attacker.equals(br.getAttacker())) {
          continue;
        }
        if (defender != null && !defender.equals(br.getDefender())) {
          continue;
        }
        if (!battleType.equalsIgnoreCase("any")) {
          continue;
        }
        return true;
        // TODO: do more types.... (maybe make a much better enum class that covers both WhoWon and
        // BattleResultDescription in a single enum with multiple variables for each enum to cover
        // the different tiers
        // of detail (ie: won/lost/draw vs conquer/blitz/etc.)
      }
    }
    return false;
  }

  /** Removes the battle with the specified ID from this list of battles. */
  public void removeBattle(final PlayerId currentPlayer, final UUID battleId) {
    final Map<UUID, BattleRecord> current = records.get(currentPlayer);
    // we can't count on this being the current player. If we created a battle using edit mode, then
    // the battle might be
    // under a different player.
    if (current == null || !current.containsKey(battleId)) {
      for (final Entry<PlayerId, Map<UUID, BattleRecord>> entry : records.entrySet()) {
        if (entry.getValue() != null && entry.getValue().containsKey(battleId)) {
          entry.getValue().remove(battleId);
          return;
        }
      }
      throw new IllegalStateException(
          "Trying to remove info from battle records that do not exist");
    }
    current.remove(battleId);
  }

  /** Adds the specified battles to this list of battles. */
  public void addRecord(final BattleRecords other) {
    for (final PlayerId p : other.records.keySet()) {
      final Map<UUID, BattleRecord> currentRecord = records.get(p);
      if (currentRecord != null) {
        // this only comes up if we use edit mode to create an attack for a player who's already had
        // their turn and
        // therefore already has their record.
        final Map<UUID, BattleRecord> additionalRecords = other.records.get(p);
        for (final Entry<UUID, BattleRecord> entry : additionalRecords.entrySet()) {
          final UUID guid = entry.getKey();
          final BattleRecord br = entry.getValue();
          if (currentRecord.containsKey(guid)) {
            throw new IllegalStateException(
                "Should not be adding battle record for player "
                    + p.getName()
                    + " when they are already on the record. "
                    + "Trying to add: "
                    + br.toString());
          }
          currentRecord.put(guid, br);
        }
        records.put(p, currentRecord);
      } else {
        records.put(p, other.records.get(p));
      }
    }
  }

  /** Removes the specified battles from this list of battles. */
  public void removeRecord(final BattleRecords other) {
    for (final PlayerId p : other.records.keySet()) {
      final Map<UUID, BattleRecord> currentRecord = records.get(p);
      if (currentRecord == null) {
        throw new IllegalStateException(
            "Trying to remove a player records but records do not exist");
      }
      final Map<UUID, BattleRecord> toRemoveRecords = other.records.get(p);
      for (final Entry<UUID, BattleRecord> entry : toRemoveRecords.entrySet()) {
        final UUID guid = entry.getKey();
        if (!currentRecord.containsKey(guid)) {
          throw new IllegalStateException(
              "Trying to remove a battle record but record does not exist");
        }
        currentRecord.remove(guid);
      }
    }
  }

  public void addBattle(
      final PlayerId currentPlayerAndAttacker,
      final UUID battleId,
      final Territory battleSite,
      final BattleType battleType) {
    Map<UUID, BattleRecord> current = records.get(currentPlayerAndAttacker);
    if (current == null) {
      current = new HashMap<>();
    }
    final BattleRecord initial = new BattleRecord(battleSite, currentPlayerAndAttacker, battleType);
    current.put(battleId, initial);
    records.put(currentPlayerAndAttacker, current);
  }

  public void addResultToBattle(
      final PlayerId currentPlayer,
      final UUID battleId,
      final PlayerId defender,
      final int attackerLostTuv,
      final int defenderLostTuv,
      final BattleResultDescription battleResultDescription,
      final BattleResults battleResults) {
    final Map<UUID, BattleRecord> current = records.get(currentPlayer);
    if (current == null) {
      throw new IllegalStateException("Trying to add info to battle records that do not exist");
    }
    if (!current.containsKey(battleId)) {
      throw new IllegalStateException("Trying to add info to a battle that does not exist");
    }
    final BattleRecord record = current.get(battleId);
    record.setResult(
        defender, attackerLostTuv, defenderLostTuv, battleResultDescription, battleResults);
  }

  public void clear() {
    records.clear();
  }

  public boolean isEmpty() {
    return records.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("[");
    for (final Entry<PlayerId, Map<UUID, BattleRecord>> entry : records.entrySet()) {
      sb.append(", ");
      sb.append(entry.getKey().getName());
      sb.append("={");
      final StringBuilder sb2 = new StringBuilder();
      for (final Entry<UUID, BattleRecord> entry2 : entry.getValue().entrySet()) {
        sb2.append(", ");
        final String guid = entry2.getKey().toString();
        sb2.append(
            guid, Math.max(0, Math.min(guid.length(), 7 * guid.length() / 8)), guid.length());
        sb2.append(":");
        sb2.append(entry2.getValue().toString());
      }
      sb.append(sb2.toString().replaceFirst(", ", ""));
      sb.append("}");
    }
    sb.append("]");
    return sb.toString().replaceFirst(", ", "");
  }
}
