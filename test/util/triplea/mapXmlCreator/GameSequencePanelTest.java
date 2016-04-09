package util.triplea.mapXmlCreator;

import javax.swing.JPanel;

import org.junit.Test;

public class GameSequencePanelTest extends MapXmlCreatorTestBase {

  @Test
  public void testLayout() {
    GameSequencePanel.layout(getMapXmlCreator());

    assertTrue(DynamicRowsPanel.me.isPresent());
    final DynamicRowsPanel dynamicRowsPanel = DynamicRowsPanel.me.get();
    assertTrue((dynamicRowsPanel instanceof GameSequencePanel));

    // test number of components added to ownPanel
    final GameSequencePanel panel = (GameSequencePanel) dynamicRowsPanel;
    final JPanel ownPanel = panel.getOwnPanel();
    final int countOwnPanelComponents = ownPanel.getComponents().length;
    final int countExpectedRows = MapXmlHelper.getGamePlaySequenceMap().size();
    final int countComponentsPerRow = 4;
    final int countHeaderLabels = 3;
    final int countBottomButtons = 1;
    final int countExpectedComponents =
        countExpectedRows * countComponentsPerRow + countHeaderLabels + countBottomButtons;
    assertTrue((countExpectedComponents == countOwnPanelComponents));
  }

}
