package games.strategy.triplea.ui.history;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Renderable;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.dataObjects.PlacementDescription;
import games.strategy.triplea.ui.DicePanel;
import games.strategy.triplea.ui.MapPanel;
import games.strategy.triplea.ui.SimpleUnitPanel;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;

public class HistoryDetailsPanel extends JPanel implements IHistoryDetailsPanel {
  private static final long serialVersionUID = 5092004144144006960L;
  private final GameData data;
  private final JTextArea title = new JTextArea();
  private final JScrollPane scroll = new JScrollPane(title);
  private final MapPanel mapPanel;

  public HistoryDetailsPanel(final GameData data, final MapPanel mapPanel) {
    this.data = data;
    setLayout(new GridBagLayout());
    title.setWrapStyleWord(true);
    title.setBackground(this.getBackground());
    title.setLineWrap(true);
    title.setBorder(null);
    title.setEditable(false);
    scroll.setBorder(null);
    this.mapPanel = mapPanel;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void render(final HistoryNode node) {
    removeAll();
    mapPanel.setRoute(null);
    final Insets insets = new Insets(5, 0, 0, 0);
    title.setText(node.getTitle());
    add(scroll,
        new GridBagConstraints(0, 0, 1, 1, 1, 0.1, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0));
    final GridBagConstraints mainConstraints =
        new GridBagConstraints(0, 1, 1, 1, 1, 0.9, GridBagConstraints.NORTH, GridBagConstraints.BOTH, insets, 0, 0);
    if (node instanceof Renderable) {
      final Object details = ((Renderable) node).getRenderingData();
      if (details instanceof DiceRoll) {
        final DicePanel dicePanel = new DicePanel(mapPanel.getUIContext(), data);
        dicePanel.setDiceRoll((DiceRoll) details);
        add(dicePanel, mainConstraints);
      } else if (details instanceof MoveDescription) {
        final MoveDescription moveMessage = (MoveDescription) details;
        renderUnits(mainConstraints, moveMessage.getUnits());
        mapPanel.setRoute(moveMessage.getRoute());
        if (!mapPanel.isShowing(moveMessage.getRoute().getEnd())) {
          mapPanel.centerOn(moveMessage.getRoute().getEnd());
        }
      } else if (details instanceof PlacementDescription) {
        final PlacementDescription placeMessage = (PlacementDescription) details;
        renderUnits(mainConstraints, placeMessage.getUnits());
        if (!mapPanel.isShowing(placeMessage.getTerritory())) {
          mapPanel.centerOn(placeMessage.getTerritory());
        }
      } else if (details instanceof Collection) {
        final Collection<Object> objects = (Collection<Object>) details;
        final Iterator<Object> objIter = objects.iterator();
        if (objIter.hasNext()) {
          final Object obj = objIter.next();
          if (obj instanceof Unit) {
            final Collection<Unit> units = (Collection<Unit>) details;
            renderUnits(mainConstraints, units);
          }
        }
      } else if (details instanceof Territory) {
        final Territory t = (Territory) details;
        if (!mapPanel.isShowing(t)) {
          mapPanel.centerOn(t);
        }
      }
    }
    add(Box.createGlue());
    validate();
    repaint();
  }

  private void renderUnits(final GridBagConstraints mainConstraints, final Collection<Unit> units) {
    final Collection<UnitCategory> unitsCategories = UnitSeperator.categorize(units);
    final SimpleUnitPanel unitsPanel = new SimpleUnitPanel(mapPanel.getUIContext());
    unitsPanel.setUnitsFromCategories(unitsCategories, data);
    add(unitsPanel, mainConstraints);
  }
}
