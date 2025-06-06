package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** A contiguous sequence of {@link GameStep}s within a single game round. */
@Slf4j
public class GameSequence extends GameDataComponent implements Iterable<GameStep> {
  private static final long serialVersionUID = 6354618406598578287L;

  @Getter private final List<GameStep> steps = new ArrayList<>();
  private int currentIndex;
  private int round = 1;
  @Getter private int roundOffset = 0;

  public GameSequence(final GameData data) {
    super(data);
  }

  /**
   * Only used when we are trying to export the data to a save game, and we need to change the round
   * and step to something other than the current round and step (because we are creating a save
   * game at a certain point in history, for example).
   */
  public synchronized void setRoundAndStep(
      final int currentRound, final String stepDisplayName, final @Nullable GamePlayer player) {
    round = currentRound;
    boolean found = false;
    for (int i = 0; i < steps.size(); i++) {
      final GameStep step = steps.get(i);
      if (step != null
          && step.getDisplayName().equalsIgnoreCase(stepDisplayName)
          && ((player == null && step.getPlayerId() == null)
              || (player != null && player.equals(step.getPlayerId())))) {
        currentIndex = i;
        found = true;
        break;
      }
    }
    if (!found) {
      currentIndex = 0;
      log.error(
          "Step Not Found ({}:{}), will instead use: {}",
          stepDisplayName,
          (player != null) ? player.getName() : "null",
          steps.get(currentIndex));
    }
  }

  public void addStep(final GameStep step) {
    steps.add(step);
  }

  public int getRound() {
    return round + roundOffset;
  }

  public void setRoundOffset(final int roundOffset) {
    this.roundOffset = roundOffset;
  }

  public int getStepIndex() {
    return currentIndex;
  }

  void setStepIndex(final int newIndex) {
    if ((newIndex < 0) || (newIndex >= steps.size())) {
      throw new IllegalArgumentException("New index out of range: " + newIndex);
    }
    currentIndex = newIndex;
  }

  /**
   * Moves to the next step.
   *
   * @return boolean whether the round has changed.
   */
  public synchronized boolean next() {
    currentIndex++;
    if (currentIndex >= steps.size()) {
      currentIndex = 0;
      round++;
      return true;
    }
    return false;
  }

  /**
   * Only tests to see if we are on the last step. Used for finding if we need to make a new round
   * or not. Does not change any data or fields.
   */
  public synchronized boolean testWeAreOnLastStep() {
    return currentIndex + 1 >= steps.size();
  }

  public synchronized GameStep getStep() {
    // since we can now delete game steps mid game, it is a good idea to test if our index is out of
    // range
    if (currentIndex < 0) {
      currentIndex = 0;
    }
    if (currentIndex >= steps.size()) {
      next();
    }
    return getStep(currentIndex);
  }

  public GameStep getStep(final int index) {
    if ((index < 0) || (index >= steps.size())) {
      throw new IllegalArgumentException(
          "Attempt to access invalid state: " + index + ", steps = " + steps);
    }
    return steps.get(index);
  }

  @Override
  public Iterator<GameStep> iterator() {
    return steps.iterator();
  }

  public int size() {
    return steps.size();
  }
}
