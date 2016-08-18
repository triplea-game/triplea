package games.strategy.engine.data.changefactory;

import java.util.Map;

import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.triplea.delegate.dataObjects.BattleRecords;

class RemoveBattleRecordsChange extends Change {
  private static final long serialVersionUID = 3286634991233029854L;
  private final BattleRecords m_recordsToRemove;
  private final int m_round;

  RemoveBattleRecordsChange(final BattleRecords battleRecords, final int round) {
    m_round = round;
    // do not make a copy, this is only called from AddBattleRecordsChange, and we make a copy when we
    // perform, so no need for another copy.
    m_recordsToRemove = battleRecords;
  }

  @Override
  protected void perform(final GameData data) {
    final Map<Integer, BattleRecords> currentRecords = data.getBattleRecordsList().getBattleRecordsMap();
    // make a copy else we will get a concurrent modification error
    BattleRecordsList.removeRecords(currentRecords, m_round, new BattleRecords(m_recordsToRemove));
  }

  @Override
  public Change invert() {
    return new AddBattleRecordsChange(m_recordsToRemove, m_round);
  }

  @Override
  public String toString() {
    // This only occurs when serialization went badly, or something cannot be serialized.
    if (m_recordsToRemove == null) {
      throw new IllegalStateException(
          "Records cannot be null (most likely caused by improper or impossible serialization): " + m_recordsToRemove);
    }
    return "Adding Battle Records: " + m_recordsToRemove;
  }
}
