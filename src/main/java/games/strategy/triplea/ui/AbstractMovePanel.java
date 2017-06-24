package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.remote.IAbstractMoveDelegate;

public abstract class AbstractMovePanel extends ActionPanel {
  private static final long serialVersionUID = -4153574987414031433L;
  private static final String MOVE_PANEL_CANCEL = "movePanel.cancel";
  private static final Logger logger = Logger.getLogger(MovePanel.class.getName());
  private static final int entryPadding = 15;
  private final TripleAFrame frame;
  private boolean listening = false;
  private final JLabel actionLabel = new JLabel();
  protected MoveDescription moveMessage;
  protected List<UndoableMove> undoableMoves;
  protected AbstractAction doneMove = new AbstractAction("Done") {
    private static final long serialVersionUID = -6497408896615920650L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (doneMoveAction()) {
        moveMessage = null;
        release();
      }
    }
  };
  private final Action DONE_MOVE_ACTION = new WeakAction("Done", doneMove);
  private final Action cancelMove = new AbstractAction("Cancel") {
    private static final long serialVersionUID = -257745862234175428L;

    @Override
    public void actionPerformed(final ActionEvent e) {
      cancelMoveAction();
      if (frame != null) {
        frame.clearStatusMessage();
      }
      this.setEnabled(false);
      CANCEL_MOVE_ACTION.setEnabled(false);
    }
  };

  protected AbstractMovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    this.frame = frame;
    CANCEL_MOVE_ACTION.setEnabled(false);
    undoableMoves = Collections.emptyList();
  }

  /*
   * sub-classes method for done handling
   */
  protected abstract boolean doneMoveAction();

  /*
   * sub-classes method for cancel handling
   */
  protected abstract void cancelMoveAction();

  private final AbstractAction CANCEL_MOVE_ACTION = new WeakAction("Cancel", cancelMove);
  protected AbstractUndoableMovesPanel undoableMovesPanel;
  private IPlayerBridge bridge;

  protected IPlayerBridge getPlayerBridge() {
    return bridge;
  }

  // m_frame methods
  protected final void clearStatusMessage() {
    frame.clearStatusMessage();
  }

  protected final void setStatusErrorMessage(final String message) {
    frame.setStatusErrorMessage(message);
  }

  protected final void setStatusWarningMessage(final String message) {
    frame.setStatusWarningMessage(message);
  }

  protected final boolean getListening() {
    return listening;
  }

  protected final void setMoveMessage(final MoveDescription message) {
    moveMessage = message;
  }

  protected final List<UndoableMove> getUndoableMoves() {
    return undoableMoves;
  }

  protected final void enableCancelButton() {
    CANCEL_MOVE_ACTION.setEnabled(true);
  }

  protected final GameData getGameData() {
    return bridge.getGameData();
  }

  @SuppressWarnings("unchecked")
  private IAbstractMoveDelegate<UndoableMove> getMoveDelegate() {
    return (IAbstractMoveDelegate<UndoableMove>) bridge.getRemoteDelegate();
  }

  protected final void updateMoves() {
    undoableMoves = getMoveDelegate().getMovesMade();
    this.undoableMovesPanel.setMoves(new ArrayList<>(m_undoableMoves));
  }

  public final void cancelMove() {
    CANCEL_MOVE_ACTION.actionPerformed(null);
  }

  public final String undoMove(final int moveIndex) {
    return undoMove(moveIndex, false);
  }

  /**
   * Executes an undo move for any of the units passed in as a parameter.
   *
   * <p>
   * "Cannot undo" Error messages are suppressed if any moves cannot be undone
   * (at least until we come up with a way to deal with "n" reasons for an undo
   * failure rather than just one)
   * </p>
   */
  public void undoMoves(final Set<Unit> units) {
    final Set<UndoableMove> movesToUndo = getMovesToUndo(units, getMoveDelegate().getMovesMade());

    if (movesToUndo.size() == 0) {
      final String error =
          "Could not undo any moves, check that the unit has moved and that you can undo the move normally";
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
      return;
    }

    undoMovesInReverseOrder(movesToUndo);
  }

  private static Set<UndoableMove> getMovesToUndo(final Set<Unit> units, final List<UndoableMove> movesMade) {
    final Set<UndoableMove> movesToUndo = new HashSet<>();

    if (movesMade != null) {
      for (final UndoableMove move : movesMade) {
        if (move != null) {
          if (move.containsAnyOf(units) && move.getcanUndo()) {
            movesToUndo.add(move);
          }
        }
      }
    }
    return movesToUndo;
  }

  /*
   * Undo moves in reverse order, from largest index to smallest. Undo will reorder move index numbers, so going top
   * down avoids this renumbering.
   */
  private void undoMovesInReverseOrder(final Set<UndoableMove> movesToUndo) {
    final List<Integer> moveIndexes = getSortedMoveIndexes(movesToUndo);
    for (int i = moveIndexes.size() - 1; i >= 0; i--) {
      undoMove(moveIndexes.get(i));
    }
  }

  private static List<Integer> getSortedMoveIndexes(final Set<UndoableMove> moves) {
    final List<Integer> moveIndexes = new ArrayList<>();
    for (final UndoableMove move : moves) {
      moveIndexes.add(move.getIndex());
    }
    Collections.sort(moveIndexes);
    return moveIndexes;
  }


  protected final String undoMove(final int moveIndex, final boolean suppressError) {
    // clean up any state we may have
    CANCEL_MOVE_ACTION.actionPerformed(null);
    // undo the move
    final String error = getMoveDelegate().undoMove(moveIndex);
    if (error != null && !suppressError) {
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
    } else {
      updateMoves();
    }
    undoMoveSpecific();
    return error;
  }

  /**
   * sub-classes method for undo handling.
   */
  protected abstract void undoMoveSpecific();

  protected final void cleanUp() {
    SwingUtilities.invokeLater(() -> {
      logger.fine("cleanup");
      if (!listening) {
        throw new IllegalStateException("Not listening");
      }
      listening = false;
      cleanUpSpecific();
      bridge = null;
      CANCEL_MOVE_ACTION.setEnabled(false);
      final JComponent rootPane = getRootPane();
      if (rootPane != null) {
        rootPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null);
      }
      removeAll();
      REFRESH.run();
    });
  }

  /*
   * sub-classes method for clean-up
   */
  protected abstract void cleanUpSpecific();

  @Override
  public final void setActive(final boolean active) {
    super.setActive(active);
    SwingUtilities.invokeLater(() -> CANCEL_MOVE_ACTION.actionPerformed(null));
  }

  protected final void display(final PlayerID id, final String actionLabel) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      removeAll();
      this.actionLabel.setText(id.getName() + actionLabel);
      add(leftBox(this.actionLabel));
      if (setCancelButton()) {
        add(leftBox(new JButton(CANCEL_MOVE_ACTION)));
      }
      add(leftBox(new JButton(DONE_MOVE_ACTION)));
      addAdditionalButtons();
      add(Box.createVerticalStrut(entryPadding));
      add(undoableMovesPanel);
      add(Box.createGlue());
      SwingUtilities.invokeLater(REFRESH);
    });
  }

  protected void addAdditionalButtons() {}

  protected abstract boolean setCancelButton();

  protected static JComponent leftBox(final JComponent c) {
    final Box b = new Box(BoxLayout.X_AXIS);
    b.add(c);
    b.add(Box.createHorizontalGlue());
    return b;
  }

  protected final void setUp(final IPlayerBridge bridge) {
    SwingUtilities.invokeLater(() -> {
      logger.fine("setup");
      setUpSpecific();
      this.bridge = bridge;
      updateMoves();
      if (listening) {
        throw new IllegalStateException("Not listening");
      }
      listening = true;
      if (getRootPane() != null) {
        final String key = MOVE_PANEL_CANCEL;
        getRootPane().getActionMap().put(key, CANCEL_MOVE_ACTION);
        getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);
      }
    });
  }

  /*
   * sub-classes method for set-up
   */
  protected abstract void setUpSpecific();

  protected void clearDependencies() {
    // used by some subclasses
  }

  final MoveDescription waitForMove(final IPlayerBridge bridge) {
    setUp(bridge);
    waitForRelease();
    cleanUp();
    final MoveDescription rVal = moveMessage;
    moveMessage = null;
    clearDependencies();
    return rVal;
  }
}
