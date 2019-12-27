package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MoveBatcher implements an algorithm that batches AI moves together, reducing the number of
 * distinct moves the AI does (and thus the visual delay) by combining moves together.
 */
public class MoveBatcher {
  private final ArrayList<ArrayList<MoveDescription>> moveSequences = new ArrayList<>();

  /**
   * Starts a new sequence. This must be called before calling any add*() methods. A sequence
   * indicates a logical dependency relationship between the moves in that sequence.
   */
  public void newSequence() {
    moveSequences.add(new ArrayList<>());
  }

  /**
   * Adds a transport load move.
   *
   * @param unit The unit to move.
   * @param route The route to move on.
   * @param transport The transport to load.
   */
  public void addTransportLoad(final Unit unit, final Route route, final Unit transport) {
    addMove(new MoveDescription(List.of(unit), route, Map.of(unit, transport)));
  }

  /**
   * Adds a move for the specified units. The units list must not be changed after adding the move.
   *
   * @param units The units to move.
   * @param route The route to move on.
   */
  public void addMove(final List<Unit> units, final Route route) {
    addMove(new MoveDescription(units, route));
  }

  private void addMove(final MoveDescription newMove) {
    final ArrayList<MoveDescription> sequence = moveSequences.get(moveSequences.size() - 1);
    if (!sequence.isEmpty()) {
      final MoveDescription lastMove = sequence.get(sequence.size() - 1);
      if (canMergeMoves(lastMove, newMove)) {
        sequence.set(sequence.size() - 1, mergeMoves(lastMove, newMove));
        return;
      }
    }
    sequence.add(newMove);
  }

  /**
   * Batches and returns the list of moves.
   *
   * @return the list of moves.
   */
  public List<MoveDescription> batchMoves() {
    final var moves = new ArrayList<MoveDescription>();
    int i = 0;
    for (final var sequence : moveSequences) {
      if (!sequence.isEmpty()) {
        mergeSequences(sequence, moveSequences.subList(i + 1, moveSequences.size()));
        moves.addAll(sequence);
      }
      i++;
    }
    return moves;
  }

  /**
   * Attempts to merge a sequence of moves with other sequences. Any sequence merged into this
   * sequence will have its moves cleared.
   *
   * @param sequence The sequence to merge other sequences into.
   * @param sequences The sequences to try to merge into the sequence.
   */
  private static void mergeSequences(
      final ArrayList<MoveDescription> sequence, final List<ArrayList<MoveDescription>> sequences) {
    for (final var otherSequence : sequences) {
      boolean merge = (otherSequence.size() == sequence.size());
      for (int i = 0; merge && i < sequence.size(); i++) {
        merge = canMergeMoves(sequence.get(i), otherSequence.get(i));
      }
      if (!merge) {
        continue;
      }
      for (int i = 0; i < sequence.size(); i++) {
        sequence.set(i, mergeMoves(sequence.get(i), otherSequence.get(i)));
      }
      otherSequence.clear();
    }
  }

  private static boolean canMergeMoves(final MoveDescription move1, final MoveDescription move2) {
    return isTransportLoad(move1) == isTransportLoad(move2)
        && move1.getRoute().equals(move2.getRoute());
  }

  private static boolean isTransportLoad(final MoveDescription move) {
    return !move.getUnitsToTransports().isEmpty();
  }

  // Merges the two moves. Caller must ensure canMergeMoves() is true before calling.
  private static MoveDescription mergeMoves(
      final MoveDescription move1, final MoveDescription move2) {
    if (!canMergeMoves(move1, move2)) {
      throw new IllegalStateException("can't merge moves: " + move1 + " and " + move2);
    }
    final var units = new ArrayList<>(move1.getUnits());
    units.addAll(move2.getUnits());
    final var unitsToTransports = new HashMap<>(move1.getUnitsToTransports());
    unitsToTransports.putAll(move2.getUnitsToTransports());
    return new MoveDescription(units, move1.getRoute(), unitsToTransports);
  }
}
