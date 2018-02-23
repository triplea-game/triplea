package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.remote.IAbstractMoveDelegate;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;

public abstract class AbstractMovePanel extends ActionPanel {
  private static final long serialVersionUID = -4153574987414031433L;
  private static final int entryPadding = 15;
  private final TripleAFrame frame;
  private boolean listening = false;
  private final JLabel actionLabel = new JLabel();
  private MoveDescription moveMessage;
  private List<UndoableMove> undoableMoves;


  private final JButton cancelMoveButton = JButtonBuilder.builder()
      .title("Cancel")
      .actionListener(this::cancelMove)
      .build();

  AbstractMovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    this.frame = frame;
    disableCancelButton();
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


  AbstractUndoableMovesPanel undoableMovesPanel;
  private IPlayerBridge bridge;

  IPlayerBridge getPlayerBridge() {
    return bridge;
  }

  // m_frame methods
  final void clearStatusMessage() {
    frame.clearStatusMessage();
  }

  final void setStatusErrorMessage(final String message) {
    frame.setStatusErrorMessage(message);
  }

  final void setStatusWarningMessage(final String message) {
    frame.setStatusWarningMessage(message);
  }

  final boolean getListening() {
    return listening;
  }

  final void setMoveMessage(final MoveDescription message) {
    moveMessage = message;
  }

  final List<UndoableMove> getUndoableMoves() {
    return undoableMoves;
  }

  final void enableCancelButton() {
    cancelMoveButton.setEnabled(true);
  }

  private void disableCancelButton() {
    cancelMoveButton.setEnabled(false);
  }

  protected final GameData getGameData() {
    return bridge.getGameData();
  }

  @SuppressWarnings("unchecked")
  private IAbstractMoveDelegate<UndoableMove> getMoveDelegate() {
    return (IAbstractMoveDelegate<UndoableMove>) bridge.getRemoteDelegate();
  }

  private void updateMoves() {
    undoableMoves = getMoveDelegate().getMovesMade();
    this.undoableMovesPanel.setMoves(new ArrayList<>(undoableMoves));
  }

  final void cancelMove() {
    cancelMoveAction();
    if (frame != null) {
      frame.clearStatusMessage();
    }
    this.setEnabled(false);
    disableCancelButton();
  }

  final String undoMove(final int moveIndex) {
    return undoMove(moveIndex, false);
  }

  final String undoMove(final int moveIndex, final boolean suppressError) {
    // clean up any state we may have
    cancelMove();
    // undo the move
    final String error = getMoveDelegate().undoMove(moveIndex);
    if ((error != null) && !suppressError) {
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
    } else {
      updateMoves();
    }
    undoMoveSpecific();
    return error;
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
  void undoMoves(final Set<Unit> units) {
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

  /**
   * sub-classes method for undo handling.
   */
  protected abstract void undoMoveSpecific();

  final void cleanUp() {
    SwingUtilities.invokeLater(() -> {
      if (!listening) {
        throw new IllegalStateException("Not listening");
      }
      listening = false;
      cleanUpSpecific();
      bridge = null;
      disableCancelButton();
      removeAll();
      refresh.run();
    });
  }

  /*
   * sub-classes method for clean-up
   */
  protected abstract void cleanUpSpecific();

  @Override
  public final void setActive(final boolean active) {
    super.setActive(active);
    SwingUtilities.invokeLater(this::cancelMove);
  }

  protected final void display(final PlayerID id, final String actionLabel) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      removeAll();
      this.actionLabel.setText(id.getName() + actionLabel);
      add(leftBox(this.actionLabel));
      if (setCancelButton()) {
        add(leftBox(cancelMoveButton));
      }
      add(leftBox(new JButton(SwingAction.of("Done", e -> {
        if (doneMoveAction()) {
          moveMessage = null;
          release();
        }
      }))));
      addAdditionalButtons();
      add(Box.createVerticalStrut(entryPadding));
      add(undoableMovesPanel);
      add(Box.createGlue());
      SwingUtilities.invokeLater(refresh);
    });
  }

  protected void addAdditionalButtons() {}

  protected abstract boolean setCancelButton();

  static JComponent leftBox(final JComponent c) {
    final Box b = new Box(BoxLayout.X_AXIS);
    b.add(c);
    b.add(Box.createHorizontalGlue());
    return b;
  }

  protected final void setUp(final IPlayerBridge bridge) {
    SwingUtilities.invokeLater(() -> {
      setUpSpecific();
      this.bridge = bridge;
      updateMoves();
      if (listening) {
        throw new IllegalStateException("Not listening");
      }
      listening = true;
      if (getRootPane() != null) {
        SwingComponents.addEscapeKeyListener(this, this::cancelMove);
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
    final MoveDescription returnValue = moveMessage;
    moveMessage = null;
    clearDependencies();
    return returnValue;
  }
}
