package games.strategy.triplea.delegate.power.calculator;

public interface OffenseOrDefenseCalculator {

  StrengthOrRollCalculator getRoll();

  StrengthOrRollCalculator getStrength();
}
