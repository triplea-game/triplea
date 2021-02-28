package games.strategy.engine.data;

import com.google.common.base.Preconditions;
import games.strategy.triplea.delegate.data.BattleRecords;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/** A holder for BattleRecords. */
public class BattleRecordsList extends GameDataComponent {
  private static final long serialVersionUID = 7515693859612849475L;
  private final Map<Integer, BattleRecords> battleRecords = new HashMap<>();

  public BattleRecordsList(final GameData data) {
    super(data);
  }

  public static void addRecords(
      final Map<Integer, BattleRecords> recordList,
      final int currentRound,
      final BattleRecords other) {
    final BattleRecords current = recordList.get(currentRound);
    if (current == null) {
      recordList.put(currentRound, other);
      return;
    }
    current.addRecord(other);
    recordList.put(currentRound, current);
  }

  public static void removeRecords(
      final Map<Integer, BattleRecords> recordList, final int round, final BattleRecords other) {
    final BattleRecords current = recordList.get(round);
    if (current == null) {
      throw new IllegalStateException("Trying to remove records for round that does not exist");
    }
    current.removeRecord(other);
  }

  private BattleRecords getCurrentRoundCopy() {
    final BattleRecords current = battleRecords.get(getData().getSequence().getRound());
    return (current == null) ? new BattleRecords() : new BattleRecords(current);
  }

  public Map<Integer, BattleRecords> getBattleRecordsMap() {
    return battleRecords;
  }

  public Map<Integer, BattleRecords> getBattleRecordsMapCopy() {
    return copyList(battleRecords);
  }

  private static Map<Integer, BattleRecords> copyList(final Map<Integer, BattleRecords> records) {
    final Map<Integer, BattleRecords> copy = new HashMap<>();
    for (final Entry<Integer, BattleRecords> entry : records.entrySet()) {
      copy.put(entry.getKey(), new BattleRecords(entry.getValue()));
    }
    return copy;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("BattleRecordsList:");
    for (final Entry<Integer, BattleRecords> entry : battleRecords.entrySet()) {
      sb.append("\n");
      sb.append(entry.getKey().toString());
      sb.append(" -> ");
      sb.append(entry.getValue().toString());
    }
    return sb.toString();
  }

  // Interpretation stuff below

  /**
   * Returns the TUV damage caused by {@code attacker} from the battle results in {@code
   * battleRecordsList}.
   */
  public static int getTuvDamageCausedByPlayer(
      final GamePlayer attacker,
      final BattleRecordsList battleRecordsList,
      final int beginningRound,
      final int endRound,
      final boolean currentRoundOnly,
      final boolean includeNullPlayer) {
    final Collection<BattleRecords> brs = new ArrayList<>();
    if (currentRoundOnly) {
      if (battleRecordsList != null) {
        brs.add(battleRecordsList.getCurrentRoundCopy());
      }
    } else {
      if (battleRecordsList != null) {
        final Map<Integer, BattleRecords> currentList = battleRecordsList.getBattleRecordsMapCopy();
        if (currentList != null) {
          for (int i = beginningRound; i <= endRound; i++) {
            final BattleRecords currentRecords = currentList.get(i);
            if (currentRecords != null) {
              brs.add(currentRecords);
            }
          }
        }
      }
    }
    int damageCausedByAttacker = 0;
    for (final BattleRecords br : brs) {
      damageCausedByAttacker +=
          BattleRecords.getLostTuvForBattleRecords(
              BattleRecords.getRecordsForPlayerId(attacker, br), false, includeNullPlayer);
    }
    return damageCausedByAttacker;
  }

  /**
   * Determines if there were any battles that match the specified criteria.
   *
   * @param attacker if null then any player
   * @param defender if null then any player
   */
  public static boolean getWereThereBattlesInTerritoriesMatching(
      final GamePlayer attacker,
      final GamePlayer defender,
      final String battleType,
      final Collection<Territory> anyOfTheseTerritories,
      final BattleRecordsList brl,
      final int beginningRound,
      final int endRound,
      final boolean currentRoundOnly) {
    Preconditions.checkNotNull(brl);
    final Collection<BattleRecords> brs = new ArrayList<>();
    if (currentRoundOnly) {
      brs.add(brl.getCurrentRoundCopy());
    } else {
      final Map<Integer, BattleRecords> currentList = brl.getBattleRecordsMapCopy();
      for (int i = beginningRound; i <= endRound; i++) {
        final BattleRecords currentRecords = currentList.get(i);
        if (currentRecords != null) {
          brs.add(currentRecords);
        }
      }
    }
    // null for attacker means any attacker
    for (final BattleRecords br : brs) {
      if (BattleRecords.getWereThereBattlesInTerritoriesMatching(
          (attacker == null
              ? BattleRecords.getAllRecords(br)
              : BattleRecords.getRecordsForPlayerId(attacker, br)),
          attacker,
          defender,
          battleType,
          anyOfTheseTerritories)) {
        return true;
      }
    }
    return false;
  }
}
