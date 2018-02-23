package games.strategy.triplea.delegate;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.Event;
import games.strategy.engine.history.EventChild;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Step;
import games.strategy.triplea.Constants;

public abstract class BaseEditDelegate extends BasePersistentDelegate {
  private static final String EDITMODE_ON = "Turning on Edit Mode";
  private static final String EDITMODE_OFF = "Turning off Edit Mode";

  @Override
  public void setDelegateBridgeAndPlayer(final IDelegateBridge delegateBridge) {
    super.setDelegateBridgeAndPlayer(new GameDelegateBridge(delegateBridge));
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void end() {}

  @Override
  public boolean delegateCurrentlyRequiresUserInput() {
    return true;
  }

  public static boolean getEditMode(final GameData data) {
    final Object editMode = data.getProperties().get(Constants.EDIT_MODE);
    return (editMode != null) && (editMode instanceof Boolean) && (boolean) editMode;
  }

  public boolean getEditMode() {
    return getEditMode(getData());
  }

  private String checkPlayerId() {
    final IRemotePlayer remotePlayer = getRemotePlayer();
    if (!bridge.getPlayerId().equals(remotePlayer.getPlayerId())) {
      return "Edit actions can only be performed during players turn";
    }
    return null;
  }

  String checkEditMode() {
    final String result = checkPlayerId();
    if (null != result) {
      return result;
    }
    if (!getEditMode(getData())) {
      return "Edit mode is not enabled";
    }
    return null;
  }

  public String setEditMode(final boolean editMode) {
    final IRemotePlayer remotePlayer = getRemotePlayer();
    if (!bridge.getPlayerId().equals(remotePlayer.getPlayerId())) {
      return "Edit Mode can only be toggled during players turn";
    }
    logEvent((editMode ? EDITMODE_ON : EDITMODE_OFF), null);
    bridge.addChange(ChangeFactory.setProperty(Constants.EDIT_MODE, editMode, getData()));
    return null;
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
