package games.puzzle.slidingtiles.delegate;

import java.io.Serializable;

import games.puzzle.slidingtiles.attachments.Tile;
import games.strategy.common.delegate.AbstractDelegate;
import games.strategy.engine.data.Change;
import games.strategy.engine.data.ChangeFactory;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.message.IRemote;
import games.strategy.grid.delegate.remote.IGridPlayDelegate;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.grid.ui.display.IGridGameDisplay;

/**
 * Responsible for performing a move in a game of n-puzzle.
 */
public class PlayDelegate extends AbstractDelegate implements IGridPlayDelegate {
  private GameMap map;

  /**
   * Called before the delegate will run.
   */
  @Override
  public void start() {
    super.start();
    map = getData().getMap();
  }

  @Override
  public void end() {
    super.end();
  }

  @Override
  public Serializable saveState() {
    final SlidingTilesPlayExtendedDelegateState state = new SlidingTilesPlayExtendedDelegateState();
    state.superState = super.saveState();
    // add other variables to state here:
    return state;
  }

  @Override
  public void loadState(final Serializable state) {
    final SlidingTilesPlayExtendedDelegateState s = (SlidingTilesPlayExtendedDelegateState) state;
    super.loadState(s.superState);
    // load other variables from state here:
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }

  /**
   * Attempt to play.
   *
   * @param play
   *        <code>Territory</code> where the play should occur
   */
  @Override
  public String play(final IGridPlayData play) {
    final Territory from = play.getStart();
    Territory to = play.getEnd();
    if (from.equals(to)) {
      final Tile fromTile = (Tile) from.getAttachment("tile");
      if (fromTile != null && fromTile.getValue() != 0) {
        final Territory blank = getBlankNeighbor(map, from);
        if (blank == null) {
          return "Invalid move";
        } else {
          to = blank;
        }
      }
    } else {
      final String error = isValidPlay(from, to);
      if (error != null) {
        return error;
      }
    }
    performPlay(from, to, m_player);
    return null;
  }

  @Override
  public void signalStatus(final String status) {
    final IGridGameDisplay display = (IGridGameDisplay) m_bridge.getDisplayChannelBroadcaster();
    display.setStatus(status);
  }

  public static Territory getBlankNeighbor(final GameMap map, final Territory t) {
    for (final Territory neighbor : map.getNeighbors(t)) {
      final Tile neighborTile = (Tile) neighbor.getAttachment("tile");
      if (neighborTile != null && neighborTile.getValue() == 0) {
        return neighbor;
      }
    }
    return null;
  }

  /**
   * Check to see if a play is valid.
   *
   * @param play
   *        <code>Territory</code> where the play should occur
   */
  private String isValidPlay(final Territory from, final Territory to) {
    final int startValue = ((Tile) from.getAttachment("tile")).getValue();
    final int destValue = ((Tile) to.getAttachment("tile")).getValue();
    if (startValue != 0 && destValue == 0) {
      return null;
    } else {
      return "Move does not swap a tile with the blank square";
      /*
       * if (territory.getOwner().equals(PlayerID.NULL_PLAYERID))
       * return null;
       * else
       * return "Square is not empty";
       */
      // return "Playing not yet implemented.";
    }
  }

  /**
   * Perform a play.
   *
   * @param play
   *        <code>Territory</code> where the play should occur
   */
  private void performPlay(final Territory from, final Territory to, final PlayerID player) {
    final String transcriptText = player.getName() + " moved tile from " + from.getName() + " to " + to.getName();
    m_bridge.getHistoryWriter().startEvent(transcriptText);
    swap(m_bridge, from, to);
  }

  static void swap(final IDelegateBridge bridge, final Territory from, final Territory to) {
    final Tile fromAttachment = (Tile) from.getAttachment("tile");
    final Tile toAttachment = (Tile) to.getAttachment("tile");
    final int fromValue = fromAttachment.getValue();
    final int toValue = toAttachment.getValue();
    final Change fromChange =
        ChangeFactory.attachmentPropertyChange(fromAttachment, Integer.toString(toValue), "value");
    final Change toChange = ChangeFactory.attachmentPropertyChange(toAttachment, Integer.toString(fromValue), "value");
    final CompositeChange change = new CompositeChange();
    change.add(fromChange);
    change.add(toChange);
    bridge.addChange(change);
    final IGridGameDisplay display = (IGridGameDisplay) bridge.getDisplayChannelBroadcaster();
    display.refreshTerritories(null);
    // return change;
  }

  /**
   * If this class implements an interface which inherits from IRemote, returns the class of that interface.
   * Otherwise, returns null.
   */
  @Override
  public Class<? extends IRemote> getRemoteType() {
    // This class implements IPlayDelegate, which inherits from IRemote.
    return IGridPlayDelegate.class;
  }
}


class SlidingTilesPlayExtendedDelegateState implements Serializable {
  private static final long serialVersionUID = 2850777141854664044L;
  Serializable superState;
  // add other variables here:
}
