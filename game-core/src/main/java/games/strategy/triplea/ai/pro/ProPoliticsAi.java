package games.strategy.triplea.ai.pro;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.RelationshipType;
import games.strategy.triplea.ai.AiPoliticalUtils;
import games.strategy.triplea.ai.pro.data.ProTerritory;
import games.strategy.triplea.ai.pro.data.ProTerritoryManager;
import games.strategy.triplea.ai.pro.logging.ProLogger;
import games.strategy.triplea.ai.pro.util.ProOddsCalculator;
import games.strategy.triplea.ai.pro.util.ProUtils;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.delegate.DelegateFinder;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.PoliticsDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.triplea.java.collections.CollectionUtils;

/** Pro politics AI. */
class ProPoliticsAi {

  private final ProOddsCalculator calc;
  private final ProData proData;

  ProPoliticsAi(final AbstractProAi ai) {
    calc = ai.getCalc();
    proData = ai.getProData();
  }

  List<PoliticalActionAttachment> politicalActions() {
    final GameData data = proData.getData();
    final GamePlayer player = proData.getPlayer();
    final float numPlayers = data.getPlayerList().getPlayers().size();
    final double round = data.getSequence().getRound();
    final ProTerritoryManager territoryManager = new ProTerritoryManager(calc, proData);
    final PoliticsDelegate politicsDelegate = DelegateFinder.politicsDelegate(data);
    ProLogger.info("Politics for " + player.getName());

    // Find valid war actions
    final List<PoliticalActionAttachment> actionChoicesTowardsWar =
        AiPoliticalUtils.getPoliticalActionsTowardsWar(
            player, politicsDelegate.getTestedConditions(), data);
    ProLogger.trace("War options: " + actionChoicesTowardsWar);
    final List<PoliticalActionAttachment> validWarActions =
        CollectionUtils.getMatches(
            actionChoicesTowardsWar,
            Matches.abstractUserActionAttachmentCanBeAttempted(
                politicsDelegate.getTestedConditions()));
    ProLogger.trace("Valid War options: " + validWarActions);

    // Divide war actions into enemy and neutral
    final Map<PoliticalActionAttachment, List<GamePlayer>> enemyMap = new HashMap<>();
    final Map<PoliticalActionAttachment, List<GamePlayer>> neutralMap = new HashMap<>();
    for (final PoliticalActionAttachment action : validWarActions) {
      final List<GamePlayer> warPlayers = new ArrayList<>();
      for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
          action.getRelationshipChanges()) {
        final GamePlayer player1 = relationshipChange.player1;
        final GamePlayer player2 = relationshipChange.player2;
        final RelationshipType oldRelation =
            data.getRelationshipTracker().getRelationshipType(player1, player2);
        final RelationshipType newRelation = relationshipChange.relationshipType;
        if (!oldRelation.equals(newRelation)
            && Matches.relationshipTypeIsAtWar().test(newRelation)
            && (player1.equals(player) || player2.equals(player))) {
          GamePlayer warPlayer = player2;
          if (warPlayer.equals(player)) {
            warPlayer = player1;
          }
          warPlayers.add(warPlayer);
        }
      }
      if (!warPlayers.isEmpty()) {
        if (ProUtils.isNeutralPlayer(warPlayers.get(0))) {
          neutralMap.put(action, warPlayers);
        } else {
          enemyMap.put(action, warPlayers);
        }
      }
    }
    ProLogger.debug("Neutral options: " + neutralMap);
    ProLogger.debug("Enemy options: " + enemyMap);
    final List<PoliticalActionAttachment> results = new ArrayList<>();
    if (!enemyMap.isEmpty()) {

      // Find all attack options
      territoryManager.populatePotentialAttackOptions();
      final List<ProTerritory> attackOptions =
          territoryManager.removePotentialTerritoriesThatCantBeConquered();
      ProLogger.trace(
          player.getName()
              + ", numAttackOptions="
              + attackOptions.size()
              + ", options="
              + attackOptions);

      // Find attack options per war action
      final Map<PoliticalActionAttachment, Double> attackPercentageMap = new HashMap<>();
      for (final PoliticalActionAttachment action : enemyMap.keySet()) {
        int count = 0;
        final List<GamePlayer> enemyPlayers = enemyMap.get(action);
        for (final ProTerritory patd : attackOptions) {
          if (Matches.isTerritoryOwnedBy(enemyPlayers).test(patd.getTerritory())
              || Matches.territoryHasUnitsThatMatch(Matches.unitOwnedBy(enemyPlayers))
                  .test(patd.getTerritory())) {
            count++;
          }
        }
        final double attackPercentage = count / (attackOptions.size() + 1.0);
        attackPercentageMap.put(action, attackPercentage);
        ProLogger.trace(
            enemyPlayers + ", count=" + count + ", attackPercentage=" + attackPercentage);
      }

      // Decide whether to declare war on an enemy
      final List<PoliticalActionAttachment> options = new ArrayList<>(attackPercentageMap.keySet());
      Collections.shuffle(options);
      for (final PoliticalActionAttachment action : options) {
        final double roundFactor = (round - 1) * .05; // 0, .05, .1, .15, etc
        final double warChance =
            roundFactor + attackPercentageMap.get(action) * (1 + 10 * roundFactor);
        final double random = Math.random();
        ProLogger.trace(enemyMap.get(action) + ", warChance=" + warChance + ", random=" + random);
        if (random <= warChance) {
          results.add(action);
          ProLogger.debug("---Declared war on " + enemyMap.get(action));
          break;
        }
      }
    } else if (!neutralMap.isEmpty()) {

      // Decide whether to declare war on a neutral
      final List<PoliticalActionAttachment> options = new ArrayList<>(neutralMap.keySet());
      Collections.shuffle(options);
      final double random = Math.random();
      final double warChance = .01;
      ProLogger.debug("warChance=" + warChance + ", random=" + random);
      if (random <= warChance) {
        results.add(options.get(0));
        ProLogger.debug("Declared war on " + enemyMap.get(options.get(0)));
      }
    }

    // Old code used for non-war actions
    if (Math.random() < .5) {
      final List<PoliticalActionAttachment> actionChoicesOther =
          AiPoliticalUtils.getPoliticalActionsOther(
              player, politicsDelegate.getTestedConditions(), data);
      if (!actionChoicesOther.isEmpty()) {
        Collections.shuffle(actionChoicesOther);
        int i = 0;
        final double random = Math.random();
        final int maxOtherActionsPerTurn =
            (random < .3
                ? 0
                : (random < .6 ? 1 : (random < .9 ? 2 : (random < .99 ? 3 : (int) numPlayers))));
        final Iterator<PoliticalActionAttachment> actionOtherIter = actionChoicesOther.iterator();
        while (actionOtherIter.hasNext() && maxOtherActionsPerTurn > 0) {
          final PoliticalActionAttachment action = actionOtherIter.next();
          if (!Matches.abstractUserActionAttachmentCanBeAttempted(
                  politicsDelegate.getTestedConditions())
              .test(action)) {
            continue;
          }
          if (!player.getResources().has(action.getCostResources())) {
            continue;
          }
          i++;
          if (i > maxOtherActionsPerTurn) {
            break;
          }
          results.add(action);
        }
      }
    }
    doActions(results);
    return results;
  }

  void doActions(final List<PoliticalActionAttachment> actions) {
    final GameData data = proData.getData();
    final PoliticsDelegate politicsDelegate = DelegateFinder.politicsDelegate(data);
    for (final PoliticalActionAttachment action : actions) {
      ProLogger.debug("Performing action: " + action);
      politicsDelegate.attemptAction(action);
    }
  }
}
