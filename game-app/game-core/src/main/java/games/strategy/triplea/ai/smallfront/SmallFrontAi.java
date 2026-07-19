package games.strategy.triplea.ai.smallfront;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.delegate.strategic.simulation.StrategicAction;
import games.strategy.triplea.delegate.strategic.simulation.StrategicActionResolver;
import games.strategy.triplea.delegate.strategic.simulation.StrategicActionSpaceOverflow;
import games.strategy.triplea.delegate.strategic.simulation.StrategicMoveCandidateGenerator;
import games.strategy.triplea.delegate.strategic.simulation.StrategicPhase;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Plays a Small Front map by asking a {@link SmallFrontPolicy} to pick from the same legal action
 * mask the reinforcement-learning environment uses.
 *
 * <p>The Pro AI cannot play these maps: they have no production frontier, so TuvCostsCalculator
 * returns an empty cost map, every unit is worth 0, no attack ever looks profitable and the AI
 * never moves. Rather than teach the Pro AI about an economy that deliberately does not exist, this
 * drives the move generator the maps were designed around.
 *
 * <p>There is no purchase, tech or placement on these maps, so those hooks do nothing.
 */
@Slf4j
public class SmallFrontAi extends AbstractAi {
  private static final int MAX_ACTIONS = 4096;

  /** A turn cannot need more decisions than this; the bound just stops a policy looping forever. */
  private static final int MAX_MOVES_PER_PHASE = 200;

  private final SmallFrontPolicy policy;

  public SmallFrontAi(final String name, final String playerLabel) {
    this(name, playerLabel, new PlanAwareOperationalPolicy());
  }

  public SmallFrontAi(final String name, final String playerLabel, final SmallFrontPolicy policy) {
    super(name, playerLabel);
    this.policy = Objects.requireNonNull(policy);
  }

  @Override
  protected void move(
      final boolean nonCombat,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    if (nonCombat) {
      movePhase(StrategicPhase.REDEPLOYMENT, moveDel, data, player);
      return;
    }
    movePhase(StrategicPhase.COMBAT_MOVE, moveDel, data, player);
    movePhase(StrategicPhase.AIR_ASSIGNMENT, moveDel, data, player);
  }

  private void movePhase(
      final StrategicPhase phase,
      final IMoveDelegate moveDel,
      final GameData data,
      final GamePlayer player) {
    final List<StrategicAction> completedActions = new ArrayList<>();
    for (int i = 0; i < MAX_MOVES_PER_PHASE; i++) {
      final List<StrategicAction> actions = legalActions(data, player, phase, moveDel);
      if (actions.isEmpty()) {
        return;
      }
      final Optional<StrategicAction> chosen =
          policy.choose(actions, data, player, List.copyOf(completedActions));
      if (chosen.isEmpty() || "end_phase".equals(chosen.get().type())) {
        return;
      }
      if (!execute(chosen.get(), moveDel, data)) {
        return;
      }
      completedActions.add(chosen.get());
      policy.onActionCompleted(chosen.get(), data, player);
    }
  }

  private List<StrategicAction> legalActions(
      final GameData data,
      final GamePlayer player,
      final StrategicPhase phase,
      final IMoveDelegate moveDel) {
    try {
      // MoveValidator reads the moves already made, so pass them or the generator answers more
      // permissively than the delegate will and offers moves it then rejects.
      return StrategicMoveCandidateGenerator.generate(
          data, player, phase, MAX_ACTIONS, moveDel.getMovesMade());
    } catch (final StrategicActionSpaceOverflow e) {
      log.warn("Small Front AI: too many candidate moves to consider, standing pat", e);
      return List.of();
    }
  }

  /** Returns false when the move did not happen, which ends the phase rather than spin. */
  private boolean execute(
      final StrategicAction action, final IMoveDelegate moveDel, final GameData data) {
    final Route route;
    final List<Unit> units;
    try {
      route = StrategicActionResolver.resolveRoute(data, action.parameters().get("route"));
      units =
          StrategicActionResolver.resolveUnits(
              action.parameters().get("unitIds"), route.getStart());
    } catch (final RuntimeException e) {
      log.warn("Small Front AI: could not decode its own action {}", action, e);
      return false;
    }
    final Optional<String> error = moveDel.performMove(new MoveDescription(units, route));
    if (error.isPresent()) {
      // A move into fog can legitimately bounce off something the generator could not see.
      log.debug("Small Front AI: move rejected: {}", error.get());
      return false;
    }
    return true;
  }

  @Override
  protected void purchase(
      final boolean purchaseForBid,
      final int pusToSpend,
      final IPurchaseDelegate purchaseDelegate,
      final GameData data,
      final GamePlayer player) {}

  @Override
  protected void tech(
      final ITechDelegate techDelegate, final GameData data, final GamePlayer player) {}

  @Override
  protected void place(
      final boolean placeForBid,
      final IAbstractPlaceDelegate placeDelegate,
      final GameState data,
      final GamePlayer player) {}
}
