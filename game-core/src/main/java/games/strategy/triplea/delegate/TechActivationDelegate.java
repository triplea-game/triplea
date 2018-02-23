package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.util.CollectionUtils;

/**
 * Logic for activating tech rolls. This delegate requires the
 * TechnologyDelegate to run correctly.
 */
@MapSupport
public class TechActivationDelegate extends BaseTripleADelegate {
  private boolean needToInitialize = true;

  /** Creates new TechActivationDelegate. */
  public TechActivationDelegate() {}

  /**
   * Called before the delegate will run. In this class, this does all the
   * work.
   */
  @Override
  public void start() {
    super.start();
    final GameData data = getData();
    if (!needToInitialize) {
      return;
    }
    // Activate techs
    final Map<PlayerID, Collection<TechAdvance>> techMap = DelegateFinder.techDelegate(data).getAdvances();
    final Collection<TechAdvance> advances = techMap.get(player);
    if ((advances != null) && (advances.size() > 0)) {
      // Start event
      bridge.getHistoryWriter().startEvent(player.getName() + " activating " + advancesAsString(advances));
      for (final TechAdvance advance : advances) {
        TechTracker.addAdvance(player, bridge, advance);
      }
    }
    // empty
    techMap.put(player, null);
    if (Properties.getTriggers(data)) {
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      final Predicate<TriggerAttachment> techActivationDelegateTriggerMatch = TriggerAttachment.availableUses
          .and(TriggerAttachment.whenOrDefaultMatch(null, null))
          .and(TriggerAttachment.unitPropertyMatch()
              .or(TriggerAttachment.techMatch())
              .or(TriggerAttachment.supportMatch()));
      // get all possible triggers based on this match.
      final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
          new HashSet<>(Collections.singleton(player)), techActivationDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final HashMap<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
            CollectionUtils.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerUnitPropertyChange(toFireTestedAndSatisfied, bridge, null, null, true, true, true,
            true);
        TriggerAttachment.triggerTechChange(toFireTestedAndSatisfied, bridge, null, null, true, true, true, true);
        TriggerAttachment.triggerSupportChange(toFireTestedAndSatisfied, bridge, null, null, true, true, true, true);
      }
    }
    shareTechnology();
    needToInitialize = false;
  }

  @Override
  public void end() {
    super.end();
    needToInitialize = true;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  private void shareTechnology() {
    final PlayerAttachment pa = PlayerAttachment.get(player);
    if (pa == null) {
      return;
    }
    final Collection<PlayerID> shareWith = pa.getShareTechnology();
    if ((shareWith == null) || shareWith.isEmpty()) {
      return;
    }
    final GameData data = getData();
    final Collection<TechAdvance> currentAdvances = TechTracker.getCurrentTechAdvances(player, data);
    for (final PlayerID p : shareWith) {
      final Collection<TechAdvance> availableTechs = TechnologyDelegate.getAvailableTechs(p, data);
      final Collection<TechAdvance> toGive = CollectionUtils.intersection(currentAdvances, availableTechs);
      if (!toGive.isEmpty()) {
        // Start event
        bridge.getHistoryWriter()
            .startEvent(player.getName() + " giving technology to " + p.getName() + ": " + advancesAsString(toGive));
        for (final TechAdvance advance : toGive) {
          TechTracker.addAdvance(p, bridge, advance);
        }
      }
    }
  }

  @Override
  public Serializable saveState() {
    final TechActivationExtendedDelegateState state = new TechActivationExtendedDelegateState();
    state.superState = super.saveState();
    state.m_needToInitialize = needToInitialize;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final TechActivationExtendedDelegateState s = (TechActivationExtendedDelegateState) state;
    super.loadState(s.superState);
    needToInitialize = s.m_needToInitialize;
  }

  // Return string representing all advances in collection
  private static String advancesAsString(final Collection<TechAdvance> advances) {
    int count = advances.size();
    final StringBuilder text = new StringBuilder();
    for (final TechAdvance advance : advances) {
      text.append(advance.getName());
      count--;
      if (count > 1) {
        text.append(", ");
      }
      if (count == 1) {
        text.append(" and ");
      }
    }
    return text.toString();
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return null;
  }
}
