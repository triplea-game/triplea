package tools.map.xml.creator;

import static org.junit.Assert.assertTrue;

import javax.swing.JPanel;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class GameSequencePanelTest extends MapXmlCreatorTestBase {

  @Test
  public void testLayout() {
    // //TODO: find a way to allow Travis CI build without failing with
    // // "java.awt.HeadlessException:
    // // No X11 DISPLAY variable was set, but this program performed an operation which requires it."
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
