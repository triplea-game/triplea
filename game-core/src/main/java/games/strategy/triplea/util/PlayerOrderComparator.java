package games.strategy.triplea.util;

import java.util.Comparator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegate;

public class PlayerOrderComparator implements Comparator<PlayerID> {
  private final GameData gameData;

  public PlayerOrderComparator(final GameData data) {
    gameData = data;
  }

  /**
   * sort based on first step that isn't a bid related step.
   */
  @Override
  public int compare(final PlayerID p1, final PlayerID p2) {
    if (p1.equals(p2)) {
      return 0;
    }
    gameData.acquireReadLock();
    final GameSequence sequence;
    try {
      sequence = gameData.getSequence();
    } finally {
      gameData.releaseReadLock();
    }
    for (final GameStep s : sequence) {
      if (s.getPlayerId() == null) {
        continue;
      }
      gameData.acquireReadLock();
      final IDelegate delegate;
      try {
        delegate = s.getDelegate();
      } finally {
        gameData.releaseReadLock();
      }
      if ((delegate != null) && (delegate.getClass() != null)) {
        final String delegateClassName = delegate.getClass().getName();
        if (delegateClassName.equals("games.strategy.triplea.delegate.InitializationDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPurchaseDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPlaceDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.EndRoundDelegate")) {
          continue;
        }
      } else if ((s.getName() != null) && (s.getName().endsWith("Bid") || s.getName().endsWith("BidPlace"))) {
        continue;
      }
      if (s.getPlayerId().equals(p1)) {
        return -1;
      } else if (s.getPlayerId().equals(p2)) {
        return 1;
      }
    }
    return 0;
  }
}
