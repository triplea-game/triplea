package games.strategy.triplea.ui.history;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.MoveDescription;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.PlacementDescription;
import games.strategy.triplea.ui.DicePanel;
import games.strategy.triplea.ui.SimpleUnitPanel;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.triplea.swing.ScrollableJPanel;
import org.triplea.swing.SwingComponents;

/**
 * A UI component that displays details about the currently-selected history node.
 *
 * <p>The history node details include:
 *
 * <ul>
 *   <li>The textual description of the node.
 *   <li>A custom UI component provided by the node that provides graphical details. For example, a
 *       purchase event may display the icons (and counts) for each unit purchased.
 * </ul>
 */
public class HistoryDetailsPanel extends JPanel {
  private static final long serialVersionUID = 5092004144144006960L;
  private final GameData data;
  private final MapPanel mapPanel;
  private final JTextArea title = new JTextArea();
  private final JPanel content = new ScrollableJPanel();

  public HistoryDetailsPanel(final GameData data, final MapPanel mapPanel) {
    this.data = data;
    this.mapPanel = mapPanel;

    content.setLayout(new GridBagLayout());

    title.setWrapStyleWord(true);
    title.setBackground(this.getBackground());
    title.setLineWrap(true);
    title.setBorder(null);
    title.setEditable(false);

    SwingUtilities.invokeLater(
        () -> {
          final JScrollPane scroll = new JScrollPane(content);
          scroll.setBorder(null);
          scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
          scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
          setLayout(new BorderLayout());
          add(scroll, BorderLayout.CENTER);
        });
  }

  @SuppressWarnings("unchecked")
  void render(final HistoryNode node) {
    mapPanel.setRoute(null);
    content.removeAll();
    final Insets insets = new Insets(5, 0, 0, 0);
    title.setText(node.getTitle());
    content.add(
        title,
        new GridBagConstraints(
            0, 0, 1, 1, 1, 0.1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
    final GridBagConstraints mainConstraints =
        new GridBagConstraints(
            0, 1, 1, 1, 1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0);
    if (node instanceof Renderable renderable) {
      final Object renderingData = renderable.getRenderingData();
      if (renderingData instanceof DiceRoll diceRoll) {
        final DicePanel dicePanel = new DicePanel(mapPanel.getUiContext(), data);
        dicePanel.setDiceRoll(diceRoll);
        content.add(dicePanel, mainConstraints);
      } else if (renderingData instanceof MoveDescription moveMessage) {
        renderUnits(mainConstraints, moveMessage.getUnits());
        mapPanel.setRoute(moveMessage.getRoute());
        showTerritory(moveMessage.getRoute().getEnd());
      } else if (renderingData instanceof PlacementDescription placeMessage) {
        renderUnits(mainConstraints, placeMessage.getUnits());
        showTerritory(placeMessage.getTerritory());
      } else if (renderingData instanceof Collection) {
        final Collection<Object> objects = (Collection<Object>) renderingData;
        final Iterator<Object> objIter = objects.iterator();
        if (objIter.hasNext()) {
          final Object obj = objIter.next();
          if (obj instanceof Unit) {
            final Collection<Unit> units = (Collection<Unit>) renderingData;
            renderUnits(mainConstraints, units);
          }
        }
      } else if (renderingData instanceof Territory territory) {
        showTerritory(territory);
      }
    }
    content.add(Box.createGlue());
    SwingComponents.redraw(this);
  }

  private void showTerritory(final Territory territory) {
    if (!mapPanel.isShowing(territory)) {
      mapPanel.centerOn(territory);
    }
  }

  private void renderUnits(final GridBagConstraints mainConstraints, final Collection<Unit> units) {
    final SimpleUnitPanel unitsPanel = new SimpleUnitPanel(mapPanel.getUiContext());
    unitsPanel.setUnits(units);
    content.add(unitsPanel, mainConstraints);
  }
}
