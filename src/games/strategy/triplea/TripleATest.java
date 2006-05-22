package games.strategy.triplea;

import games.strategy.engine.random.EmailValidatorTest;
import games.strategy.triplea.delegate.*;
import junit.framework.*;

public class TripleATest
{

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(PlaceDelegateTest.class);
        suite.addTestSuite(MoveDelegateTest.class);
        suite.addTestSuite(MoveValidatorTest.class);
        suite.addTestSuite(EmailValidatorTest.class);
        suite.addTestSuite(RevisedTest.class);
        suite.addTestSuite(LHTRTest.class);
        suite.addTestSuite(DiceRollTest.class);
        return suite;
    }

}
