package games.strategy.engine.data.changefactory;

import java.util.Map;

import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;

class AddBattleRecordsChange extends Change {
  private static final long serialVersionUID = -6927678548172402611L;
  private final BattleRecords m_recordsToAdd;
  private final int m_round;

  AddBattleRecordsChange(final BattleRecords battleRecords, final GameData data) {
    m_round = data.getSequence().getRound();
    // make a copy because this is only done once, and only externally from battle
    // tracker, and the source will be cleared (battle tracker clears out the records
    // each turn)
    m_recordsToAdd = new BattleRecords(battleRecords);
  }

  AddBattleRecordsChange(final BattleRecords battleRecords, final int round) {
    m_round = round;
    // do not make a copy, this is only called from RemoveBattleRecordsChange, and we make a copy when we
    // perform, so no need for another copy.
    m_recordsToAdd = battleRecords;
  }

  @Override
  protected void perform(final GameData data) {
    final Map<Integer, BattleRecords> currentRecords = data.getBattleRecordsList().getBattleRecordsMap();
    // make a copy because otherwise ours will be
    // cleared when we RemoveBattleRecordsChange
    BattleRecordsList.addRecords(currentRecords, m_round, new BattleRecords(m_recordsToAdd));
  }

  @Override
  public Change invert() {
    return new RemoveBattleRecordsChange(m_recordsToAdd, m_round);
  }

  @Override
  public String toString() {
    // This only occurs when serialization went badly, or something cannot be serialized.
    if (m_recordsToAdd == null) {
      throw new IllegalStateException(
          "Records cannot be null (most likely caused by improper or impossible serialization): " + m_recordsToAdd);
    }
    return "Adding Battle Records: " + m_recordsToAdd;
  }
}
