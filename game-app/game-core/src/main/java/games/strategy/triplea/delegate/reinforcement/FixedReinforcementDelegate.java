package games.strategy.triplea.delegate.reinforcement;

import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.BaseTripleADelegate;
import java.io.Serial;
import java.io.Serializable;

/** Explicit scenario phase that delivers fixed reinforcements before movement and combat. */
public final class FixedReinforcementDelegate extends BaseTripleADelegate {
  private FixedReinforcementTracker tracker = new FixedReinforcementTracker();

  @Override
  public void start() {
    super.start();
    FixedReinforcementService.apply(bridge, player, tracker);
  }

  @Override
  public Class<IRemote> getRemoteType() {
    return IRemote.class;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  public FixedReinforcementObservation getObservation() {
    return FixedReinforcementObservationFactory.create(getData(), player, tracker);
  }

  public FixedReinforcementTracker getTracker() {
    return tracker;
  }

  @Override
  public Serializable saveState() {
    final State state = new State();
    state.superState = super.saveState();
    state.tracker = tracker;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final State saved = (State) state;
    super.loadState(saved.superState);
    tracker = saved.tracker == null ? new FixedReinforcementTracker() : saved.tracker;
  }

  private static final class State implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Serializable superState;
    private FixedReinforcementTracker tracker;
  }
}
