package games.strategy.triplea.delegate.battle;

import com.google.gson.JsonObject;
import games.strategy.engine.data.serializer.GameRefResolver;
import games.strategy.engine.data.serializer.SaverSupport;
import games.strategy.engine.data.serializer.TextSaver;
import games.strategy.triplea.delegate.RocketsFireHelper;
import java.io.Serializable;

/**
 * Text serializer for {@code BattleExtendedDelegateState}. The ten phase flags are written
 * natively; the identity-graph members — the {@code BattleTracker}, the optional {@code
 * RocketsFireHelper}, and the {@code IBattle currentBattle} (non-null only during active
 * resolution) — ride through {@link SaverSupport#writeNested} as legacy blobs. Keeping {@code
 * currentBattle} a blob is the v0 save-boundary: a battle mid-resolution is not yet text-encoded.
 */
public final class BattleStateSaver implements TextSaver<BattleExtendedDelegateState> {

  @Override
  public Class<BattleExtendedDelegateState> type() {
    return BattleExtendedDelegateState.class;
  }

  @Override
  public JsonObject write(final BattleExtendedDelegateState s, final GameRefResolver refs) {
    final JsonObject json = new JsonObject();
    json.add("superState", SaverSupport.writeNested(s.superState, refs));
    json.addProperty("needToInitialize", s.needToInitialize);
    json.addProperty("needToScramble", s.needToScramble);
    json.addProperty("needToCreateRockets", s.needToCreateRockets);
    json.addProperty("needToKamikazeSuicideAttacks", s.needToKamikazeSuicideAttacks);
    json.addProperty("needToClearEmptyAirBattleAttacks", s.needToClearEmptyAirBattleAttacks);
    json.addProperty("needToAddBombardmentSources", s.needToAddBombardmentSources);
    json.addProperty("needToFireRockets", s.needToFireRockets);
    json.addProperty("needToRecordBattleStatistics", s.needToRecordBattleStatistics);
    json.addProperty("needToCheckDefendingPlanesCanLand", s.needToCheckDefendingPlanesCanLand);
    json.addProperty("needToCleanup", s.needToCleanup);
    json.add("battleTracker", SaverSupport.writeNested(s.battleTracker, refs));
    json.add("rocketHelper", SaverSupport.writeNested(s.rocketHelper, refs));
    json.add("currentBattle", SaverSupport.writeNested((Serializable) s.currentBattle, refs));
    return json;
  }

  @Override
  public BattleExtendedDelegateState read(final JsonObject json, final GameRefResolver refs) {
    final BattleExtendedDelegateState s = new BattleExtendedDelegateState();
    s.superState = SaverSupport.readNested(json.getAsJsonObject("superState"), refs);
    s.needToInitialize = json.get("needToInitialize").getAsBoolean();
    s.needToScramble = json.get("needToScramble").getAsBoolean();
    s.needToCreateRockets = json.get("needToCreateRockets").getAsBoolean();
    s.needToKamikazeSuicideAttacks = json.get("needToKamikazeSuicideAttacks").getAsBoolean();
    s.needToClearEmptyAirBattleAttacks =
        json.get("needToClearEmptyAirBattleAttacks").getAsBoolean();
    s.needToAddBombardmentSources = json.get("needToAddBombardmentSources").getAsBoolean();
    s.needToFireRockets = json.get("needToFireRockets").getAsBoolean();
    s.needToRecordBattleStatistics = json.get("needToRecordBattleStatistics").getAsBoolean();
    s.needToCheckDefendingPlanesCanLand =
        json.get("needToCheckDefendingPlanesCanLand").getAsBoolean();
    s.needToCleanup = json.get("needToCleanup").getAsBoolean();
    s.battleTracker =
        (BattleTracker) SaverSupport.readNested(json.getAsJsonObject("battleTracker"), refs);
    s.rocketHelper =
        (RocketsFireHelper) SaverSupport.readNested(json.getAsJsonObject("rocketHelper"), refs);
    s.currentBattle =
        (IBattle) SaverSupport.readNested(json.getAsJsonObject("currentBattle"), refs);
    return s;
  }
}
