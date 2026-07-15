package games.strategy.triplea.delegate.supply;

import games.strategy.engine.message.IRemote;
import games.strategy.triplea.delegate.BaseTripleADelegate;
import java.io.Serial;
import java.io.Serializable;

/** Explicit owner-turn phase that evaluates road supply before Combat Move. */
public final class SupplyDelegate extends BaseTripleADelegate {
  private SupplyTracker tracker = new SupplyTracker();

  @Override
  public void start() {
    super.start();
    SupplyService.apply(bridge, player, tracker);
  }

  @Override
  public Class<IRemote> getRemoteType() {
    return IRemote.class;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  public SupplyObservation getObservation() {
    return SupplyObservationFactory.create(getData(), player, tracker);
  }

  public SupplyTracker getTracker() {
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
    tracker = saved.tracker == null ? new SupplyTracker() : saved.tracker;
  }

  private static final class State implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Serializable superState;
    private SupplyTracker tracker;
  }
}
