package games.strategy.triplea.ai.Dynamix_AI.CommandCenter;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.DefaultDelegateBridge;
import games.strategy.triplea.delegate.BattleTracker;

/**
 * Note that this class will most likely be removed when the AI is completed...
 */
public class CachedInstanceCenter {
  public static GameData CachedGameData = null;
  public static DefaultDelegateBridge CachedDelegateBridge = null;
  public static BattleTracker CachedBattleTracker = null;

  public static void clearCachedDelegatesAndData() {
    CachedInstanceCenter.CachedBattleTracker = null;
    CachedInstanceCenter.CachedGameData = null;
    CachedInstanceCenter.CachedDelegateBridge = null;
  }
}
