/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package games.strategy.kingstable.delegate;

import junit.framework.Test;
import junit.framework.TestSuite;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.kingstable.ui.display.DummyDisplay;


/**
 * Test suite for the King's Table play delegate.
 * 
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class PlayDelegateTest extends DelegateTest
{

	PlayDelegate m_delegate;
	TestDelegateBridge m_bridgeWhite;
	TestDelegateBridge m_bridgeBlack;
	

	/** 
	 * Creates new PlayDelegateTest 
	 */
	public PlayDelegateTest(String name)
	{
		super(name);
	}

	/**
	 * Create a test suite for the King's Table play delegate.
	 * 
	 * @return a test suite for the King's Table play delegate
	 */
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTestSuite(PlayDelegateTest.class);

		return suite;
	}

	
	/**
	 * This method will be called before each test method in this class is run.
	 * 
	 * So, if there were four test methods in this class, this method would be called four times - once per method.
	 */
	public void setUp() throws Exception
	{
		super.setUp();
		
		m_bridgeBlack = new TestDelegateBridge(m_data, black, new DummyDisplay());
		m_bridgeBlack.setStepName("BlackTurn");
		m_bridgeWhite = new TestDelegateBridge(m_data, white, new DummyDisplay());
		m_bridgeWhite.setStepName("WhiteTurn");
		
		//setupTurn(white);
		setupTurn(black);
	}
	
	private void setupTurn(PlayerID player)
	{
		m_delegate = new PlayDelegate();
		m_delegate.initialize("PlayDelegate", "PlayDelegate");
		
		if (player==black)
			m_delegate.start(m_bridgeBlack, m_data);
		else if (player==white)
			m_delegate.start(m_bridgeWhite, m_data);
	}

	/**
	 * A normal, completely legal play.
	 */
	public void testNormalPlay()
	{
		Territory start = m_data.getMap().getTerritoryFromCoordinates(4,0);
		Territory end = m_data.getMap().getTerritoryFromCoordinates(4,3);
		
		String results = m_delegate.play(start, end);
		assertValid(results);
	}
	
	/**
	 * A play can't start in an empty square.
	 */
	public void testMoveFromEmptySquare()
	{
		Territory start = m_data.getMap().getTerritoryFromCoordinates(2,2);
		Territory end = m_data.getMap().getTerritoryFromCoordinates(2,3);
		
		String results = m_delegate.play(start, end);
		assertError(results);
	}
  

	/**
	 * A play can't end in a non-empty square.
	 */
	public void testMoveToOccupiedSquare()
	{
		Territory start = m_data.getMap().getTerritoryFromCoordinates(4,0);
		Territory end = m_data.getMap().getTerritoryFromCoordinates(5,0);
		
		String results = m_delegate.play(start, end);
		assertError(results);
	}
	
	
	/**
	 * A play can't move through an occupied square.
	 */
	public void testMoveThroughOccupiedSquare()
	{
		Territory start = m_data.getMap().getTerritoryFromCoordinates(5,0);
		Territory end = m_data.getMap().getTerritoryFromCoordinates(5,2);
		
		String results = m_delegate.play(start, end);
		assertError(results);
	}
	
	/*
	public void prepareForCapturesPt1()
	{
		String results = null;
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(3,0), m_data.getMap().getTerritoryFromCoordinates(1,0));
		assertValid(results);
		
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(6,4), m_data.getMap().getTerritoryFromCoordinates(9,4));
		assertValid(results);
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,1), m_data.getMap().getTerritoryFromCoordinates(10,1));
		assertValid(results);
		
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,3), m_data.getMap().getTerritoryFromCoordinates(2,3));
		assertValid(results);
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,9), m_data.getMap().getTerritoryFromCoordinates(7,9));
		assertValid(results);
		
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,4), m_data.getMap().getTerritoryFromCoordinates(5,1));
		assertValid(results);
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(7,10), m_data.getMap().getTerritoryFromCoordinates(8,10));
		assertValid(results);
	}
	
	public void prepareForCapturesPt2()
	{
		String results = null;
		
		// King moves
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,5), m_data.getMap().getTerritoryFromCoordinates(5,3));
		assertValid(results);		
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(10,6), m_data.getMap().getTerritoryFromCoordinates(9,6));
		assertValid(results);
		
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(5,3), m_data.getMap().getTerritoryFromCoordinates(8,3));
		assertValid(results);	
		
		setupTurn(black);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(10,7), m_data.getMap().getTerritoryFromCoordinates(10,9));
		assertValid(results);
		
		setupTurn(white);
		results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(8,3), m_data.getMap().getTerritoryFromCoordinates(8,9));
		assertValid(results);
	}
	
	public void prepareForCaptures()
	{
		prepareForCapturesPt1();
		prepareForCapturesPt2();
	}
	
	public void testCaptureWhite()
	{
		int originalWhitePieces = white.getUnits().getUnitCount();
		int originalBlackPieces = black.getUnits().getUnitCount();
		
		prepareForCaptures();
		
		setupTurn(black);
		String results = m_delegate.play(m_data.getMap().getTerritoryFromCoordinates(10,3), m_data.getMap().getTerritoryFromCoordinates(9,3));
		assertValid(results);
		
		assertNumberOfPieces(black, originalBlackPieces);
		assertNumberOfPieces(white, originalWhitePieces-1);
		
	}
	
	
	public void assertNumberOfPieces(PlayerID player, int numPieces)
	{
		int actualNumPieces = player.getUnits().getUnitCount();
		System.out.println(actualNumPieces);
		System.out.println(numPieces);
		
		assertEquals(actualNumPieces, numPieces);
	}
	
	
	*/
	
	
	
	
	
}
