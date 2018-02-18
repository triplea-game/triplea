package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.HashSet;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAi;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.player.ITripleAPlayer;
import games.strategy.triplea.ui.display.ITripleADisplay;

/**
 * Base class designed to make writing custom TripleA delegates simpler.
 * Code common to all TripleA delegates is implemented here.
 */
public abstract class BaseTripleADelegate extends AbstractDelegate {
  private boolean startBaseStepsFinished = false;
  private boolean endBaseStepsFinished = false;

  /**
   * Creates a new instance of the Delegate.
   */
  public BaseTripleADelegate() {
    super();
  }

  /**
   * Called before the delegate will run.
   * All classes should call super.start if they override this.
   * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in
   * the edit delegate.
   */
  @Override
  public void start() {
    super.start();
    if (!startBaseStepsFinished) {
      startBaseStepsFinished = true;
      triggerWhenTriggerAttachments(TriggerAttachment.BEFORE);
    }
  }

  /**
   * Called before the delegate will stop running.
   * All classes should call super.end if they override this.
   * Persistent delegates like Edit Delegate should not extend BaseDelegate, because we do not want to fire triggers in
   * the edit delegate.
   */
  @Override
  public void end() {
    super.end();
    // we are firing triggers, for maps that include them
    if (!endBaseStepsFinished) {
      endBaseStepsFinished = true;
      triggerWhenTriggerAttachments(TriggerAttachment.AFTER);
    }
    // these should probably be somewhere else, but we are relying on the fact that reloading a save go into the start
    // step,
    // but nothing goes into the end step, and therefore there is no way to save then have the end step repeat itself
    startBaseStepsFinished = false;
    endBaseStepsFinished = false;
  }

  @Override
  public Serializable saveState() {
    final BaseDelegateState state = new BaseDelegateState();
    state.m_startBaseStepsFinished = startBaseStepsFinished;
    state.m_endBaseStepsFinished = endBaseStepsFinished;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final BaseDelegateState s = (BaseDelegateState) state;
    startBaseStepsFinished = s.m_startBaseStepsFinished;
    endBaseStepsFinished = s.m_endBaseStepsFinished;
  }

  private void triggerWhenTriggerAttachments(final String beforeOrAfter) {
    final GameData data = getData();
    if (Properties.getTriggers(data)) {
      final String stepName = data.getSequence().getStep().getName();
      // we use AND in order to make sure there are uses and when is set correctly.
      final Predicate<TriggerAttachment> baseDelegateWhenTriggerMatch = TriggerAttachment.availableUses
          .and(TriggerAttachment.whenOrDefaultMatch(beforeOrAfter, stepName));
      TriggerAttachment.collectAndFireTriggers(new HashSet<>(data.getPlayerList().getPlayers()),
          baseDelegateWhenTriggerMatch, bridge, beforeOrAfter, stepName);
    }
    PoliticsDelegate.chainAlliancesTogether(bridge);
  }

  @Override
  protected ITripleADisplay getDisplay() {
    return getDisplay(bridge);
  }

  protected static ITripleADisplay getDisplay(final IDelegateBridge bridge) {
    return (ITripleADisplay) bridge.getDisplayChannelBroadcaster();
  }

  @Override
  protected ITripleAPlayer getRemotePlayer() {
    return getRemotePlayer(bridge);
  }

  protected static ITripleAPlayer getRemotePlayer(final IDelegateBridge bridge) {
    return (ITripleAPlayer) bridge.getRemotePlayer();
  }

  @Override
  protected ITripleAPlayer getRemotePlayer(final PlayerID player) {
    return getRemotePlayer(player, bridge);
  }

  protected static ITripleAPlayer getRemotePlayer(final PlayerID player, final IDelegateBridge bridge) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAi(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
    }
    return (ITripleAPlayer) bridge.getRemotePlayer(player);
  }
}
