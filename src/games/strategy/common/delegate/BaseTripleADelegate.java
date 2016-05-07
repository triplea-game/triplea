package games.strategy.common.delegate;

import java.util.HashSet;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.triplea.TripleA;
import games.strategy.triplea.ai.weakAI.WeakAI;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.delegate.PoliticsDelegate;
import games.strategy.triplea.player.ITripleaPlayer;
import games.strategy.triplea.ui.display.ITripleaDisplay;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;

/**
 * Base class designed to make writing custom TripleA delegates simpler.
 * Code common to all TripleA delegates is implemented here.
 */
public abstract class BaseTripleADelegate extends BaseDelegate {

  /**
   * Creates a new instance of the Delegate
   */
  public BaseTripleADelegate() {
    super();
  }

  @Override
  protected void triggerOnStart() {
    triggerWhenTriggerAttachments(AbstractTriggerAttachment.BEFORE);
  }

  @Override
  protected void triggerOnEnd() {
    triggerWhenTriggerAttachments(AbstractTriggerAttachment.AFTER);
  }

  private void triggerWhenTriggerAttachments(final String beforeOrAfter) {
    final GameData data = getData();
    if (games.strategy.triplea.Properties.getTriggers(data)) {
      final String stepName = data.getSequence().getStep().getName();
      // we use AND in order to make sure there are uses and when is set correctly.
      final Match<TriggerAttachment> baseDelegateWhenTriggerMatch = new CompositeMatchAnd<TriggerAttachment>(
          TriggerAttachment.availableUses, TriggerAttachment.whenOrDefaultMatch(beforeOrAfter, stepName));
      TriggerAttachment.collectAndFireTriggers(new HashSet<PlayerID>(data.getPlayerList().getPlayers()),
          baseDelegateWhenTriggerMatch, m_bridge, beforeOrAfter, stepName);
    }
    PoliticsDelegate.chainAlliancesTogether(m_bridge);
  }

  @Override
  protected ITripleaDisplay getDisplay() {
    return (ITripleaDisplay) getDisplay(m_bridge);
  }

  @Override
  protected ITripleaPlayer getRemotePlayer() {
    return (ITripleaPlayer) getRemotePlayer(m_bridge);
  }

  @Override
  protected ITripleaPlayer getRemotePlayer(final PlayerID player) {
    // if its the null player, return a do nothing proxy
    if (player.isNull()) {
      return new WeakAI(player.getName(), TripleA.WEAK_COMPUTER_PLAYER_TYPE);
    }
    return (ITripleaPlayer) m_bridge.getRemotePlayer(player);
  }

}
