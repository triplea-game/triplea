package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameSequence;
import games.strategy.engine.data.GameStep;

import java.util.ArrayList;

class GameSequenceChange extends Change {
  private static final long serialVersionUID = -8925565771506676074L;
  private final GameStep[] m_oldSteps;
  private final GameStep[] m_newSteps;

  GameSequenceChange(final GameSequence oldSequence, final GameStep[] newSteps) {
    final ArrayList<GameStep> oldSteps = new ArrayList<>();
    for (final GameStep step : oldSequence) {
      oldSteps.add(step);
    }
    m_oldSteps = oldSteps.toArray(new GameStep[oldSteps.size()]);
    m_newSteps = newSteps;
  }

  private GameSequenceChange(final GameStep[] oldSteps, final GameStep[] newSteps) {
    m_oldSteps = oldSteps;
    m_newSteps = newSteps;
  }

  @Override
  protected void perform(final GameData data) {
    final GameSequence steps = data.getSequence();
    steps.removeAllSteps();
    for (final GameStep newStep : m_newSteps) {
      steps.addStep(newStep);
    }
  }

  @Override
  public Change invert() {
    return new GameSequenceChange(m_newSteps, m_oldSteps);
  }
}
