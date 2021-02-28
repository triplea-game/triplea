package games.strategy.triplea.ui.menubar.help;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

@UtilityClass
class MoveHelpMenu {
  private static final String moveSelectionHelpTitle = "Movement/Selection Help";

  Action buildMenu() {
    return SwingAction.of(
        moveSelectionHelpTitle,
        e -> {
          // html formatted string
          final JEditorPane editorPane = new JEditorPane();
          editorPane.setEditable(false);
          editorPane.setContentType("text/html");
          final String hints =
              "<b> Selecting Units</b><br>"
                  + "Left click on a unit stack to select 1 unit.<br>"
                  + "ALT-Left click on a unit stack to select 10 units of that type in "
                  + "the stack.<br>"
                  + "CTRL-Left click on a unit stack to select all units of that type in the "
                  + "stack.<br>"
                  + "Shift-Left click on a unit to select all units in the territory.<br>"
                  + "Left click on a territory but not on a unit to bring up a selection "
                  + "window for inputting the desired selection.<br>"
                  + "<br><b> Deselecting Units</b><br>"
                  + "Right click somewhere not on a unit stack to unselect the last "
                  + "selected unit.<br>"
                  + "Right click on a unit stack to unselect one unit in the stack.<br>"
                  + "ALT-Right click on a unit stack to unselect 10 units of that type in "
                  + "the stack.<br>"
                  + "CTRL-Right click on a unit stack to unselect all units of that type "
                  + "in the stack.<br>"
                  + "CTRL-Right click somewhere not on a unit stack to unselect all units "
                  + "selected.<br>"
                  + "<br><b> Moving Units</b><br>"
                  + "After selecting units Left click on a territory to move units there "
                  + "(do not Left click and Drag, instead select units, then move the mouse, "
                  + "then select the territory).<br>"
                  + "CTRL-Left click on a territory to select the territory as a way point "
                  + "(forces units take the shortest path to move through this territory on "
                  + "their way to the destination).<br>"
                  + "<br><b> Moving the Map Screen</b><br>"
                  + "Right click and Drag the mouse to move your screen over the map.<br>"
                  + "Left click the map (anywhere), use the arrow keys (or WASD keys) to move "
                  + "your map around. Holding down control will move the map faster.<br />"
                  + "Left click in the Minimap at the top right of the screen, and Drag "
                  + "the mouse.<br>"
                  + "Move the mouse to the edge of the map to scroll in that direction. Moving "
                  + "the mouse even closer to the edge will scroll faster.<br>"
                  + "Scrolling the mouse wheel will move the map up and down.<br>"
                  + "<br><b> Zooming Out</b><br>"
                  + "Holding ALT while Scrolling the Mouse Wheel will zoom the map in and "
                  + "out.<br>"
                  + "Select 'Zoom' from the 'View' menu, and change to the desired level.<br>"
                  + "Hold CTRL with - or + to zoom out and in.<br>"
                  + "<br><b> Other Things</b><br>"
                  + "Press 'f' to highlight all units you own that have movement left "
                  + "(move phases only).<br>"
                  + "Press 'u' while mousing over a unit to undo all moves that unit has made "
                  + "(beta).<br>"
                  + "To list specific units from a territory in the Territory panel, drag and "
                  + "drop from the territory on the map to the territory panel.<br>";
          editorPane.setText(hints);
          final JScrollPane scroll = new JScrollPane(editorPane);
          JOptionPane.showMessageDialog(
              null, scroll, moveSelectionHelpTitle, JOptionPane.PLAIN_MESSAGE);
        });
  }
}
