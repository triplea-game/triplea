package games.strategy.triplea.util;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.delegate.IDelegate;
import java.io.Serializable;
import java.util.Comparator;

/** A comparator for {@link GamePlayer} that sorts instances in game play order. */
public class PlayerOrderComparator implements Comparator<GamePlayer>, Serializable {
  private static final long serialVersionUID = -6271054939349383653L;
  private final GameData gameData;

  public PlayerOrderComparator(final GameData data) {
    gameData = data;
  }

  /** sort based on first step that isn't a bid related step. */
  @Override
  public int compare(final GamePlayer p1, final GamePlayer p2) {
    if (p1.equals(p2)) {
      return 0;
    }
    final GameSequence sequence;
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      sequence = gameData.getSequence();
    }
    for (final GameStep s : sequence) {
      if (s.getPlayerId() == null) {
        continue;
      }
      final IDelegate delegate;
      try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
        delegate = s.getDelegate();
      }
      if (delegate != null) {
        final String delegateClassName = delegate.getClass().getName();
        if (delegateClassName.equals("games.strategy.triplea.delegate.InitializationDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPurchaseDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.BidPlaceDelegate")
            || delegateClassName.equals("games.strategy.triplea.delegate.EndRoundDelegate")) {
          continue;
        }
      } else if (s.getName() != null
          && (GameStep.isBidStepName(s.getName()) || GameStep.isBidPlaceStepName(s.getName()))) {
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
