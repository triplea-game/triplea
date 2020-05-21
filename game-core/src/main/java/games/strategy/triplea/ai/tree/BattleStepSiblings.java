package games.strategy.triplea.ai.tree;

import lombok.Data;

@Data
public class BattleStepSiblings {
  double probability;
  double winProbability;
  double loseProbability;
  double tieProbability;
  double averageRounds;
  StepUnits units;
}
