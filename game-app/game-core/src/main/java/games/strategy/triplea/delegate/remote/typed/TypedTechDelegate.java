package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.data.TechResults;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import org.triplea.java.collections.IntegerMap;

/**
 * Typed-message-backed {@link ITechDelegate} handed to the AI in place of the reflective
 * current-delegate proxy. The AI's real tech roll forwards over the messenger latch; the
 * server-side lifecycle methods stay unsupported.
 */
public class TypedTechDelegate extends AbstractTypedCurrentDelegate implements ITechDelegate {
  public TypedTechDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public TechResults rollTech(
      final int rollCount,
      final TechnologyFrontier techToRollFor,
      final int newTokens,
      final IntegerMap<GamePlayer> whoPaysHowMuch) {
    return invokeCurrent(
            new RollTechRequest(rollCount, techToRollFor, newTokens, whoPaysHowMuch),
            RollTechResponse.TYPE)
        .getTechResults();
  }
}
