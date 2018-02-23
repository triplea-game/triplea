package games.strategy.triplea.delegate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import javax.swing.JOptionPane;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.framework.headlessGameServer.HeadlessGameServer;
import games.strategy.engine.message.IRemote;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.AbstractTriggerAttachment;
import games.strategy.triplea.attachments.ICondition;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.TriggerAttachment;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.LocalizeHtml;

/**
 * A delegate used to check for end of game conditions.
 */
@MapSupport
public class EndRoundDelegate extends BaseTripleADelegate {
  private boolean gameOver = false;
  private Collection<PlayerID> winners = new ArrayList<>();

  /** Creates a new instance of EndRoundDelegate. */
  public EndRoundDelegate() {}

  @Override
  public void start() {
    super.start();
    if (gameOver) {
      return;
    }
    String victoryMessage;
    final GameData data = getData();
    if (isPacificTheater()) {
      final PlayerID japanese = data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_JAPANESE);
      final PlayerAttachment pa = PlayerAttachment.get(japanese);
      if ((pa != null) && (pa.getVps() >= 22)) {
        victoryMessage = "Axis achieve VP victory";
        bridge.getHistoryWriter().startEvent(victoryMessage);
        final Collection<PlayerID> winners = data.getAllianceTracker()
            .getPlayersInAlliance(data.getAllianceTracker().getAlliancesPlayerIsIn(japanese).iterator().next());
        signalGameOver(victoryMessage, winners, bridge);
      }
    }
    // Check for Winning conditions
    if (isTotalVictory()) { // Check for Win by Victory Cities
      victoryMessage = " achieve TOTAL VICTORY with ";
      checkVictoryCities(bridge, victoryMessage, " Total Victory VCs");
    }
    if (isHonorableSurrender()) {
      victoryMessage = " achieve an HONORABLE VICTORY with ";
      checkVictoryCities(bridge, victoryMessage, " Honorable Victory VCs");
    }
    if (isProjectionOfPower()) {
      victoryMessage = " achieve victory through a PROJECTION OF POWER with ";
      checkVictoryCities(bridge, victoryMessage, " Projection of Power VCs");
    }
    if (isEconomicVictory()) { // Check for regular economic victory
      for (final String allianceName : data.getAllianceTracker().getAlliances()) {
        final int victoryAmount = getEconomicVictoryAmount(data, allianceName);
        final Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
        int teamProd = 0;
        for (final PlayerID player : teamMembers) {
          teamProd += getProduction(player);
          if (teamProd >= victoryAmount) {
            victoryMessage = allianceName + " achieve economic victory";
            bridge.getHistoryWriter().startEvent(victoryMessage);
            final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance(allianceName);
            // Added this to end the game on victory conditions
            signalGameOver(victoryMessage, winners, bridge);
          }
        }
      }
    }
    // now check for generic trigger based victories
    if (isTriggeredVictory()) {
      // First set up a match for what we want to have fire as a default in this delegate. List out as a composite match
      // OR.
      // use 'null, null' because this is the Default firing location for any trigger that does NOT have 'when' set.
      final Predicate<TriggerAttachment> endRoundDelegateTriggerMatch = AbstractTriggerAttachment.availableUses
          .and(AbstractTriggerAttachment.whenOrDefaultMatch(null, null))
          .and(TriggerAttachment.activateTriggerMatch().or(TriggerAttachment.victoryMatch()));
      // get all possible triggers based on this match.
      final HashSet<TriggerAttachment> toFirePossible = TriggerAttachment.collectForAllTriggersMatching(
          new HashSet<>(data.getPlayerList().getPlayers()), endRoundDelegateTriggerMatch);
      if (!toFirePossible.isEmpty()) {
        // get all conditions possibly needed by these triggers, and then test them.
        final HashMap<ICondition, Boolean> testedConditions =
            TriggerAttachment.collectTestsForAllTriggers(toFirePossible, bridge);
        // get all triggers that are satisfied based on the tested conditions.
        final Set<TriggerAttachment> toFireTestedAndSatisfied = new HashSet<>(
            CollectionUtils.getMatches(toFirePossible, AbstractTriggerAttachment.isSatisfiedMatch(testedConditions)));
        // now list out individual types to fire, once for each of the matches above.
        TriggerAttachment.triggerActivateTriggerOther(testedConditions, toFireTestedAndSatisfied, bridge, null, null,
            true, true, true, true);
        // will call
        TriggerAttachment.triggerVictory(toFireTestedAndSatisfied, bridge, null, null, true, true, true, true);
        // signalGameOver itself
      }
    }
    if (isWW2V2() || isWW2V3()) {
      return;
    }
    final PlayerList playerList = data.getPlayerList();
    // now test older maps that only use these 5 players, to see if someone has won
    final PlayerID russians = playerList.getPlayerId(Constants.PLAYER_NAME_RUSSIANS);
    final PlayerID germans = playerList.getPlayerId(Constants.PLAYER_NAME_GERMANS);
    final PlayerID british = playerList.getPlayerId(Constants.PLAYER_NAME_BRITISH);
    final PlayerID japanese = playerList.getPlayerId(Constants.PLAYER_NAME_JAPANESE);
    final PlayerID americans = playerList.getPlayerId(Constants.PLAYER_NAME_AMERICANS);
    if ((germans == null) || (russians == null) || (british == null) || (japanese == null) || (americans == null)
        || (playerList.size() > 5)) {
      return;
    }
    // Quick check to see who still owns their own capital
    final boolean russia =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(russians, data).getOwner().equals(russians);
    final boolean germany =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(germans, data).getOwner().equals(germans);
    final boolean britain =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(british, data).getOwner().equals(british);
    final boolean japan =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(japanese, data).getOwner().equals(japanese);
    final boolean america =
        TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(americans, data).getOwner().equals(americans);
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
    if (germany && japan && (count >= 2)) {
      bridge.getHistoryWriter().startEvent("Axis" + victoryMessage);
      final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance("Axis");
      signalGameOver("Axis" + victoryMessage, winners, bridge);
    }
    if (russia && !germany && britain && !japan && america) {
      bridge.getHistoryWriter().startEvent("Allies" + victoryMessage);
      final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance("Allies");
      signalGameOver("Allies" + victoryMessage, winners, bridge);
    }
  }

  @Override
  public void end() {
    super.end();
    final GameData data = getData();
    if (Properties.getTriggers(data)) {
      final CompositeChange change = new CompositeChange();
      for (final PlayerID player : data.getPlayerList().getPlayers()) {
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
    state.m_gameOver = gameOver;
    state.m_winners = winners;
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final EndRoundExtendedDelegateState s = (EndRoundExtendedDelegateState) state;
    super.loadState(s.superState);
    gameOver = s.m_gameOver;
    winners = s.m_winners;
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return false;
  }

  private void checkVictoryCities(final IDelegateBridge bridge, final String victoryMessage,
      final String victoryType) {
    final GameData data = bridge.getData();

    final Collection<Territory> territories = data.getMap().getTerritories();
    for (final String allianceName : data.getAllianceTracker().getAlliances()) {
      final int vcAmount = getVcAmount(data, allianceName, victoryType);
      final Set<PlayerID> teamMembers = data.getAllianceTracker().getPlayersInAlliance(allianceName);
      int teamVCs = 0;
      for (final Territory t : territories) {
        if (Matches.isTerritoryOwnedBy(teamMembers).test(t)) {
          final TerritoryAttachment ta = TerritoryAttachment.get(t);
          if (ta != null) {
            teamVCs += ta.getVictoryCity();
          }
        }
      }
      if (teamVCs >= vcAmount) {
        bridge.getHistoryWriter().startEvent(allianceName + victoryMessage + vcAmount + " Victory Cities!");
        final Collection<PlayerID> winners = data.getAllianceTracker().getPlayersInAlliance(allianceName);
        // Added this to end the game on victory conditions
        signalGameOver(allianceName + victoryMessage + vcAmount + " Victory Cities!", winners, bridge);
      }
    }
  }

  private static int getEconomicVictoryAmount(final GameData data, final String alliance) {
    return data.getProperties().get(alliance + " Economic Victory", 200);
  }

  private static int getVcAmount(final GameData data, final String alliance, final String type) {
    int defaultVc = 20;
    if (type.equals(" Total Victory VCs")) {
      defaultVc = 18;
    } else if (type.equals(" Honorable Victory VCs")) {
      defaultVc = 15;
    } else if (type.equals(" Projection of Power VCs")) {
      defaultVc = 13;
    }
    return data.getProperties().get((alliance + type), defaultVc);
  }

  /**
   * Notify all players that the game is over.
   *
   * @param status
   *        the "game over" text to be displayed to each user.
   */
  public void signalGameOver(final String status, final Collection<PlayerID> winners, final IDelegateBridge bridge) {
    // TO NOT USE playerBridge, because it might be null here! use aBridge instead.
    // If the game is over, we need to be able to alert all UIs to that fact.
    // The display object can send a message to all UIs.
    if (!gameOver) {
      gameOver = true;
      this.winners = winners;
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_GAME_WON,
          (((this.winners != null) && !this.winners.isEmpty()) ? this.winners.iterator().next()
              : PlayerID.NULL_PLAYERID));
      // send a message to everyone's screen except the HOST (there is no 'current player' for the end round delegate)
      final String title = "Victory Achieved"
          + (winners.isEmpty() ? "" : " by " + MyFormatter.defaultNamedToTextList(winners, ", ", false));
      // we send the bridge, because we can call this method from outside this delegate, which
      // means our local copy of playerBridge could be null.
      getDisplay(bridge).reportMessageToAll(("<html>" + status + "</html>"), title, true, false, true);
      final boolean stopGame;
      if (HeadlessGameServer.headless()) {
        // a terrible dirty hack, but I can't think of a better way to do it right now. If we are headless, end the
        // game.
        stopGame = true;
      } else {
        // now tell the HOST, and see if they want to continue the game.
        String displayMessage = LocalizeHtml.localizeImgLinksInHtml(status);
        if (displayMessage.endsWith("</body>")) {
          displayMessage = displayMessage.substring(0, displayMessage.length() - "</body>".length())
              + "</br><p>Do you want to continue?</p></body>";
        } else {
          displayMessage = displayMessage + "</br><p>Do you want to continue?</p>";
        }
        // this is currently the ONLY instance of JOptionPane that is allowed outside of the UI classes. maybe there is
        // a better way?
        stopGame = JOptionPane.OK_OPTION != EventThreadJOptionPane.showConfirmDialog(null,
            "<html>" + displayMessage + "</html>", "Continue Game?  (" + title + ")", JOptionPane.YES_NO_OPTION);
      }
      if (stopGame) {
        bridge.stopGameSequence();
      }
    }
  }

  /**
   * if null, the game is not over yet.
   */
  public Collection<PlayerID> getWinners() {
    if (!gameOver) {
      return null;
    }
    return winners;
  }

  private boolean isWW2V2() {
    return Properties.getWW2V2(getData());
  }

  private boolean isWW2V3() {
    return Properties.getWW2V3(getData());
  }

  private boolean isPacificTheater() {
    return Properties.getPacificTheater(getData());
  }

  private boolean isTotalVictory() {
    return Properties.getTotalVictory(getData());
  }

  private boolean isHonorableSurrender() {
    return Properties.getHonorableSurrender(getData());
  }

  private boolean isProjectionOfPower() {
    return Properties.getProjectionOfPower(getData());
  }

  private boolean isEconomicVictory() {
    return Properties.getEconomicVictory(getData());
  }

  private boolean isTriggeredVictory() {
    return Properties.getTriggeredVictory(getData());
  }

  private int getProduction(final PlayerID id) {
    return StreamSupport.stream(getData().getMap().spliterator(), false)
        .filter(current -> current.getOwner().equals(id))
        .mapToInt(TerritoryAttachment::getProduction)
        .sum();
  }

  @Override
  public Class<? extends IRemote> getRemoteType() {
    return null;
  }
}
