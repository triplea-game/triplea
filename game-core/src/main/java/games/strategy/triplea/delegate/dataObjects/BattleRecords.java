package games.strategy.triplea.delegate.dataObjects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.SerializationProxySupport;
import games.strategy.engine.data.Territory;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.delegate.dataObjects.BattleRecord.BattleResultDescription;
import games.strategy.triplea.oddsCalculator.ta.BattleResults;

/**
 * The Purpose of this class is to record various information about combat,
 * in order to use it for conditions and other things later.
 */
public class BattleRecords implements Serializable {
  private static final long serialVersionUID = 1473664374777905497L;

  private final HashMap<PlayerID, HashMap<GUID, BattleRecord>> m_records;


  public BattleRecords() {
    this.m_records = new HashMap<>();
  }

  public BattleRecords(final HashMap<PlayerID, HashMap<GUID, BattleRecord>> records) {
    this.m_records = records;
  }

  @SerializationProxySupport
  public Object writeReplace() {
    return new SerializationProxy(this);
  }

  @SerializationProxySupport
  private static class SerializationProxy implements Serializable {
    private static final long serialVersionUID = 3837818110273155404L;
    private final HashMap<PlayerID, HashMap<GUID, BattleRecord>> records;

    public SerializationProxy(final BattleRecords battleRecords) {
      this.records = battleRecords.m_records;
    }

    private Object readResolve() {
      return new BattleRecords(records);
    }

  }

  // Create copy
  public BattleRecords(final BattleRecords records) {
    m_records = new HashMap<>();
    for (final Entry<PlayerID, HashMap<GUID, BattleRecord>> entry : records.m_records.entrySet()) {
      final PlayerID p = entry.getKey();
      final HashMap<GUID, BattleRecord> record = entry.getValue();
      final HashMap<GUID, BattleRecord> map = new HashMap<>();
      for (final Entry<GUID, BattleRecord> entry2 : record.entrySet()) {
        map.put(entry2.getKey(), new BattleRecord(entry2.getValue()));
      }
      m_records.put(p, map);
    }
  }

  public static Collection<BattleRecord> getAllRecords(final BattleRecords brs) {
    final Collection<BattleRecord> records = new ArrayList<>();
    for (final HashMap<GUID, BattleRecord> playerMap : brs.m_records.values()) {
      records.addAll(playerMap.values());
    }
    return records;
  }

  public static Collection<BattleRecord> getRecordsForPlayerId(final PlayerID player, final BattleRecords brs) {
    final Collection<BattleRecord> playerRecords = new ArrayList<>();
    if (brs.m_records.get(player) == null) {
      return playerRecords;
    }
    for (final Entry<GUID, BattleRecord> entry : brs.m_records.get(player).entrySet()) {
      playerRecords.add(entry.getValue());
    }
    return playerRecords;
  }

