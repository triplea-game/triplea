package games.strategy.triplea.delegate;

import com.google.common.base.Preconditions;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.FireTriggerParams;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.settings.ClientSetting;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import lombok.Getter;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.sound.SoundPath;

/** A delegate used to check for end of game conditions. */
public class EndRoundDelegate extends BaseTripleADelegate {
  private boolean gameOver = false;
  @Getter private Collection<GamePlayer> winners = new ArrayList<>();

  public EndRoundDelegate() {}

  @Override
  public void start() {
    super.start();
    if (gameOver) {
      return;
    }
    String victoryMessage;
    final GameState data = getData();
    if (Properties.getPacificTheater(getData().getProperties())) {
      final GamePlayer japanese = data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_JAPANESE);
      final PlayerAttachment pa = PlayerAttachment.get(japanese);
      if (pa != null && pa.getVps() >= 22) {
        victoryMessage = "Axis achieve VP victory";
        bridge.getHistoryWriter().startEvent(victoryMessage);
        final Collection<GamePlayer> winners =
            data.getAllianceTracker()
                .getPlayersInAlliance(
                    CollectionUtils.getAny(
                        data.getAllianceTracker().getAlliancesPlayerIsIn(japanese)));
        signalGameOver(victoryMessage, winners, bridge);
      }
    }
    // Check for Winning conditions
    if (Properties.getTotalVictory(getData().getProperties())) { // Check for Win by Victory Cities
      victoryMessage = " achieve TOTAL VICTORY with ";
      checkVictoryCities(bridge, victoryMessage, " Total Victory VCs");
    }
    if (Properties.getHonorableSurrender(getData().getProperties())) {
      victoryMessage = " achieve an HONORABLE VICTORY with ";
      checkVictoryCities(bridge, victoryMessage, " Honorable Victory VCs");
    }
    if (Properties.getProjectionOfPower(getData().getProperties())) {
      victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
      checkVictoryCities(bridge, victoryMessage, " Projection of Power VCs");
    }
    if (Properties.getEconomicVictory(
        getData().getProperties())) { // Check for regular economic victory
      for (final String allianceName : data.getAllianceTracker().getAlliances()) {
        final int victoryAmount = getEconomicVictoryAmount(data, allianceName);
        final Set<GamePlayer> teamMembers =
            data.getAllianceTracker().getPlayersInAlliance(allianceName);
        int teamProd = 0;
        for (final GamePlayer player : teamMembers) {
          teamProd += getProduction(player);
          if (teamProd >= victoryAmount) {
            victoryMessage = allianceName + " achieve economic victory";
            bridge.getHistoryWriter().startEvent(victoryMessage);
            final Collection<GamePlayer> winners =
                data.getAllianceTracker().getPlayersInAlliance(allianceName);
            // Added this to end the game on victory conditions
            signalGameOver(victoryMessage, winners, bridge);
          }
        }
      }
    }
    // now check for generic trigger based victories
    if (Properties.getTriggeredVictory(getData().getProperties())) {
      // First set up a match for what we want to have fire as a default in this delegate. List out
      // as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT
      // have 'when' set.
      final Predicate<TriggerAttachment> endRoundDelegateTriggerMatch =
          AbstractTriggerAttachment.availableUses
              .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
              .and(TriggerAttachment.activateTriggerMatch().or(TriggerAttachment.victoryMatch()));
      // get all possible triggers based on this match.
      final Set<TriggerAttachment> toFirePossible =
          TriggerAttachment.collectForAllTriggersMatching(
              Set.copyOf(data.getPlayerList().getPlayers()), endRoundDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final Map<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final Set<TriggerAttachment> toFireTestedAndSatisfied =
            new HashSet<>(
                CollectionUtils.getMatches(
                    toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
        // now list out individual types to fire, once for each of the matches above.
        final FireTriggerParams fireTriggerParams =
            new FireTriggerParams(null, null, true, true, true, true);
        TriggerAttachment.triggerActivateTriggerOther(
            testedConditions, toFireTestedAndSatisfied, bridge, fireTriggerParams);
        // will call
        TriggerAttachment.triggerVictory(toFireTestedAndSatisfied, bridge, fireTriggerParams);
        // signalGameOver itself
      }
    }
    if (Properties.getWW2V2(getData().getProperties())
        || Properties.getWW2V3(getData().getProperties())) {
      return;
    }
    final PlayerList playerList = data.getPlayerList();
    // now test older maps that only use these 5 players, to see if someone has won
    final GamePlayer russians = playerList.getPlayerId(Constants.PLAYER_NAME_RUSSIANS);
    final GamePlayer germans = playerList.getPlayerId(Constants.PLAYER_NAME_GERMANS);
    final GamePlayer british = playerList.getPlayerId(Constants.PLAYER_NAME_BRITISH);
    final GamePlayer japanese = playerList.getPlayerId(Constants.PLAYER_NAME_JAPANESE);
    final GamePlayer americans = playerList.getPlayerId(Constants.PLAYER_NAME_AMERICANS);
    if (germans == null
        || russians == null
        || british == null
        || japanese == null
        || americans == null
        || playerList.size() > 5) {
      return;
    }
    // Quick check to see who still owns their own capital
    final boolean russia =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(
                russians, data.getMap())
            .getOwner()
            .equals(russians);
    final boolean germany =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(germans, data.getMap())
            .getOwner()
            .equals(germans);
    final boolean britain =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(british, data.getMap())
            .getOwner()
            .equals(british);
    final boolean japan =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(
                japanese, data.getMap())
            .getOwner()
            .equals(japanese);
    final boolean america =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapitalOrThrow(
                americans, data.getMap())
            .getOwner()
            .equals(americans);
    int count = 0;
    if (!russia) {
      count++;
    }
    if (!britain) {
      count++;
    }
    if (!america) {
      count++;
    }
    victoryMessage = " achieve a military victory";
    if (germany && japan && count >= 2) {
      bridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
      final Collection<GamePlayer> winners = data.getAllianceTracker().getPlayersInAlliance("Axis");
      signalGameOver("Axis" + victoryMessage, winners, bridge);
    }
    if (russia && !germany && britain && !japan && america) {
      bridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
      final Collection<GamePlayer> winners =
          data.getAllianceTracker().getPlayersInAlliance("Allies");
      signalGameOver("Allies" + victoryMessage, winners, bridge);
    }
  }

  @Override
  public void end() {
    super.end();
    final GameState data = getData();
    if (Properties.getTriggers(data.getProperties())) {
      final CompositeChange change = new CompositeChange();
      for (final GamePlayer player : data.getPlayerList().getPlayers()) {
        change.add(AbstractTriggerAttachment.triggerSetUsedForThisRound(player));
      }
      if (!change.isEmpty()) {
        bridge.getHistoryWriter().startEvent("Setting uses for triggers used this round.");
        bridge.addChange(change);
      }
    }
  }

  @Override
  public Serializable saveState() {
    final EndRoundExtendedDelegateState state = new EndRoundExtendedDelegateState();
    state.superState = super.saveState();
    state.gameOver = gameOver;
    state.winners = winners;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final EndRoundExtendedDelegateState s = (EndRoundExtendedDelegateState) state;
    super.loadState(s.superState);
    gameOver = s.gameOver;
    winners = s.winners;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  private void checkVictoryCities(
      final IDelegateBridge bridge, final String victoryMessage, final String victoryType) {
    final GameState data = bridge.getData();

    final Collection<Territory> territories = data.getMap().getTerritories();
    for (final String allianceName : data.getAllianceTracker().getAlliances()) {
      final int vcAmount = getVcAmount(data, allianceName, victoryType);
      final Set<GamePlayer> teamMembers =
          data.getAllianceTracker().getPlayersInAlliance(allianceName);
      int teamVCs = 0;
      for (final Territory t : territories) {
        if (Matches.isTerritoryOwnedByAnyOf(teamMembers).test(t)) {
          teamVCs += TerritoryAttachment.get(t).map(TerritoryAttachment::getVictoryCity).orElse(0);
        }
      }
      if (teamVCs >= vcAmount) {
        bridge
            .getHistoryWriter()
            .startEvent(allianceName + victoryMessage + vcAmount + " Victory Cities!");
        final Collection<GamePlayer> winners =
            data.getAllianceTracker().getPlayersInAlliance(allianceName);
        // Added this to end the game on victory conditions
        signalGameOver(
            allianceName + victoryMessage + vcAmount + " Victory Cities!", winners, bridge);
      }
    }
  }

  private static int getEconomicVictoryAmount(final GameState data, final String alliance) {
    return data.getProperties().get(alliance + " Economic Victory", 200);
  }

  private static int getVcAmount(final GameState data, final String alliance, final String type) {
    int defaultVc = 20;
    switch (type) {
      case " Total Victory VCs":
        defaultVc = 18;
        break;
      case " Honorable Victory VCs":
        defaultVc = 15;
        break;
      case " Projection of Power VCs":
        defaultVc = 13;
        break;
      default:
        break;
    }
    return data.getProperties().get((alliance + type), defaultVc);
  }

  /**
   * Notify all players that the game is over.
   *
   * @param status the "game over" text to be displayed to each user.
   */
  public void signalGameOver(
      final String status, final Collection<GamePlayer> winners, final IDelegateBridge bridge) {
    // TO NOT USE playerBridge, because it might be null here! use aBridge instead.
    // If the game is over, we need to be able to alert all UIs to that fact.
    // The display object can send a message to all UIs.
    if (!gameOver) {
      gameOver = true;
      this.winners = winners;
      bridge
          .getSoundChannelBroadcaster()
          .playSoundForAll(
              SoundPath.CLIP_GAME_WON,
              ((this.winners != null && !this.winners.isEmpty())
                  ? CollectionUtils.getAny(this.winners)
                  : getData().getPlayerList().getNullPlayer()));
      // send a message to everyone's screen except the HOST (there is no 'current player' for the
      // end round delegate)
      final String title =
          "Victory Achieved"
              + (winners.isEmpty() ? "" : " by " + MyFormatter.defaultNamedToTextList(winners));

      if (ClientSetting.useWebsocketNetwork.getValue().orElse(false)) {
        Preconditions.checkNotNull(clientNetworkBridge);
        clientNetworkBridge.sendMessage(
            IDisplay.BroadcastMessageMessage.builder()
                .message("<html>" + status + "</html>")
                .title(title)
                .build());
      } else {
        // we send the bridge, because we can call this method from outside this delegate, which
        // means our local copy of playerBridge could be null.
        bridge
            .getDisplayChannelBroadcaster()
            .reportMessageToAll(("<html>" + status + "</html>"), title, true, false, true);
        bridge.stopGameSequence(status, title);
      }
    }
  }

  public boolean isGameOver() {
    return gameOver;
  }

  private int getProduction(final GamePlayer gamePlayer) {
    return StreamSupport.stream(getData().getMap().spliterator(), false)
        .filter(Matches.isTerritoryOwnedBy(gamePlayer))
        .mapToInt(TerritoryAttachment::getProduction)
        .sum();
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return null;
  }
}
