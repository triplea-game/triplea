package games.strategy.kingstable.delegate;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestDelegateBridge;
import games.strategy.grid.kingstable.delegate.PlayDelegate;
import games.strategy.grid.ui.GridPlayData;
import games.strategy.grid.ui.IGridPlayData;
import games.strategy.kingstable.ui.display.DummyDisplay;

/**
 * Test suite for the King's Table play delegate.
 *
 */
public class PlayDelegateTest extends DelegateTest {
  PlayDelegate m_delegate;
  TestDelegateBridge m_bridgeWhite;
  TestDelegateBridge m_bridgeBlack;

  /**
   * Creates new PlayDelegateTest
   */
  public PlayDelegateTest(final String name) {
    super(name);
  }

  /**
   * This method will be called before each test method in this class is run.
   *
   * So, if there were four test methods in this class, this method would be called four times - once per method.
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    m_bridgeBlack = new TestDelegateBridge(m_data, black, new DummyDisplay());
    m_bridgeBlack.setStepName("BlackTurn", true);
    m_bridgeWhite = new TestDelegateBridge(m_data, white, new DummyDisplay());
    m_bridgeWhite.setStepName("WhiteTurn", true);
    // setupTurn(white);
    setupTurn(black);
  }

  private void setupTurn(final PlayerID player) {
    m_delegate = new PlayDelegate();
    m_delegate.initialize("PlayDelegate", "PlayDelegate");
    if (player == black) {
      m_delegate.setDelegateBridgeAndPlayer(m_bridgeBlack);
      m_delegate.start();
    } else if (player == white) {
      m_delegate.setDelegateBridgeAndPlayer(m_bridgeWhite);
      m_delegate.start();
    }
  }

  /**
   * A normal, completely legal play.
   */
  public void testNormalPlay() {
    final Territory start = m_data.getMap().getTerritoryFromCoordinates(4, 0);
    final Territory end = m_data.getMap().getTerritoryFromCoordinates(4, 3);
    final IGridPlayData play = new GridPlayData(start, end, null);
    final String results = m_delegate.play(play);
    assertValid(results);
  }

  /**
   * A play can't start in an empty square.
   */
  public void testMoveFromEmptySquare() {
    final Territory start = m_data.getMap().getTerritoryFromCoordinates(2, 2);
    final Territory end = m_data.getMap().getTerritoryFromCoordinates(2, 3);
    final IGridPlayData play = new GridPlayData(start, end, null);
    final String results = m_delegate.play(play);
    assertError(results);
  }

  /**
   * A play can't end in a non-empty square.
   */
  public void testMoveToOccupiedSquare() {
    final Territory start = m_data.getMap().getTerritoryFromCoordinates(4, 0);
    final Territory end = m_data.getMap().getTerritoryFromCoordinates(5, 0);
    final IGridPlayData play = new GridPlayData(start, end, null);
    final String results = m_delegate.play(play);
    assertError(results);
  }

  /**
   * A play can't move through an occupied square.
   */
  public void testMoveThroughOccupiedSquare() {
    final Territory start = m_data.getMap().getTerritoryFromCoordinates(5, 0);
    final Territory end = m_data.getMap().getTerritoryFromCoordinates(5, 2);
    final IGridPlayData play = new GridPlayData(start, end, null);
    final String results = m_delegate.play(play);
    assertError(results);
  }
}
