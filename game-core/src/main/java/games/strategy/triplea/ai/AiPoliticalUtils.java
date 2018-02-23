package games.strategy.triplea.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.AbstractEndTurnDelegate;
import games.strategy.triplea.delegate.Matches;
import games.strategy.util.CollectionUtils;

/**
 * Basic utility methods to handle basic AI stuff for Politics this AI always
 * tries to get from Neutral to War with state if it is free with everyone this
 * AI will not go through a different Neutral state to reach a War state. (ie go
 * from NAP to Peace to War)
 */
public class AiPoliticalUtils {
  public static List<PoliticalActionAttachment> getPoliticalActionsTowardsWar(final PlayerID id,
      final HashMap<ICondition, Boolean> testedConditions, final GameData data) {
    final List<PoliticalActionAttachment> acceptableActions = new ArrayList<>();
    for (final PoliticalActionAttachment nextAction : PoliticalActionAttachment.getValidActions(id, testedConditions,
        data)) {
      if (wantToPerFormActionTowardsWar(nextAction, id, data)) {
        acceptableActions.add(nextAction);
      }
    }
    return acceptableActions;
  }

  public static List<PoliticalActionAttachment> getPoliticalActionsOther(final PlayerID id,
      final HashMap<ICondition, Boolean> testedConditions, final GameData data) {
    final List<PoliticalActionAttachment> warActions = getPoliticalActionsTowardsWar(id, testedConditions, data);
    final Collection<PoliticalActionAttachment> validActions =
        PoliticalActionAttachment.getValidActions(id, testedConditions, data);
    validActions.removeAll(warActions);
    final List<PoliticalActionAttachment> acceptableActions = new ArrayList<>();
    for (final PoliticalActionAttachment nextAction : validActions) {
      if (warActions.contains(nextAction)) {
        continue;
      }
      if (goesTowardsWar(nextAction, id, data) && (Math.random() < .5)) {
        continue;
      }
      if (awayFromAlly(nextAction, id, data) && (Math.random() < .9)) {
        continue;
      }
      if (isFree(nextAction)) {
        acceptableActions.add(nextAction);
      } else if (CollectionUtils.countMatches(validActions, Matches.politicalActionHasCostBetween(0, 0)) > 1) {
        if ((Math.random() < .9) && isAcceptableCost(nextAction, id, data)) {
          acceptableActions.add(nextAction);
        }
      } else {
        if ((Math.random() < .4) && isAcceptableCost(nextAction, id, data)) {
          acceptableActions.add(nextAction);
        }
      }
    }
    return acceptableActions;
  }

  private static boolean wantToPerFormActionTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID id,
      final GameData data) {
    return isFree(nextAction) && goesTowardsWar(nextAction, id, data);
  }

  // this code has a rare risk of circular loop actions.. depending on the map
  // designer
  // only switches from a Neutral to an War state... won't go through
  // in-between neutral states
  // TODO have another look at this part.
  private static boolean goesTowardsWar(final PoliticalActionAttachment nextAction, final PlayerID p0,
      final GameData data) {
    for (final String relationshipChangeString : nextAction.getRelationshipChange()) {
      final String[] relationshipChange = relationshipChangeString.split(":");
      final PlayerID p1 = data.getPlayerList().getPlayerId(relationshipChange[0]);
      final PlayerID p2 = data.getPlayerList().getPlayerId(relationshipChange[1]);
      // only continue if p1 or p2 is the AI
      if (p0.equals(p1) || p0.equals(p2)) {
        final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
        final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
        if (currentType.getRelationshipTypeAttachment().isNeutral()
            && newType.getRelationshipTypeAttachment().isWar()) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean awayFromAlly(final PoliticalActionAttachment nextAction, final PlayerID p0,
      final GameData data) {
    for (final String relationshipChangeString : nextAction.getRelationshipChange()) {
      final String[] relationshipChange = relationshipChangeString.split(":");
      final PlayerID p1 = data.getPlayerList().getPlayerId(relationshipChange[0]);
      final PlayerID p2 = data.getPlayerList().getPlayerId(relationshipChange[1]);
      // only continue if p1 or p2 is the AI
      if (p0.equals(p1) || p0.equals(p2)) {
        final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
        final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
        if (currentType.getRelationshipTypeAttachment().isAllied()
            && (newType.getRelationshipTypeAttachment().isNeutral()
                || newType.getRelationshipTypeAttachment().isWar())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isFree(final PoliticalActionAttachment nextAction) {
    return nextAction.getCostPU() <= 0;
  }

  private static boolean isAcceptableCost(final PoliticalActionAttachment nextAction, final PlayerID player,
      final GameData data) {
    // if we have 21 or more PUs and the cost of the action is l0% or less of our total money, then it is an acceptable
    // price.
    final float production = AbstractEndTurnDelegate.getProduction(data.getMap().getTerritoriesOwnedBy(player), data);
    return (production >= 21) && ((nextAction.getCostPU()) <= ((production / 10)));
  }
}