  public static int getLostTuvForBattleRecords(final Collection<BattleRecord> brs, final boolean attackerLostTuv,
      final boolean includeNullPlayer) {
    int lostTuv = 0;
    for (final BattleRecord br : brs) {
      if (!includeNullPlayer && ((br.getDefender() == null) || (br.getAttacker() == null) || br.getDefender().isNull()
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

  public static boolean getWereThereBattlesInTerritoriesMatching(final Collection<BattleRecord> brs,
      final PlayerID attacker, final PlayerID defender, final String battleType,
      final Collection<Territory> anyOfTheseTerritories) {
    for (final BattleRecord br : brs) {
      if (anyOfTheseTerritories.contains(br.getBattleSite())) {
        if ((attacker != null) && !attacker.equals(br.getAttacker())) {
          continue;
        }
        if ((defender != null) && !defender.equals(br.getDefender())) {
          continue;
        }
        if (!battleType.equalsIgnoreCase("any")) {
          continue;
        }
        return true;
        // TODO: do more types.... (maybe make a much better enum class that covers both WhoWon and
        // BattleResultDescription in a single enum
        // with multiple variables for each enum to cover the different tiers of detail (ie: won/lost/draw vs
        // conquer/blitz/etc.)
      }
    }
    return false;
  }

  public void removeBattle(final PlayerID currentPlayer, final GUID battleId) {
    final HashMap<GUID, BattleRecord> current = m_records.get(currentPlayer);
    // we can't count on this being the current player. If we created a battle using edit mode, then the battle might be
    // under a different
    // player.
    if ((current == null) || !current.containsKey(battleId)) {
      for (final Entry<PlayerID, HashMap<GUID, BattleRecord>> entry : m_records.entrySet()) {
        if ((entry.getValue() != null) && entry.getValue().containsKey(battleId)) {
          entry.getValue().remove(battleId);
          return;
        }
      }
      throw new IllegalStateException("Trying to remove info from battle records that do not exist");
    }
    current.remove(battleId);
  }

  public void addRecord(final BattleRecords other) {
    for (final PlayerID p : other.m_records.keySet()) {
      final HashMap<GUID, BattleRecord> currentRecord = m_records.get(p);
      if (currentRecord != null) {
        // this only comes up if we use edit mode to create an attack for a player who's already had their turn and
        // therefore already has
        // their record.
        final HashMap<GUID, BattleRecord> additionalRecords = other.m_records.get(p);
        for (final Entry<GUID, BattleRecord> entry : additionalRecords.entrySet()) {
          final GUID guid = entry.getKey();
          final BattleRecord br = entry.getValue();
          if (currentRecord.containsKey(guid)) {
            throw new IllegalStateException("Should not be adding battle record for player " + p.getName()
                + " when they are already on the record. " + "Trying to add: " + br.toString());
          }
          currentRecord.put(guid, br);
        }
        m_records.put(p, currentRecord);
      } else {
        m_records.put(p, other.m_records.get(p));
      }
    }
  }

  public void removeRecord(final BattleRecords other) {
    for (final PlayerID p : other.m_records.keySet()) {
      final HashMap<GUID, BattleRecord> currentRecord = m_records.get(p);
      if (currentRecord == null) {
        throw new IllegalStateException("Trying to remove a player records but records do not exist");
      }
      final HashMap<GUID, BattleRecord> toRemoveRecords = other.m_records.get(p);
      for (final Entry<GUID, BattleRecord> entry : toRemoveRecords.entrySet()) {
        final GUID guid = entry.getKey();
        if (!currentRecord.containsKey(guid)) {
          throw new IllegalStateException("Trying to remove a battle record but record does not exist");
        }
        currentRecord.remove(guid);
      }
    }
  }

  public void addBattle(final PlayerID currentPlayerAndAttacker, final GUID battleId, final Territory battleSite,
      final BattleType battleType) {
    HashMap<GUID, BattleRecord> current = m_records.get(currentPlayerAndAttacker);
    if (current == null) {
      current = new HashMap<>();
    }
    final BattleRecord initial = new BattleRecord(battleSite, currentPlayerAndAttacker, battleType);
    current.put(battleId, initial);
    m_records.put(currentPlayerAndAttacker, current);
  }

  public void addResultToBattle(final PlayerID currentPlayer, final GUID battleId, final PlayerID defender,
      final int attackerLostTuv, final int defenderLostTuv, final BattleResultDescription battleResultDescription,
      final BattleResults battleResults) {
    final HashMap<GUID, BattleRecord> current = m_records.get(currentPlayer);
    if (current == null) {
      throw new IllegalStateException("Trying to add info to battle records that do not exist");
    }
    if (!current.containsKey(battleId)) {
      throw new IllegalStateException("Trying to add info to a battle that does not exist");
    }
    final BattleRecord record = current.get(battleId);
    record.setResult(defender, attackerLostTuv, defenderLostTuv, battleResultDescription, battleResults);
  }

  public void clear() {
    m_records.clear();
  }

  public boolean isEmpty() {
    return m_records.isEmpty();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("[");
    for (final Entry<PlayerID, HashMap<GUID, BattleRecord>> entry : m_records.entrySet()) {
      sb.append(", ");
      sb.append(entry.getKey().getName());
      sb.append("={");
      final StringBuilder sb2 = new StringBuilder("");
      for (final Entry<GUID, BattleRecord> entry2 : entry.getValue().entrySet()) {
        sb2.append(", ");
        final String guid = entry2.getKey().toString();
        sb2.append(guid.substring(Math.max(0, Math.min(guid.length(), (7 * guid.length()) / 8)), guid.length()));
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
