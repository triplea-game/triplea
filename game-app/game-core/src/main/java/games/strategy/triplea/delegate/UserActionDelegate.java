package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.triplea.attachments.AbstractConditionsAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.UserActionAttachment;
import games.strategy.triplea.delegate.remote.IUserActionDelegate;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.UserActionText;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.IntegerMap;
import org.triplea.sound.SoundPath;

/** Contains validation and logic to change game data for UserActionAttachments. */
public class UserActionDelegate extends BaseTripleADelegate implements IUserActionDelegate {
  @Override
  public void end() {
    super.end();
    resetAttempts();
  }

  @Override
  public Serializable saveState() {
    final UserActionExtendedDelegateState state = new UserActionExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final UserActionExtendedDelegateState s = (UserActionExtendedDelegateState) state;
    super.loadState(s.superState);
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return !getValidActions().isEmpty();
  }

  private Map<ICondition, Boolean> getTestedConditions() {
    final Set<ICondition> allConditionsNeeded =
        AbstractConditionsAttachment.getAllConditionsRecursive(
            new HashSet<>(UserActionAttachment.getUserActionAttachments(player)), null);
    return AbstractConditionsAttachment.testAllConditionsRecursive(
        allConditionsNeeded, null, bridge);
  }

  @Override
  public Collection<UserActionAttachment> getValidActions() {
    final GameData data = bridge.getData();
    data.acquireReadLock();
    final Map<ICondition, Boolean> testedConditions;
    try {
      testedConditions = getTestedConditions();
    } finally {
      data.releaseReadLock();
    }
    return UserActionAttachment.getValidActions(player, testedConditions);
  }

  @Override
  public void attemptAction(final UserActionAttachment actionChoice) {
    if (actionChoice.canPerform(getTestedConditions())) {
      if (checkEnoughMoney(
          actionChoice)) { // See if the player has got enough money to pay for the action
        // Charge for attempting the action
        chargeForAction(actionChoice);
        // take one of the uses this round
        actionChoice.useAttempt(getBridge());
        if (actionRollSucceeds(actionChoice)) { // See if the action is successful
          if (actionIsAccepted(actionChoice)) {
            // activate the triggers
            activateTriggers(actionChoice);
            // notify the players
            notifySuccess(actionChoice);
          } else {
            // notify the players of the failed attempt
            notifyFailure(actionChoice);
          }
        } else {
          // notify the players of the failed attempt
          notifyFailure(actionChoice);
        }
      } else {
        // notify the player he hasn't got enough money;
        notifyMoney(actionChoice);
      }
    } else {
      // notify the player the action isn't valid anymore (shouldn't happen)
      notifyNoValidAction();
    }
  }

  private boolean checkEnoughMoney(final UserActionAttachment userActionAttachment) {
    return bridge.getGamePlayer().getResources().has(userActionAttachment.getCostResources());
  }

  /**
   * Subtract money from the player's wallet.
   *
   * @param userActionAttachment the UserActionAttachment this the money is charged for.
   */
  private void chargeForAction(final UserActionAttachment userActionAttachment) {
    final IntegerMap<Resource> cost = userActionAttachment.getCostResources();
    if (!cost.isEmpty()) {
      final String transcriptText =
          bridge.getGamePlayer().getName()
              + " spend "
              + ResourceCollection.toString(cost, getData())
              + " on User Action: "
              + MyFormatter.attachmentNameToText(userActionAttachment.getName());
      bridge.getHistoryWriter().startEvent(transcriptText);
      final Change charge =
          ChangeFactory.removeResourceCollection(
              bridge.getGamePlayer(), new ResourceCollection(getData(), cost));
      bridge.addChange(charge);
    } else {
      final String transcriptText =
          bridge.getGamePlayer().getName()
              + " takes action: "
              + MyFormatter.attachmentNameToText(userActionAttachment.getName());
      // we must start an event anyway
      bridge.getHistoryWriter().startEvent(transcriptText);
    }
  }

