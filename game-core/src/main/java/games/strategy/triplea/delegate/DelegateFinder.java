package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.delegate.IDelegate;

/**
 * A collection of methods for obtaining various types of delegate instances from the game data.
 */
public class DelegateFinder {
  private static IDelegate findDelegate(final GameData data, final String delegateName) {
    final IDelegate delegate = data.getDelegateList().getDelegate(delegateName);
    if (delegate == null) {
      throw new IllegalStateException(delegateName + " delegate not found");
    }
    return delegate;
  }

  public static PoliticsDelegate politicsDelegate(final GameData data) {
    return (PoliticsDelegate) findDelegate(data, "politics");
  }

  public static BattleDelegate battleDelegate(final GameData data) {
    return (BattleDelegate) findDelegate(data, "battle");
  }

  public static AbstractMoveDelegate moveDelegate(final GameData data) {
    return (AbstractMoveDelegate) findDelegate(data, "move");
  }

  public static TechnologyDelegate techDelegate(final GameData data) {
    return (TechnologyDelegate) findDelegate(data, "tech");
  }
}
