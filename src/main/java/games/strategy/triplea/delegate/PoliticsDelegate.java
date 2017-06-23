package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.RelationshipType;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.delegate.remote.IPoliticsDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.PoliticsText;
import games.strategy.util.Match;

/**
 * Responsible allowing players to perform politicalActions.
 */
@MapSupport
public class PoliticsDelegate extends BaseTripleADelegate implements IPoliticsDelegate {
  // protected HashMap<ICondition, Boolean> m_testedConditions = null;
  // private final boolean m_needToInitialize = true;
  /** Creates new PoliticsDelegate. */
  public PoliticsDelegate() {}

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
  }

  @Override
  public void end() {
    super.end();
    resetAttempts();
    if (games.strategy.triplea.Properties.getTriggers(getData())) {
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      final Match<TriggerAttachment> politicsDelegateTriggerMatch = Match.all(
          TriggerAttachment.availableUses, TriggerAttachment.whenOrDefaultMatch(null, null),
          Match.any(TriggerAttachment.relationshipChangeMatch()));
      // get all possible triggers based on this match.
      final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
          new HashSet<>(Collections.singleton(m_player)), politicsDelegateTriggerMatch, m_bridge);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final HashMap<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, m_bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
            Match.getMatches(toFirePossible, TriggerAttachment.isSatisfiedMatch(testedConditions)));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerRelationshipChange(toFireTestedAndSatisfied, m_bridge, null, null, true, true, true,
            true);
      }
    }
    chainAlliancesTogether(m_bridge);
    givesBackOriginalTerritories(m_bridge);
    // m_needToInitialize = true;
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
    if (!m_player.amNotDeadYet(getData())) {
      return false;
    }
    if (!games.strategy.triplea.Properties.getUsePolitics(getData())) {
      return false;
    }
    return !getValidActions().isEmpty();
  }

  public HashMap<ICondition, Boolean> getTestedConditions() {
    final HashSet<ICondition> allConditionsNeeded = RulesAttachment.getAllConditionsRecursive(
        new HashSet<>(PoliticalActionAttachment.getPoliticalActionAttachments(m_player)), null);
    return RulesAttachment.testAllConditionsRecursive(allConditionsNeeded, null, m_bridge);
  }

  @Override
  public Collection<PoliticalActionAttachment> getValidActions() {
    final GameData data = m_bridge.getData();
    final HashMap<ICondition, Boolean> testedConditions;
    data.acquireReadLock();
    try {
      testedConditions = getTestedConditions();
    } finally {
      data.releaseReadLock();
    }
    return PoliticalActionAttachment.getValidActions(m_player, testedConditions, data);
  }

  @Override
  public Class<IPoliticsDelegate> getRemoteType() {
    return IPoliticsDelegate.class;
  }

  @Override
  public void attemptAction(final PoliticalActionAttachment paa) {
    if (!games.strategy.triplea.Properties.getUsePolitics(getData())) {
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
        notifyMoney(paa, false);
      }
    } else {
      // notify the player the action isn't valid anymore (shouldn't happen)
      notifyNoValidAction(paa);
    }
  }

  /**
   * Get a list of players that should accept this action and then ask each
   * player if it accepts this action.
   *
   * @param paa
   *        the politicalActionAttachment that should be accepted
   */
  private boolean actionIsAccepted(final PoliticalActionAttachment paa) {
    final GameData data = getData();
    final Match<PoliticalActionAttachment> intoAlliedChainOrIntoOrOutOfWar =
        Match.any(
            Matches.politicalActionIsRelationshipChangeOf(null,
                Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.invert(),
                Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether, data),
            Matches.politicalActionIsRelationshipChangeOf(null, Matches.RelationshipTypeIsAtWar.invert(),
                Matches.RelationshipTypeIsAtWar, data),
            Matches.politicalActionIsRelationshipChangeOf(null, Matches.RelationshipTypeIsAtWar,
                Matches.RelationshipTypeIsAtWar.invert(), data));
    if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data)
        || !intoAlliedChainOrIntoOrOutOfWar.match(paa)) {
      for (final PlayerID player : paa.getActionAccept()) {
        if (!(getRemotePlayer(player)).acceptAction(m_player,
            PoliticsText.getInstance().getAcceptanceQuestion(paa.getText()), true)) {
          return false;
        }
      }
    } else {
      // if alliances chain together, then our allies must have a say in anyone becoming a new ally/enemy
      final LinkedHashSet<PlayerID> playersWhoNeedToAccept = new LinkedHashSet<>();
      playersWhoNeedToAccept.addAll(paa.getActionAccept());
      playersWhoNeedToAccept.addAll(Match.getMatches(data.getPlayerList().getPlayers(),
          Matches.isAlliedAndAlliancesCanChainTogether(m_player, data)));
      for (final PlayerID player : paa.getActionAccept()) {
        playersWhoNeedToAccept.addAll(Match.getMatches(data.getPlayerList().getPlayers(),
            Matches.isAlliedAndAlliancesCanChainTogether(player, data)));
      }
      final HashSet<PlayerID> alliesWhoMustAccept = playersWhoNeedToAccept;
      alliesWhoMustAccept.removeAll(paa.getActionAccept());
      for (final PlayerID player : playersWhoNeedToAccept) {
        String actionText = PoliticsText.getInstance().getAcceptanceQuestion(paa.getText());
        if (actionText.equals("NONE")) {
          actionText = m_player.getName() + " wants to take the following action: "
              + MyFormatter.attachmentNameToText(paa.getName()) + " \r\n Do you approve?";
        } else {
          actionText = m_player.getName() + " wants to take the following action: "
              + MyFormatter.attachmentNameToText(paa.getName()) + ".  Do you approve? \r\n\r\n " + m_player.getName()
              + " will ask " + MyFormatter.defaultNamedToTextList(paa.getActionAccept())
              + ", the following question: \r\n " + actionText;
        }
        if (!(getRemotePlayer(player)).acceptAction(m_player, actionText, true)) {
          return false;
        }
      }
      for (final PlayerID player : paa.getActionAccept()) {
        if (!(getRemotePlayer(player)).acceptAction(m_player,
            PoliticsText.getInstance().getAcceptanceQuestion(paa.getText()), true)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Let the player know this action isn't valid anymore, this shouldn't
   * happen as the player shouldn't get an option to push the button on
   * non-valid actions.
   */
  private void notifyNoValidAction(final PoliticalActionAttachment paa) {
    sendNotification("This action isn't available anymore (this shouldn't happen!?!)");
  }

  private void notifyPoliticsTurnedOff() {
    sendNotification("Politics is turned off in the game options");
  }

  /**
   * Let the player know he is being charged for money or that he hasn't got
   * enough money
   *
   * @param paa
   *        the actionattachment the player is notified about
   * @param enough
   *        is this a notification about enough or not enough money.
   */
  private void notifyMoney(final PoliticalActionAttachment paa, final boolean enough) {
    if (enough) {
      sendNotification("Charging " + paa.getCostPU() + " PU's to perform this action");
    } else {
      sendNotification("You don't have ennough money, you need " + paa.getCostPU() + " PU's to perform this action");
    }
  }

  /**
   * Subtract money from the players wallet
   *
   * @param paa
   *        the politicalactionattachment this the money is charged for.
   */
  private void chargeForAction(final PoliticalActionAttachment paa) {
    final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
    final int cost = paa.getCostPU();
    if (cost > 0) {
      // don't notify user of spending money anymore
      // notifyMoney(paa, true);
      final String transcriptText = m_bridge.getPlayerID().getName() + " spend " + cost + " PU on Political Action: "
          + MyFormatter.attachmentNameToText(paa.getName());
      m_bridge.getHistoryWriter().startEvent(transcriptText);
      final Change charge = ChangeFactory.changeResourcesChange(m_bridge.getPlayerID(), PUs, -cost);
      m_bridge.addChange(charge);
    } else {
      final String transcriptText = m_bridge.getPlayerID().getName() + " takes Political Action: "
          + MyFormatter.attachmentNameToText(paa.getName());
      // we must start an event anyway
      m_bridge.getHistoryWriter().startEvent(transcriptText);
    }
  }

  /**
   * @param paa
   *        The Political Action the player should be charged for.
   * @return false if the player can't afford the action
   */
  private boolean checkEnoughMoney(final PoliticalActionAttachment paa) {
    final Resource PUs = getData().getResourceList().getResource(Constants.PUS);
    final int cost = paa.getCostPU();
    final int has = m_bridge.getPlayerID().getResources().getQuantity(PUs);
    return has >= cost;
  }

  /**
   * Let all players involved in this action know the action has failed.
   *
   * @param paa
   *        the political action attachment that just failed.
   */
  private void notifyFailure(final PoliticalActionAttachment paa) {
    getSoundChannel().playSoundForAll(SoundPath.CLIP_POLITICAL_ACTION_FAILURE, m_player);
    final String transcriptText =
        m_bridge.getPlayerID().getName() + " fails on action: " + MyFormatter.attachmentNameToText(paa.getName());
    m_bridge.getHistoryWriter().addChildToEvent(transcriptText);
    sendNotification(PoliticsText.getInstance().getNotificationFailure(paa.getText()));
    notifyOtherPlayers(PoliticsText.getInstance().getNotificationFailureOthers(paa.getText()));
  }

  /**
   * Let all players involved in this action know the action was successful
   *
   * @param paa the political action attachment that just succeeded.
   */
  private void notifySuccess(final PoliticalActionAttachment paa) {
    getSoundChannel().playSoundForAll(SoundPath.CLIP_POLITICAL_ACTION_SUCCESSFUL, m_player);
    sendNotification(PoliticsText.getInstance().getNotificationSucccess(paa.getText()));
    notifyOtherPlayers(PoliticsText.getInstance().getNotificationSuccessOthers(paa.getText()));
  }

  /**
   * Send a notification to the other players involved in this action (all
   * players except the player starting the action).
   */
  private void notifyOtherPlayers(final String notification) {
    if (!"NONE".equals(notification)) {
      // we can send it to just paa.getOtherPlayers(), or we can send it to all players. both are good options.
      final Collection<PlayerID> currentPlayer = new ArrayList<>();
      currentPlayer.add(m_player);
      final Collection<PlayerID> otherPlayers = getData().getPlayerList().getPlayers();
      otherPlayers.removeAll(currentPlayer);
      this.getDisplay().reportMessageToPlayers(otherPlayers, currentPlayer, notification, notification);
    }
  }

  /**
   * Send a notification to the current player.
   *
   * @param text
   *        if NONE don't send a notification
   */
  private void sendNotification(final String text) {
    if (!"NONE".equals(text)) {
      this.getRemotePlayer().reportMessage(text, text);
    }
  }

  /**
   * Changes all relationships.
   *
   * @param paa
   *        the political action to change the relationships for
   */
  private void changeRelationships(final PoliticalActionAttachment paa) {
    getMyselfOutOfAlliance(paa, m_player, m_bridge);
    getNeutralOutOfWarWithAllies(paa, m_player, m_bridge);
    final CompositeChange change = new CompositeChange();
    for (final String relationshipChange : paa.getRelationshipChange()) {
      final String[] s = relationshipChange.split(":");
      final PlayerID player1 = getData().getPlayerList().getPlayerID(s[0]);
      final PlayerID player2 = getData().getPlayerList().getPlayerID(s[1]);
      final RelationshipType oldRelation = getData().getRelationshipTracker().getRelationshipType(player1, player2);
      final RelationshipType newRelation = getData().getRelationshipTypeList().getRelationshipType(s[2]);
      if (oldRelation.equals(newRelation)) {
        continue;
      }
      change.add(ChangeFactory.relationshipChange(player1, player2, oldRelation, newRelation));
      m_bridge.getHistoryWriter()
          .addChildToEvent(m_bridge.getPlayerID().getName() + " succeeds on action: "
              + MyFormatter.attachmentNameToText(paa.getName()) + ": Changing Relationship for " + player1.getName()
              + " and " + player2.getName() + " from " + oldRelation.getName() + " to " + newRelation.getName());
      MoveDelegate.getBattleTracker(getData()).addRelationshipChangesThisTurn(player1, player2, oldRelation,
          newRelation);
    }
    if (!change.isEmpty()) {
      m_bridge.addChange(change);
    }
    chainAlliancesTogether(m_bridge);
  }

  /**
   * @param paa
   *        the action to check if it succeeds
   * @return true if the action succeeds, usually because the die-roll succeeded.
   */
  private boolean actionRollSucceeds(final PoliticalActionAttachment paa) {
    final int hitTarget = paa.getChanceToHit();
    final int diceSides = paa.getChanceDiceSides();
    if (diceSides <= 0 || hitTarget >= diceSides) {
      paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, true, true);
      return true;
    } else if (hitTarget <= 0) {
      paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, false, true);
      return false;
    }
    final int rollResult = m_bridge.getRandom(diceSides, m_player, DiceType.NONCOMBAT,
        "Attempting the Political Action: " + MyFormatter.attachmentNameToText(paa.getName())) + 1;
    final boolean success = rollResult <= hitTarget;
    final String notificationMessage = "rolling (" + hitTarget + " out of " + diceSides + ") result: " + rollResult
        + " = " + (success ? "Success!" : "Failure!");
    m_bridge.getHistoryWriter()
        .addChildToEvent(MyFormatter.attachmentNameToText(paa.getName()) + " : " + notificationMessage);
    paa.changeChanceDecrementOrIncrementOnSuccessOrFailure(m_bridge, success, true);
    sendNotification(notificationMessage);
    return success;
  }

  /**
   * Reset the attempts-counter for this action, so next round the player can
   * try again for a number of attempts.
   */
  private void resetAttempts() {
    for (final PoliticalActionAttachment paa : PoliticalActionAttachment.getPoliticalActionAttachments(m_player)) {
      paa.resetAttempts(getBridge());
    }
  }

  private static void getMyselfOutOfAlliance(final PoliticalActionAttachment paa, final PlayerID player,
      final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data)) {
      return;
    }
    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    final Collection<PlayerID> p1AlliedWith =
        Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player, data));
    p1AlliedWith.remove(player);
    final CompositeChange change = new CompositeChange();
    for (final String relationshipChangeString : paa.getRelationshipChange()) {
      final String[] relationshipChange = relationshipChangeString.split(":");
      final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
      final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
      if (!(p1.equals(player) || p2.equals(player))) {
        continue;
      }
      final PlayerID pOther = (p1.equals(player) ? p2 : p1);
      if (!p1AlliedWith.contains(pOther)) {
        continue;
      }
      final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
      final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
      if (Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.match(currentType)
          && Matches.RelationshipTypeIsAlliedAndAlliancesCanChainTogether.invert().match(newType)) {
        for (final PlayerID p3 : p1AlliedWith) {
          final RelationshipType currentOther = data.getRelationshipTracker().getRelationshipType(p3, player);
          if (!currentOther.equals(newType)) {
            change.add(ChangeFactory.relationshipChange(p3, player, currentOther, newType));
            aBridge.getHistoryWriter().addChildToEvent(
                player.getName() + " and " + p3.getName() + " sign a " + newType.getName() + " treaty");
            MoveDelegate.getBattleTracker(data).addRelationshipChangesThisTurn(p3, player, currentOther, newType);
          }
        }
      }
    }
    if (!change.isEmpty()) {
      aBridge.addChange(change);
    }
  }

  private static void getNeutralOutOfWarWithAllies(final PoliticalActionAttachment paa, final PlayerID player,
      final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data)) {
      return;
    }

    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    final Collection<PlayerID> p1AlliedWith =
        Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(player, data));
    final CompositeChange change = new CompositeChange();
    for (final String relationshipChangeString : paa.getRelationshipChange()) {
      final String[] relationshipChange = relationshipChangeString.split(":");
      final PlayerID p1 = data.getPlayerList().getPlayerID(relationshipChange[0]);
      final PlayerID p2 = data.getPlayerList().getPlayerID(relationshipChange[1]);
      if (!(p1.equals(player) || p2.equals(player))) {
        continue;
      }
      final PlayerID pOther = (p1.equals(player) ? p2 : p1);
      final RelationshipType currentType = data.getRelationshipTracker().getRelationshipType(p1, p2);
      final RelationshipType newType = data.getRelationshipTypeList().getRelationshipType(relationshipChange[2]);
      if (Matches.RelationshipTypeIsAtWar.match(currentType)
          && Matches.RelationshipTypeIsAtWar.invert().match(newType)) {
        final Collection<PlayerID> pOtherAlliedWith =
            Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(pOther, data));
        if (!pOtherAlliedWith.contains(pOther)) {
          pOtherAlliedWith.add(pOther);
        }
        if (!p1AlliedWith.contains(player)) {
          p1AlliedWith.add(player);
        }
        for (final PlayerID p3 : p1AlliedWith) {
          for (final PlayerID p4 : pOtherAlliedWith) {
            final RelationshipType currentOther = data.getRelationshipTracker().getRelationshipType(p3, p4);
            if (!currentOther.equals(newType) && Matches.RelationshipTypeIsAtWar.match(currentOther)) {
              change.add(ChangeFactory.relationshipChange(p3, p4, currentOther, newType));
              aBridge.getHistoryWriter()
                  .addChildToEvent(p3.getName() + " and " + p4.getName() + " sign a " + newType.getName() + " treaty");
              MoveDelegate.getBattleTracker(data).addRelationshipChangesThisTurn(p3, p4, currentOther, newType);
            }
          }
        }
      }
    }
    if (!change.isEmpty()) {
      aBridge.addChange(change);
    }
  }

  static void chainAlliancesTogether(final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    if (!games.strategy.triplea.Properties.getAlliancesCanChainTogether(data)) {
      return;
    }
    final Collection<RelationshipType> allTypes = data.getRelationshipTypeList().getAllRelationshipTypes();
    RelationshipType alliedType = null;
    RelationshipType warType = null;
    for (final RelationshipType type : allTypes) {
      if (type.getRelationshipTypeAttachment().getIsDefaultWarPosition()) {
        warType = type;
      } else if (type.getRelationshipTypeAttachment().getAlliancesCanChainTogether()) {
        alliedType = type;
      }
    }
    if (alliedType == null) {
      return;
    }
    // first do alliances. then, do war (since we don't want to declare war on a potential ally).
    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    for (final PlayerID p1 : players) {
      final HashSet<PlayerID> p1NewAllies = new HashSet<>();
      final Collection<PlayerID> p1AlliedWith =
          Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1, data));
      for (final PlayerID p2 : p1AlliedWith) {
        p1NewAllies.addAll(Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p2, data)));
      }
      p1NewAllies.removeAll(p1AlliedWith);
      p1NewAllies.remove(p1);
      for (final PlayerID p3 : p1NewAllies) {
        if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(alliedType)) {
          final RelationshipType current = data.getRelationshipTracker().getRelationshipType(p1, p3);
          aBridge.addChange(ChangeFactory.relationshipChange(p1, p3, current, alliedType));
          aBridge.getHistoryWriter().addChildToEvent(
              p1.getName() + " and " + p3.getName() + " are joined together in an " + alliedType.getName() + " treaty");
          MoveDelegate.getBattleTracker(data).addRelationshipChangesThisTurn(p1, p3, current, alliedType);
        }
      }
    }
    // now war
    if (warType == null) {
      return;
    }
    for (final PlayerID p1 : players) {
      final HashSet<PlayerID> p1NewWar = new HashSet<>();
      final Collection<PlayerID> p1WarWith = Match.getMatches(players, Matches.isAtWar(p1, data));
      final Collection<PlayerID> p1AlliedWith =
          Match.getMatches(players, Matches.isAlliedAndAlliancesCanChainTogether(p1, data));
      for (final PlayerID p2 : p1AlliedWith) {
        p1NewWar.addAll(Match.getMatches(players, Matches.isAtWar(p2, data)));
      }
      p1NewWar.removeAll(p1WarWith);
      p1NewWar.remove(p1);
      for (final PlayerID p3 : p1NewWar) {
        if (!data.getRelationshipTracker().getRelationshipType(p1, p3).equals(warType)) {
          final RelationshipType current = data.getRelationshipTracker().getRelationshipType(p1, p3);
          aBridge.addChange(ChangeFactory.relationshipChange(p1, p3, current, warType));
          aBridge.getHistoryWriter().addChildToEvent(
              p1.getName() + " and " + p3.getName() + " declare " + warType.getName() + " on each other");
          MoveDelegate.getBattleTracker(data).addRelationshipChangesThisTurn(p1, p3, current, warType);
        }
      }
    }
  }

  private static void givesBackOriginalTerritories(final IDelegateBridge aBridge) {
    final GameData data = aBridge.getData();
    final CompositeChange change = new CompositeChange();
    final Collection<PlayerID> players = data.getPlayerList().getPlayers();
    for (final PlayerID p1 : players) {
      for (final PlayerID p2 : players) {
        if (!data.getRelationshipTracker().givesBackOriginalTerritories(p1, p2)) {
          continue;
        }
        for (final Territory t : data.getMap().getTerritoriesOwnedBy(p1)) {
          final PlayerID original = OriginalOwnerTracker.getOriginalOwner(t);
          if (original == null) {
            continue;
          }
          if (original.equals(p2)) {
            change.add(ChangeFactory.changeOwner(t, original));
          }
        }
      }
    }
    if (!change.isEmpty()) {
      aBridge.getHistoryWriter().startEvent("Giving back territories to original owners");
      aBridge.addChange(change);
    }
  }
}


class PoliticsExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = -3829812751864156598L;
  Serializable superState;
  // add other variables here:
  // public HashMap<ICondition, Boolean> m_testedConditions = null;
}