  /**
   * Executes the specified action.
   *
   * @param userActionAttachment the action to check if it succeeds
   * @return true if the action succeeds, usually because the die-roll succeeded.
   */
  private boolean actionRollSucceeds(final UserActionAttachment userActionAttachment) {
    final int hitTarget = userActionAttachment.getChanceToHit();
    final int diceSides = userActionAttachment.getChanceDiceSides();
    if (diceSides <= 0 || hitTarget >= diceSides) {
      userActionAttachment.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, true, true);
      return true;
    } else if (hitTarget <= 0) {
      userActionAttachment.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, false, true);
      return false;
    }
    final int rollResult =
        bridge.getRandom(
                diceSides,
                player,
                DiceType.NONCOMBAT,
                "Attempting the User Action: "
                    + MyFormatter.attachmentNameToText(userActionAttachment.getName()))
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
            MyFormatter.attachmentNameToText(userActionAttachment.getName())
                + " : "
                + notificationMessage);
    userActionAttachment.changeChanceDecrementOrIncrementOnSuccessOrFailure(bridge, success, true);
    sendNotification(notificationMessage);
    return success;
  }

  /**
   * Get a list of players that should accept this action and then ask each player if it accepts
   * this action.
   *
   * @param userActionAttachment the UserActionAttachment that should be accepted
   */
  private boolean actionIsAccepted(final UserActionAttachment userActionAttachment) {
    for (final GamePlayer player : userActionAttachment.getActionAccept()) {
      if (!getRemotePlayer(player)
          .acceptAction(
              this.player,
              UserActionText.getInstance().getAcceptanceQuestion(userActionAttachment.getText()),
              false)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Fire triggers.
   *
   * @param userActionAttachment the UserActionAttachment to activate triggers for
   */
  private void activateTriggers(final UserActionAttachment userActionAttachment) {
    UserActionAttachment.fireTriggers(userActionAttachment, getTestedConditions(), bridge);
  }

  /**
   * Send a notification to the current player.
   *
   * @param notificationText if NONE don't send a notification
   */
  private void sendNotification(final String notificationText) {
    if (!"NONE".equals(notificationText)) {
      bridge.getRemotePlayer().reportMessage(notificationText, notificationText);
    }
  }

  private void sendNotificationToPlayers(
      final Collection<GamePlayer> toPlayers,
      final Collection<GamePlayer> dontSendTo,
      final String notificationText) {
    if (!"NONE".equals(notificationText)) {
      bridge
          .getDisplayChannelBroadcaster()
          .reportMessageToPlayers(toPlayers, dontSendTo, notificationText, notificationText);
    }
  }

  /**
   * Send notifications to the other players (all players except the player starting the action).
   */
  private void notifyOtherPlayers(
      final UserActionAttachment userActionAttachment,
      final String notification,
      final String targetNotification) {
    final Collection<GamePlayer> dontSendTo = new ArrayList<>();
    dontSendTo.add(player);

    final Collection<GamePlayer> targets = userActionAttachment.getActionAccept();
    sendNotificationToPlayers(targets, dontSendTo, targetNotification);

    final Collection<GamePlayer> otherPlayers = getData().getPlayerList().getPlayers();
    otherPlayers.remove(player);
    otherPlayers.removeAll(targets);
    dontSendTo.addAll(targets);
    sendNotificationToPlayers(otherPlayers, dontSendTo, notification);
  }

  /**
   * Let all players involved in this action know the action was successful.
   *
   * @param userActionAttachment the UserActionAttachment that just succeeded.
   */
  private void notifySuccess(final UserActionAttachment userActionAttachment) {
    // play a sound
    bridge
        .getSoundChannelBroadcaster()
        .playSoundForAll(SoundPath.CLIP_USER_ACTION_SUCCESSFUL, player);
    final UserActionText uat = UserActionText.getInstance();
    final String text = userActionAttachment.getText();
    sendNotification(uat.getNotificationSuccess(text));
    notifyOtherPlayers(
        userActionAttachment,
        uat.getNotificationSuccessOthers(text),
        uat.getNotificationSuccessTarget(text));
  }

  /**
   * Let all players involved in this action know the action has failed.
   *
   * @param userActionAttachment the UserActionAttachment that just failed.
   */
  private void notifyFailure(final UserActionAttachment userActionAttachment) {
    // play a sound
    bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_USER_ACTION_FAILURE, player);
    final String transcriptText =
        bridge.getGamePlayer().getName()
            + " fails on action: "
            + MyFormatter.attachmentNameToText(userActionAttachment.getName());
    bridge.getHistoryWriter().addChildToEvent(transcriptText);
    final UserActionText uat = UserActionText.getInstance();
    final String text = userActionAttachment.getText();
    sendNotification(uat.getNotificationFailure(text));
    notifyOtherPlayers(
        userActionAttachment,
        uat.getNotificationFailureOthers(text),
        uat.getNotificationFailureTarget(text));
  }

  /**
   * Let the player know he doesn't have enough money.
   *
   * @param userActionAttachment the UserActionAttachment the player is notified about
   */
  private void notifyMoney(final UserActionAttachment userActionAttachment) {
    sendNotification(
        "You don't have enough money, you need "
            + ResourceCollection.toString(userActionAttachment.getCostResources(), getData())
            + " to perform this action");
  }

  /**
   * Let the player know this action isn't valid anymore, this shouldn't happen as the player
   * shouldn't get an option to push the button on non-valid actions.
   */
  private void notifyNoValidAction() {
    sendNotification("This action isn't available anymore (this shouldn't happen!?!)");
  }

  /**
   * Reset the attempts-counter for this action, so next round the player can try again for a number
   * of attempts.
   */
  private void resetAttempts() {
    UserActionAttachment.getUserActionAttachments(player)
        .forEach(userActionAttachment -> userActionAttachment.resetAttempts(getBridge()));
  }

  @Override
  public Class<IUserActionDelegate> getRemoteType() {
    return IUserActionDelegate.class;
  }
}
