package games.strategy.triplea;

import games.strategy.engine.random.EmailValidatorTest;
import games.strategy.triplea.baseAI.AIUtilsTest;
import games.strategy.triplea.delegate.AirThatCantLandUtilTest;
import games.strategy.triplea.delegate.BattleCalculatorTest;
import games.strategy.triplea.delegate.BigWorldTest;
import games.strategy.triplea.delegate.DiceRollTest;
import games.strategy.triplea.delegate.LHTRTest;
import games.strategy.triplea.delegate.MoveDelegateTest;
import games.strategy.triplea.delegate.MoveValidatorTest;
import games.strategy.triplea.delegate.PacificTest;
import games.strategy.triplea.delegate.Pacific_1940_Test;
import games.strategy.triplea.delegate.Pact_of_Steel_2_Test;
import games.strategy.triplea.delegate.PlaceDelegateTest;
import games.strategy.triplea.delegate.RevisedTest;
import games.strategy.triplea.delegate.UnitsThatCantFightUtilTest;
import games.strategy.triplea.delegate.VictoryTest;
import games.strategy.triplea.delegate.WW2V3_41_Test;
import games.strategy.triplea.delegate.WW2V3_42_Test;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorTest;
import games.strategy.triplea.util.UnitAutoChooserTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TripleASuite
{
	public static Test suite()
	{
		final TestSuite suite = new TestSuite(TripleASuite.class.getSimpleName());
		suite.addTestSuite(PlaceDelegateTest.class);
		suite.addTestSuite(MoveDelegateTest.class);
		suite.addTestSuite(MoveValidatorTest.class);
		suite.addTestSuite(EmailValidatorTest.class);
		suite.addTestSuite(RevisedTest.class);
		suite.addTestSuite(BigWorldTest.class);
		suite.addTestSuite(WW2V3_41_Test.class);
		suite.addTestSuite(WW2V3_42_Test.class);
		suite.addTestSuite(LHTRTest.class);
		suite.addTestSuite(PacificTest.class);
		suite.addTestSuite(VictoryTest.class);
		suite.addTestSuite(Pact_of_Steel_2_Test.class);
		suite.addTestSuite(Pacific_1940_Test.class);
		suite.addTestSuite(DiceRollTest.class);
		suite.addTestSuite(AIUtilsTest.class);
		suite.addTestSuite(OddsCalculatorTest.class);
		suite.addTestSuite(AirThatCantLandUtilTest.class);
		suite.addTestSuite(UnitAutoChooserTest.class);
		suite.addTestSuite(UnitsThatCantFightUtilTest.class);
		suite.addTestSuite(BattleCalculatorTest.class);
		return suite;
	}
}
