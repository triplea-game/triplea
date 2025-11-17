package games.strategy.triplea.ui.menubar.help;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingAction;

@UtilityClass
class MoveHelpMenu {

  Action buildMenu() {
    return SwingAction.of(
        "Movement/Selection",
        e -> {
          // html formatted string
          final JEditorPane editorPane = new JEditorPane();
          editorPane.setEditable(false);
          editorPane.setContentType("text/html");
          final String hints =
              "<font size='+1'>Selecting Units</font><br>"
                  + "Clicking on a unit stack adds <u>1 unit</u> of the stack to the selection.<br>"
                  + "<b>CTRL-click on a unit stack</b> selects <u>All units in the stack</u>.<br>"
                  + "<b>Shift-click on a territory/sea zone</b> selects "
                  + "<u>All units in that zone</u>.<br>"
                  + "<b>Click on a zone</b> (not a unit) to <u>choose the units</u> "
                  + "to select from that zone.<br>"
                  + "<b>Alt-click on a unit stack</b> selects <u>10 units</u> in the stack.<br>"
                  + "<br><font size='+1'> Deselecting Units</font><br>"
                  + "<b>Escape</b> unselects <u>All selected units</u>.<br>"
                  + "<b>Right click</b> on a unit stack deselects <u>1 unit</u> in the stack.<br>"
                  + "<b>CTRL-Right click</b> on a unit stack to unselect <u>All units</u> "
                  + "in the stack.<br>"
                  + "<b>Right click</b> (not on a unit) to deselect "
                  + "<u>the last selected unit</u>.<br>"
                  + "<b>Alt-Right click</b> on a unit stack deselects <u>10 units</u> "
                  + "in the stack.<br>"
                  + "<br><font size='+1'> Moving Units</font><br>"
                  + "After selecting units, simply click on the zone to move the units to.<br>"
                  + "<b>CTRL-click</b> on a zone to select the zone <u>as a way point</u> :<br>"
                  + "units will take the shortest path through this zone to move to "
                  + "destination.<br>"
                  + "<br><font size='+1'> Moving the Map on Screen</font><br>"
                  + "Parameters can be adjusted in menu Game><b>Engine Settings:</b> "
                  + "Map Scrolling.<br>"
                  + "<b>Scroll the mouse wheel</b> to move the map vertically,<br>"
                  + "<b>Hold Shift + Scroll the mouse wheel</b> to move the map horizontally.<br>"
                  + "<b>Hold Right click</b> and move the mouse to <u>drag the map</u>.<br>"
                  + "Click the map (anywhere), <b>use the arrow keys</b> "
                  + "to move the map around,<br>"
                  + "<b>CTRL + arrow</b> moves the map faster.<br>"
                  + "<b>Click in the Minimap</b> at the top right of the screen,"
                  + " and Drag the mouse (or not).<br>"
                  + "Unless disabled, <b>moving the mouse near the edge</b>"
                  + " scrolls the map in that direction.<br>"
                  + "<br>"
                  + "<font size='+1'> Changing the Map Zoom</font><br>"
                  + "<b>Menu View>Zoom</b> to select the desired zoom level (Maximum: 100%).<br>"
                  + "<b>Hold CTRL</b> with -/+ or Scroll the Mouse Wheel.<br>"
                  + "<br><font size='+1'> Other Tips</font><br>"
                  + "Press <b>F</b> to highlight <u>All units with movement left</u> "
                  + "(Move phases only).<br>"
                  + "Press <b>U</b> while mousing over a unit to "
                  + "<u>undo All moves by that unit</u>.<br>";
          editorPane.setText(hints);
          final JScrollPane scroll = new JScrollPane(editorPane);
          JOptionPane.showMessageDialog(
              editorPane,
              scroll,
              "Movement/Selection Help (Keyboard & Mouse)",
              JOptionPane.PLAIN_MESSAGE);
        });
  }
}
