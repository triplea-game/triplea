package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.FireTriggerParams;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.PoliticsText;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/** Responsible allowing players to perform politicalActions. */
public class PoliticsDelegate extends BaseTripleADelegate implements IPoliticsDelegate {
  @Override
  public void end() {
    super.end();
    resetAttempts();
    if (Properties.getTriggers(getData().getProperties())) {
      // First set up a match for what we want to have fire as a default in this delegate. List out
      // as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT
      // have 'when' set.
      final Predicate<TriggerAttachment> politicsDelegateTriggerMatch =
          TriggerAttachment.availableUses
              .and(TriggerAttachment.whenOrDefaultMatch(null, null))
              .and(TriggerAttachment.relationshipChangeMatch());
      // get all possible triggers based on this match.
      final Set<TriggerAttachment> toFirePossible =
          TriggerAttachment.collectForAllTriggersMatching(
              Set.of(player), politicsDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final Map<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final Set<TriggerAttachment> toFireTestedAndSatisfied =
            new HashSet<>(
                CollectionUtils.getMatches(
                    toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerRelationshipChange(
            toFireTestedAndSatisfied,
            bridge,
            new FireTriggerParams(null, null, true, true, true, true));
      }
    }
    chainAlliancesTogether(bridge);
    givesBackOriginalTerritories(bridge);
  }

  @Override
  public Serializable saveState() {
    final PoliticsExtendedDelegateState state = new PoliticsExtendedDelegateState();
    state.superState = super.saveState();
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final PoliticsExtendedDelegateState s = (PoliticsExtendedDelegateState) state;
    super.loadState(s.superState);
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    if (!player.amNotDeadYet()) {
      return false;
    }
    return Properties.getUsePolitics(getData().getProperties()) && !getValidActions().isEmpty();
  }

  public Map<ICondition, Boolean> getTestedConditions() {
    final Set<ICondition> allConditionsNeeded =
        RulesAttachment.getAllConditionsRecursive(
            new HashSet<>(PoliticalActionAttachment.getPoliticalActionAttachments(player)), null);
    return RulesAttachment.testAllConditionsRecursive(allConditionsNeeded, null, bridge);
  }

  @Override
  public Collection<PoliticalActionAttachment> getValidActions() {
    final GameData data = bridge.getData();
    final Map<ICondition, Boolean> testedConditions;
    try (GameData.Unlocker ignored = data.acquireReadLock()) {
      testedConditions = getTestedConditions();
    }
    return PoliticalActionAttachment.getValidActions(player, testedConditions, data);
  }

  @Override
  public Class<IPoliticsDelegate> getRemoteType() {
    return IPoliticsDelegate.class;
  }

  @Override
  public void attemptAction(final PoliticalActionAttachment paa) {
    if (!Properties.getUsePolitics(getData().getProperties())) {
      notifyPoliticsTurnedOff();
      return;
    }
    if (paa.canPerform(getTestedConditions())) {
      if (checkEnoughMoney(paa)) { // See if the player has got enough money to pay for the action
        // Charge for attempting the action
        chargeForAction(paa);
        // take one of the uses this round
        paa.useAttempt(getBridge());
        if (actionRollSucceeds(paa)) { // See if the action is successful
          if (actionIsAccepted(paa)) {
            // change the relationships
            changeRelationships(paa);
            // notify the players
            notifySuccess(paa);
          } else {
            // notify the players of the failed attempt
            notifyFailure(paa);
          }
        } else {
          // notify the players of the failed attempt
          notifyFailure(paa);
        }
      } else {
        // notify the player he hasn't got enough money;
        notifyMoney(paa);
      }
    } else {
      // notify the player the action isn't valid anymore (shouldn't happen)
      notifyNoValidAction();
    }
  }

  /**
   * Get a list of players that should accept this action and then ask each player if it accepts
   * this action.
   *
   * @param paa the politicalActionAttachment that should be accepted
   */
  private boolean actionIsAccepted(final PoliticalActionAttachment paa) {
    final GameState data = getData();
    final Predicate<PoliticalActionAttachment> intoAlliedChainOrIntoOrOutOfWar =
        Matches.politicalActionIsRelationshipChangeOf(
                null,
                Matches.relationshipTypeIsAlliedAndAlliancesCanChainTogether().negate(),
                Matches.relationshipTypeIsAlliedAndAlliancesCanChainTogether(),
                data.getRelationshipTracker())
            .or(
                Matches.politicalActionIsRelationshipChangeOf(
                    null,
                    Matches.relationshipTypeIsAtWar().negate(),
                    Matches.relationshipTypeIsAtWar(),
                    data.getRelationshipTracker()))
            .or(
                Matches.politicalActionIsRelationshipChangeOf(
                    null,
                    Matches.relationshipTypeIsAtWar(),
                    Matches.relationshipTypeIsAtWar().negate(),
                    data.getRelationshipTracker()));
    final String acceptanceQuestion =
        bridge
            .getResourceLoader()
            .map(PoliticsText::new)
            .map(politicsText -> politicsText.getAcceptanceQuestion(paa.getText()))
            // String is ignored if getResourceLoader() returns empty Optional.
            .orElse("");
    if (!Properties.getAlliancesCanChainTogether(data.getProperties())
        || !intoAlliedChainOrIntoOrOutOfWar.test(paa)) {
      for (final GamePlayer player : paa.getActionAccept()) {
        if (!getRemotePlayer(player).acceptAction(this.player, acceptanceQuestion, true)) {
          return false;
        }
      }
    } else {
      // if alliances chain together, then our allies must have a say in anyone becoming a new
      // ally/enemy
      final LinkedHashSet<GamePlayer> playersWhoNeedToAccept = new LinkedHashSet<>();
      playersWhoNeedToAccept.addAll(paa.getActionAccept());
      playersWhoNeedToAccept.addAll(
          CollectionUtils.getMatches(
              data.getPlayerList().getPlayers(),
              Matches.isAlliedAndAlliancesCanChainTogether(player)));
      for (final GamePlayer player : paa.getActionAccept()) {
        playersWhoNeedToAccept.addAll(
            CollectionUtils.getMatches(
                data.getPlayerList().getPlayers(),
                Matches.isAlliedAndAlliancesCanChainTogether(player)));
      }
      playersWhoNeedToAccept.removeAll(paa.getActionAccept());
      for (final GamePlayer player : playersWhoNeedToAccept) {
        final String actionText;
        if (acceptanceQuestion.equals("NONE")) {
          actionText =
              this.player.getName()
                  + " wants to take the following action: "
                  + MyFormatter.attachmentNameToText(paa.getName())
                  + " \r\n Do you approve?";
        } else {
          actionText =
              this.player.getName()
                  + " wants to take the following action: "
                  + MyFormatter.attachmentNameToText(paa.getName())
                  + ".  Do you approve? \r\n\r\n "
                  + this.player.getName()
                  + " will ask "
                  + MyFormatter.defaultNamedToTextList(paa.getActionAccept())
                  + ", the following question: \r\n "
                  + acceptanceQuestion;
        }
        if (!getRemotePlayer(player).acceptAction(this.player, actionText, true)) {
          return false;
        }
      }
      for (final GamePlayer player : paa.getActionAccept()) {
        if (!getRemotePlayer(player).acceptAction(this.player, acceptanceQuestion, true)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Let the player know this action isn't valid anymore, this shouldn't happen as the player
   * shouldn't get an option to push the button on non-valid actions.
   */
  private void notifyNoValidAction() {
    sendNotification("This action isn't available anymore (this shouldn't happen!?!)");
  }

  private void notifyPoliticsTurnedOff() {
    sendNotification("Politics is turned off in the game options");
  }

  /**
   * Let the player know he is being charged for money or that he hasn't got enough money.
   *
   * @param paa the actionattachment the player is notified about
   */
  private void notifyMoney(final PoliticalActionAttachment paa) {
    final String cost = ResourceCollection.toString(paa.getCostResources(), getData());
    sendNotification("You don't have enough money, you need " + cost + " to perform this action");
  }

  /**
   * Subtract money from the players wallet.
   *
   * @param paa the politicalactionattachment this the money is charged for.
   */
  private void chargeForAction(final PoliticalActionAttachment paa) {
    final IntegerMap<Resource> cost = paa.getCostResources();
    if (!cost.isEmpty()) {
      final String transcriptText =
          bridge.getGamePlayer().getName()
              + " spend "
              + ResourceCollection.toString(cost, getData())
              + " on Political Action: "
              + MyFormatter.attachmentNameToText(paa.getName());
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change charge =
          ChangeFactory.removeResourceCollection(
              bridge.getGamePlayer(), new ResourceCollection(getData(), cost));
      bridge.addChange(charge);
    } else {
      final String transcriptText =
          bridge.getGamePlayer().getName()
              + " takes Political Action: "
              + MyFormatter.attachmentNameToText(paa.getName());
      // we must start an event anyway
      bridge.getHistoryWriter().startEvent(transcriptText);
    }
  }

  private boolean checkEnoughMoney(final PoliticalActionAttachment paa) {
    return bridge.getGamePlayer().getResources().has(paa.getCostResources());
  }

  /**
   * Let all players involved in this action know the action has failed.
   *
   * @param paa the political action attachment that just failed.
   */
  private void notifyFailure(final PoliticalActionAttachment paa) {
    bridge
        .getSoundChannelBroadcaster()
        .playSoundForAll(SoundPath.CLIP_POLITICAL_ACTION_FAILURE, player);
    final String transcriptText =
        bridge.getGamePlayer().getName()
            + " fails on action: "
            + MyFormatter.attachmentNameToText(paa.getName());
    bridge.getHistoryWriter().addChildToEvent(transcriptText);
    bridge
        .getResourceLoader()
        .ifPresent(
            resourceLoader -> {
              PoliticsText politicsText = new PoliticsText(resourceLoader);
              sendNotification(politicsText.getNotificationFailure(paa.getText()));
              notifyOtherPlayers(politicsText.getNotificationFailureOthers(paa.getText()));
            });
  }

  /**
   * Let all players involved in this action know the action was successful.
   *
   * @param paa the political action attachment that just succeeded.
   */
  private void notifySuccess(final PoliticalActionAttachment paa) {
    bridge
        .getSoundChannelBroadcaster()
        .playSoundForAll(SoundPath.CLIP_POLITICAL_ACTION_SUCCESSFUL, player);
    bridge
        .getResourceLoader()
        .ifPresent(
            resourceLoader -> {
              PoliticsText politicsText = new PoliticsText(resourceLoader);
              sendNotification(politicsText.getNotificationSuccess(paa.getText()));
              notifyOtherPlayers(politicsText.getNotificationSuccessOthers(paa.getText()));
            });
  }

  /**
   * Send a notification to the other players involved in this action (all players except the player
   * starting the action).
   */
  private void notifyOtherPlayers(final String notification) {
    if (!"NONE".equals(notification)) {
      // we can send it to just paa.getOtherPlayers(), or we can send it to all players. both are
      // good options.
      final Collection<GamePlayer> currentPlayer = new ArrayList<>();
      currentPlayer.add(player);
      final Collection<GamePlayer> otherPlayers = getData().getPlayerList().getPlayers();
      otherPlayers.removeAll(currentPlayer);
      bridge
          .getDisplayChannelBroadcaster()
          .reportMessageToPlayers(otherPlayers, currentPlayer, notification, notification);
    }
  }

  /**
   * Send a notification to the current player.
   *
   * @param text if NONE don't send a notification
   */
  private void sendNotification(final String text) {
    if (!"NONE".equals(text)) {
      bridge.getRemotePlayer().reportMessage(text, text);
    }
  }

  /**
   * Changes all relationships.
   *
   * @param paa the political action to change the relationships for
   */
  private void changeRelationships(final PoliticalActionAttachment paa) {
    getMyselfOutOfAlliance(paa, player, bridge);
    getNeutralOutOfWarWithAllies(paa, player, bridge);
    final CompositeChange change = new CompositeChange();
    for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
        paa.getRelationshipChanges()) {
      final GamePlayer player1 = relationshipChange.player1;
      final GamePlayer player2 = relationshipChange.player2;
      final RelationshipType oldRelation =
          getData().getRelationshipTracker().getRelationshipType(player1, player2);
      final RelationshipType newRelation = relationshipChange.relationshipType;
      if (oldRelation.equals(newRelation)) {
        continue;
      }
      change.add(ChangeFactory.relationshipChange(player1, player2, oldRelation, newRelation));
      bridge
          .getHistoryWriter()
          .addChildToEvent(
              bridge.getGamePlayer().getName()
                  + " succeeds on action: "
                  + MyFormatter.attachmentNameToText(paa.getName())
                  + ": Changing Relationship for "
                  + player1.getName()
                  + " and "
                  + player2.getName()
                  + " from "
                  + oldRelation.getName()
                  + " to "
                  + newRelation.getName());
      MoveDelegate.getBattleTracker(getData())
          .addRelationshipChangesThisTurn(player1, player2, oldRelation, newRelation);
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
    chainAlliancesTogether(bridge);
  }

  /**
   * Executes the specified action.
   *
   * @param paa the action to check if it succeeds
   * @return true if the action succeeds, usually because the die-roll succeeded.
   */
  private boolean actionRollSucceeds(final PoliticalActionAttachment paa) {
    final int hitTarget = paa.getChanceToHit();
    final int diceSides = paa.getChanceDiceSides();
    if (diceSides <= 0 || hitTarget >= diceSides) {
      paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, true, true);
      return true;
    } else if (hitTarget <= 0) {
      paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, false, true);
      return false;
    }
    final int rollResult =
        bridge.getRandom(
                diceSides,
                player,
                DiceType.NONCOMBAT,
                "Attempting the Political Action: "
                    + MyFormatter.attachmentNameToText(paa.getName()))
            + 1;
    final boolean success = rollResult <= hitTarget;
    final String notificationMessage =
        "rolling ("
            + hitTarget
            + " out of "
            + diceSides
            + ") result: "
            + rollResult
            + " = "
            + (success ? "Success!" : "Failure!");
    bridge
        .getHistoryWriter()
        .addChildToEvent(
            MyFormatter.attachmentNameToText(paa.getName()) + " : " + notificationMessage);
    paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, success, true);
    sendNotification(notificationMessage);
    return success;
  }

  /**
   * Reset the attempts-counter for this action, so next round the player can try again for a number
   * of attempts.
   */
  private void resetAttempts() {
    for (final PoliticalActionAttachment paa :
        PoliticalActionAttachment.getPoliticalActionAttachments(player)) {
      paa.resetAttempts(getBridge());
    }
  }

  private static void getMyselfOutOfAlliance(
      final PoliticalActionAttachment paa, final GamePlayer player, final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    if (!Properties.getAlliancesCanChainTogether(data.getProperties())) {
      return;
    }
    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();
    final Collection<GamePlayer> p1AlliedWith =
        CollectionUtils.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player));
    p1AlliedWith.remove(player);
    final CompositeChange change = new CompositeChange();
    for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
        paa.getRelationshipChanges()) {
      final GamePlayer p1 = relationshipChange.player1;
      final GamePlayer p2 = relationshipChange.player2;
      if (!(p1.equals(player) || p2.equals(player))) {
        continue;
      }
      final GamePlayer otherPlayer = (p1.equals(player) ? p2 : p1);
      if (!p1AlliedWith.contains(otherPlayer)) {
        continue;
      }
      final RelationshipType currentType =
          data.getRelationshipTracker().getRelationshipType(p1, p2);
      final RelationshipType newType = relationshipChange.relationshipType;
      if (Matches.relationshipTypeIsAlliedAndAlliancesCanChainTogether().test(currentType)
          && Matches.relationshipTypeIsAlliedAndAlliancesCanChainTogether()
              .negate()
              .test(newType)) {
        for (final GamePlayer p3 : p1AlliedWith) {
          final RelationshipType currentOther =
              data.getRelationshipTracker().getRelationshipType(p3, player);
          if (!currentOther.equals(newType)) {
            change.add(ChangeFactory.relationshipChange(p3, player, currentOther, newType));
            bridge
                .getHistoryWriter()
                .addChildToEvent(
                    player.getName()
                        + " and "
                        + p3.getName()
                        + " sign a "
                        + newType.getName()
                        + " treaty");
            MoveDelegate.getBattleTracker(data)
                .addRelationshipChangesThisTurn(p3, player, currentOther, newType);
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  private static void getNeutralOutOfWarWithAllies(
      final PoliticalActionAttachment paa, final GamePlayer player, final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    if (!Properties.getAlliancesCanChainTogether(data.getProperties())) {
      return;
    }

    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();
    final Collection<GamePlayer> p1AlliedWith =
        CollectionUtils.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player));
    final CompositeChange change = new CompositeChange();
    for (final PoliticalActionAttachment.RelationshipChange relationshipChange :
        paa.getRelationshipChanges()) {
      final GamePlayer p1 = relationshipChange.player1;
      final GamePlayer p2 = relationshipChange.player2;
      if (!(p1.equals(player) || p2.equals(player))) {
        continue;
      }
      final GamePlayer otherPlayer = (p1.equals(player) ? p2 : p1);
      final RelationshipType currentType =
          data.getRelationshipTracker().getRelationshipType(p1, p2);
      final RelationshipType newType = relationshipChange.relationshipType;
      if (Matches.relationshipTypeIsAtWar().test(currentType)
          && Matches.relationshipTypeIsAtWar().negate().test(newType)) {
        final Collection<GamePlayer> otherPlayersAlliedWith =
            CollectionUtils.getMatches(
                players, Matches.isAlliedAndAlliancesCanChainTogether(otherPlayer));
        if (!otherPlayersAlliedWith.contains(otherPlayer)) {
          otherPlayersAlliedWith.add(otherPlayer);
        }
        if (!p1AlliedWith.contains(player)) {
          p1AlliedWith.add(player);
        }
        for (final GamePlayer p3 : p1AlliedWith) {
          for (final GamePlayer p4 : otherPlayersAlliedWith) {
            final RelationshipType currentOther =
                data.getRelationshipTracker().getRelationshipType(p3, p4);
            if (!currentOther.equals(newType)
                && Matches.relationshipTypeIsAtWar().test(currentOther)) {
              change.add(ChangeFactory.relationshipChange(p3, p4, currentOther, newType));
              bridge
                  .getHistoryWriter()
                  .addChildToEvent(
                      p3.getName()
                          + " and "
                          + p4.getName()
                          + " sign a "
                          + newType.getName()
                          + " treaty");
              MoveDelegate.getBattleTracker(data)
                  .addRelationshipChangesThisTurn(p3, p4, currentOther, newType);
            }
          }
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.addChange(change);
    }
  }

  static void chainAlliancesTogether(final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    if (!Properties.getAlliancesCanChainTogether(data.getProperties())) {
      return;
    }
    final Collection<RelationshipType> allTypes =
        data.getRelationshipTypeList().getAllRelationshipTypes();
    RelationshipType alliedType = null;
    RelationshipType warType = null;
    for (final RelationshipType type : allTypes) {
      if (type.getRelationshipTypeAttachment().isDefaultWarPosition()) {
        warType = type;
      } else if (type.getRelationshipTypeAttachment().canAlliancesChainTogether()) {
        alliedType = type;
      }
    }
    if (alliedType == null) {
      return;
    }
    // first do alliances. then, do war (since we don't want to declare war on a potential ally).
    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();
    for (final GamePlayer p1 : players) {
      final Set<GamePlayer> p1NewAllies = new HashSet<>();
      final Collection<GamePlayer> p1AlliedWith =
          CollectionUtils.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1));
      for (final GamePlayer p2 : p1AlliedWith) {
        p1NewAllies.addAll(
            CollectionUtils.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p2)));
      }
      p1NewAllies.removeAll(p1AlliedWith);
      p1NewAllies.remove(p1);
      for (final GamePlayer p3 : p1NewAllies) {
        if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(alliedType)) {
          final RelationshipType current =
              data.getRelationshipTracker().getRelationshipType(p1, p3);
          bridge.addChange(ChangeFactory.relationshipChange(p1, p3, current, alliedType));
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  p1.getName()
                      + " and "
                      + p3.getName()
                      + " are joined together in an "
                      + alliedType.getName()
                      + " treaty");
          MoveDelegate.getBattleTracker(data)
              .addRelationshipChangesThisTurn(p1, p3, current, alliedType);
        }
      }
    }
    // now war
    if (warType == null) {
      return;
    }
    for (final GamePlayer p1 : players) {
      final Set<GamePlayer> p1NewWar = new HashSet<>();
      final Collection<GamePlayer> p1WarWith =
          CollectionUtils.getMatches(players, Matches.isAtWar(p1));
      final Collection<GamePlayer> p1AlliedWith =
          CollectionUtils.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1));
      for (final GamePlayer p2 : p1AlliedWith) {
        p1NewWar.addAll(CollectionUtils.getMatches(players, Matches.isAtWar(p2)));
      }
      p1NewWar.removeAll(p1WarWith);
      p1NewWar.remove(p1);
      for (final GamePlayer p3 : p1NewWar) {
        if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(warType)) {
          final RelationshipType current =
              data.getRelationshipTracker().getRelationshipType(p1, p3);
          bridge.addChange(ChangeFactory.relationshipChange(p1, p3, current, warType));
          bridge
              .getHistoryWriter()
              .addChildToEvent(
                  p1.getName()
                      + " and "
                      + p3.getName()
                      + " declare "
                      + warType.getName()
                      + " on each other");
          MoveDelegate.getBattleTracker(data)
              .addRelationshipChangesThisTurn(p1, p3, current, warType);
        }
      }
    }
  }

  private static void givesBackOriginalTerritories(final IDelegateBridge bridge) {
    final GameState data = bridge.getData();
    final CompositeChange change = new CompositeChange();
    final Collection<GamePlayer> players = data.getPlayerList().getPlayers();
    for (final GamePlayer p1 : players) {
      for (final GamePlayer p2 : players) {
        if (!data.getRelationshipTracker().givesBackOriginalTerritories(p1, p2)) {
          continue;
        }
        for (final Territory t : data.getMap().getTerritoriesOwnedBy(p1)) {
          OriginalOwnerTracker.getOriginalOwner(t)
              .ifPresent(
                  originalOwner -> {
                    if (originalOwner.equals(p2)) {
                      change.add(ChangeFactory.changeOwner(t, originalOwner));
                    }
                  });
        }
      }
    }
    if (!change.isEmpty()) {
      bridge.getHistoryWriter().startEvent("Giving back territories to original owners");
      bridge.addChange(change);
    }
  }
}
