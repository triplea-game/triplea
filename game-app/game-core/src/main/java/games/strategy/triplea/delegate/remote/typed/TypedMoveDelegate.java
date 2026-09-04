package games.strategy.triplea.delegate.remote.typed;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.engine.posted.game.pbem.PbemMessagePoster;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Typed-message-backed {@link IMoveDelegate} handed to the AI in place of the reflective
 * current-delegate proxy. The AI's real (non-simulation) moves forward {@code performMove} over the
 * messenger latch; every other method is only reached on the server-side delegate or on the AI's
 * local-simulation move delegate (obtained from the cloned game data), never through this adapter,
 * so they stay unsupported.
 */
public class TypedMoveDelegate extends AbstractTypedCurrentDelegate implements IMoveDelegate {
  public TypedMoveDelegate(final PlayerBridge playerBridge) {
    super(playerBridge);
  }

  @Override
  public Optional<String> performMove(final MoveDescription move) {
    return Optional.ofNullable(
        invokeCurrent(new PerformMoveRequest(move), PerformMoveResponse.TYPE).getError());
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand(final GamePlayer player) {
    throw notForwarded("getTerritoriesWhereAirCantLand");
  }

  @Override
  public Collection<Territory> getTerritoriesWhereAirCantLand() {
    throw notForwarded("getTerritoriesWhereAirCantLand");
  }

  @Override
  public Collection<Territory> getTerritoriesWhereUnitsCantFight() {
    throw notForwarded("getTerritoriesWhereUnitsCantFight");
  }

  @Override
  public List<UndoableMove> getMovesMade() {
    throw notForwarded("getMovesMade");
  }

  @Override
  public String undoMove(final int moveIndex) {
    throw notForwarded("undoMove");
  }

  @Override
  public boolean postTurnSummary(final PbemMessagePoster poster, final String title) {
    throw notForwarded("postTurnSummary");
  }

  @Override
  public void setHasPostedTurnSummary(final boolean hasPostedTurnSummary) {
    throw notForwarded("setHasPostedTurnSummary");
  }

  @Override
  public boolean getHasPostedTurnSummary() {
    throw notForwarded("getHasPostedTurnSummary");
  }
}
