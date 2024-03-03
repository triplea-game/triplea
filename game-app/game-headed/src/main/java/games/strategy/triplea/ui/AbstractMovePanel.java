package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.GameState;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Unit;
import games.strategy.engine.player.PlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.remote.IAbstractMoveDelegate;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

@Slf4j
public abstract class AbstractMovePanel extends ActionPanel {
  private static final long serialVersionUID = -4153574987414031433L;

  protected AbstractUndoableMovesPanel undoableMovesPanel;
  private final TripleAFrame frame;
  private boolean listening = false;
  private MoveDescription moveMessage;
  private List<UndoableMove> undoableMoves;

  @Getter(AccessLevel.PACKAGE)
  private PlayerBridge playerBridge;

  private final JButton cancelMoveButton =
      new JButtonBuilder().title("Cancel").actionListener(this::cancelMove).build();
  private final JButton doneButton = createDoneButton();
  private final JButton undoAllButton =
      new JButtonBuilder().title("Undo All").actionListener(this::undoAll).build();

  protected AbstractMovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    this.frame = frame;
    disableCancelButton();
    undoableMoves = List.of();
  }

  @Override
  public void performDone() {
    if (doneMoveAction()) {
      moveMessage = null;
      release();
    }
  }

  protected abstract Component getUnitScrollerPanel();

  /*
   * sub-classes method for done handling
   */
  protected abstract boolean doneMoveAction();

  /*
   * sub-classes method for cancel handling
   */
  protected abstract void cancelMoveAction();

  // frame methods

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
    cancelMoveButton.setEnabled(true);
  }

  private void disableCancelButton() {
    cancelMoveButton.setEnabled(false);
  }

  protected final GameState getGameData() {
    return playerBridge.getGameData();
  }

  @SuppressWarnings("unchecked")
  private IAbstractMoveDelegate<UndoableMove> getMoveDelegate() {
    return (IAbstractMoveDelegate<UndoableMove>) playerBridge.getRemoteDelegate();
  }

  private void updateMoves() {
    undoableMoves = getMoveDelegate().getMovesMade();
    this.undoableMovesPanel.setMoves(new ArrayList<>(undoableMoves));
    undoAllButton.setEnabled(!undoableMoves.isEmpty());
  }

  protected final void cancelMove() {
    cancelMoveAction();
    if (frame != null) {
      frame.clearStatusMessage();
    }
    this.setEnabled(false);
    disableCancelButton();
  }

  final @Nullable String undoMove(final int moveIndex) {
    return undoMove(moveIndex, false);
  }

  final @Nullable String undoMove(final int moveIndex, final boolean suppressError) {
    // clean up any state we may have
    cancelMove();
    // undo the move
    final String error = getMoveDelegate().undoMove(moveIndex);
    if (error != null && !suppressError) {
      JOptionPane.showMessageDialog(
          getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
    } else {
      updateMoves();
    }
    undoMoveSpecific();
    return error;
  }

  private void undoAll() {
    final int moveCount = getUndoableMoves().size();
    final boolean suppressErrorMsgToUser = true;
    for (int i = moveCount - 1; i >= 0; i--) {
      undoMove(i, suppressErrorMsgToUser);
    }
  }

  /**
   * Executes an undo move for any of the units passed in as a parameter.
   *
   * <p>"Cannot undo" Error messages are suppressed if any moves cannot be undone (at least until we
   * come up with a way to deal with "n" reasons for an undo failure rather than just one)
   */
  void undoMoves(final Set<Unit> units) {
    final Set<UndoableMove> movesToUndo = getMovesToUndo(units, getMoveDelegate().getMovesMade());

    if (movesToUndo.isEmpty()) {
      final String error =
          "Could not undo any moves, check that the unit has moved and that you "
              + "can undo the move normally";
      JOptionPane.showMessageDialog(
          getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
      return;
    }

    undoMovesInReverseOrder(movesToUndo);
  }

  private static Set<UndoableMove> getMovesToUndo(
      final Set<Unit> units, final List<UndoableMove> movesMade) {
    final Set<UndoableMove> movesToUndo = new HashSet<>();

    if (movesMade != null) {
      for (final UndoableMove move : movesMade) {
        if (move != null && units.stream().anyMatch(move::containsUnit) && move.getCanUndo()) {
          movesToUndo.add(move);
        }
      }
    }
    return movesToUndo;
  }

  /*
   * Undo moves in reverse order, from largest index to smallest. Undo will reorder
   * move index numbers, so going top down avoids this renumbering.
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

  /** sub-classes method for undo handling. */
  protected abstract void undoMoveSpecific();

  final void cleanUp() {
    SwingUtilities.invokeLater(
        () -> {
          if (!listening) {
            throw new IllegalStateException("Not listening");
          }
          listening = false;
          cleanUpSpecific();
          playerBridge = null;
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

  protected final void display(final GamePlayer gamePlayer, final String actionLabel) {
    super.display(gamePlayer);
    SwingUtilities.invokeLater(
        () -> {
          removeAll();
          add(movedUnitsPanel(gamePlayer, actionLabel));
          refresh.run();
        });
  }

  protected List<Component> getAdditionalButtons() {
    return List.of();
  }

  protected abstract boolean setCancelButton();

  private JPanel movedUnitsPanel(final GamePlayer gamePlayer, final String actionLabel) {
    final JPanel movedUnitsPanel = new JPanel();
    movedUnitsPanel.setLayout(new BoxLayout(movedUnitsPanel, BoxLayout.Y_AXIS));

    this.actionLabel.setText(gamePlayer.getName() + actionLabel);
    movedUnitsPanel.add(SwingComponents.leftBox(this.actionLabel));

    final JPanel buttonsPanel = new JPanel();
    if (setCancelButton()) {
      buttonsPanel.add(cancelMoveButton);
    }
    buttonsPanel.add(doneButton);
    buttonsPanel.add(undoAllButton);
    movedUnitsPanel.add(buttonsPanel);

    // Add any additional buttons on their own lines, since their text may be
    // arbitrarily long.
    getAdditionalButtons().forEach(movedUnitsPanel::add);
    movedUnitsPanel.add(undoableMovesPanel);
    movedUnitsPanel.add(Box.createGlue());
    return movedUnitsPanel;
  }

  protected final void setUp(final PlayerBridge bridge) {
    SwingUtilities.invokeLater(
        () -> {
          setUpSpecific();
          this.playerBridge = bridge;
          AsyncRunner.runAsync(this::updateMoves)
              .exceptionally(e -> log.warn("Failed to receive move updates", e));

          if (listening) {
            throw new IllegalStateException("Not listening");
          }
          listening = true;
          if (getRootPane() != null) {
            SwingKeyBinding.addKeyBinding(getRootPane(), KeyCode.ESCAPE, this::cancelMove);
          }
        });
  }

  /*
   * sub-classes method for set-up
   */
  protected abstract void setUpSpecific();

  public final MoveDescription waitForMove(final PlayerBridge bridge) {
    setUp(bridge);
    waitForRelease();
    cleanUp();
    final MoveDescription returnValue = moveMessage;
    moveMessage = null;
    return returnValue;
  }
}
