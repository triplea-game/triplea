package games.strategy.kingstable;

import games.strategy.kingstable.delegate.PlayDelegateTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class KingsTableTest
{
	
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(PlayDelegateTest.class);
        
        return suite;
    }

}
