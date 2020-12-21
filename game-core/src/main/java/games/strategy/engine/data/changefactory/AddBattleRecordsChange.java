package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.BattleRecordsList;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameDataInjections;
import games.strategy.triplea.delegate.data.BattleRecords;
import java.util.Map;

class AddBattleRecordsChange extends Change {
  private static final long serialVersionUID = -6927678548172402611L;

  private final BattleRecords recordsToAdd;
  private final int round;

  AddBattleRecordsChange(final BattleRecords battleRecords, final GameDataInjections data) {
    round = data.getSequence().getRound();
    // make a copy because this is only done once, and only externally from battle
    // tracker, and the source will be cleared (battle tracker clears out the records each turn)
    recordsToAdd = new BattleRecords(battleRecords);
  }

  AddBattleRecordsChange(final BattleRecords battleRecords, final int round) {
    this.round = round;
    // do not make a copy, this is only called from RemoveBattleRecordsChange, and we make a copy
    // when we
    // perform, so no need for another copy.
    recordsToAdd = battleRecords;
  }

  @Override
  protected void perform(final GameDataInjections data) {
    final Map<Integer, BattleRecords> currentRecords =
        data.getBattleRecordsList().getBattleRecordsMap();
    // make a copy because otherwise ours will be cleared when we RemoveBattleRecordsChange
    BattleRecordsList.addRecords(currentRecords, round, new BattleRecords(recordsToAdd));
  }

  @Override
  public Change invert() {
    return new RemoveBattleRecordsChange(recordsToAdd, round);
  }

  @Override
  public String toString() {
    // This only occurs when serialization went badly, or something cannot be serialized.
    if (recordsToAdd == null) {
      throw new IllegalStateException(
          "Records cannot be null (most likely caused by improper or impossible serialization)");
    }
    return "Adding Battle Records: " + recordsToAdd;
  }
}
