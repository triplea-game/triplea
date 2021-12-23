package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.engine.player.Player;
import games.strategy.triplea.Constants;

/**
 * Superclass for all logic that runs in edit mode.
 *
 * <p>TODO: Merge this class with EditDelegate, as it has no other subclasses.
 */
public abstract class BaseEditDelegate extends BasePersistentDelegate {
  private static final String EDITMODE_ON = "Turning on Edit Mode";
  private static final String EDITMODE_OFF = "Turning off Edit Mode";

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }

  public static boolean getEditMode(final GameProperties properties) {
    final Object editMode = properties.get(Constants.EDIT_MODE);
    return editMode instanceof Boolean && (boolean) editMode;
  }

  public boolean getEditMode() {
    return getEditMode(getData().getProperties());
  }

  private String checkPlayerId() {
    final Player remotePlayer = bridge.getRemotePlayer();
    if (!bridge.getGamePlayer().equals(remotePlayer.getGamePlayer())) {
      return "Edit actions can only be performed during players turn";
    }
    return null;
  }

  String checkEditMode() {
    final String result = checkPlayerId();
    if (null != result) {
      return result;
    }
    if (!getEditMode(getData().getProperties())) {
      return "Edit mode is not enabled";
    }
    return null;
  }

  public void setEditMode(final boolean editMode) {
    final Player remotePlayer = bridge.getRemotePlayer();
    if (!bridge.getGamePlayer().equals(remotePlayer.getGamePlayer())) {
      return;
    }
    logEvent((editMode ? EDITMODE_ON : EDITMODE_OFF), null);
    bridge.addChange(ChangeFactory.setProperty(Constants.EDIT_MODE, editMode, getData()));
  }

  public String addComment(final String message) {
    final String result = checkPlayerId();
    if (result != null) {
      return result;
    }
    logEvent("COMMENT: " + message, null);
    return null;
  }

  // We don't know the current context, so we need to figure
  // out whether it makes more sense to log a new event or a child.
  // If any child events came before us, then we'll log a child event.
  // Otherwise, we'll log a new event.
  void logEvent(final String message, final Object renderingObject) {
    // find last event node
    final GameData gameData = getData();
    gameData.acquireReadLock();
    boolean foundChild = false;
    try {
      HistoryNode curNode = gameData.getHistory().getLastNode();
      while (!(curNode instanceof Step) && !(curNode instanceof Event)) {
        if (curNode instanceof EventChild) {
          foundChild = true;
          break;
        }
        curNode = (HistoryNode) curNode.getPreviousNode();
      }
    } finally {
      gameData.releaseReadLock();
    }
    if (foundChild) {
      bridge.getHistoryWriter().addChildToEvent(message, renderingObject);
    } else {
      bridge.getHistoryWriter().startEvent(message, renderingObject);
    }
  }
}
