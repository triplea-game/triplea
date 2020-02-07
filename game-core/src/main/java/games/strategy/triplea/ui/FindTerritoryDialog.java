package games.strategy.triplea.ui;

import games.strategy.engine.data.Territory;
import games.strategy.triplea.ui.panels.map.MapPanel;
import java.awt.Point;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JComboBoxBuilder;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.JPanelBuilder;

final class FindTerritoryDialog extends JDialog {
  private static final long serialVersionUID = -1601616824595826610L;

  private final MapPanel mapPanel;
  private final Point originalMapPanelOffset;
  private Result result = Result.CANCEL;

  FindTerritoryDialog(final TripleAFrame owner) {
    super(owner, "Find Territory", true);

    mapPanel = owner.getMapPanel();
    originalMapPanelOffset = new Point(mapPanel.getXOffset(), mapPanel.getYOffset());

    final Collection<Territory> territories = owner.getGame().getData().getMap().getTerritories();
    final @Nullable Territory initialSelectedTerritory = mapPanel.getCurrentTerritory();
    final JComboBox<Territory> territoryComboBox =
        JComboBoxBuilder.builder(Territory.class)
            .items(territories.stream().sorted().collect(Collectors.toList()))
            .nullableSelectedItem(initialSelectedTerritory)
            .enableAutoComplete()
            .itemSelectedAction(mapPanel::highlightTerritory)
            .build();
    if (initialSelectedTerritory != null) {
      mapPanel.highlightTerritory(initialSelectedTerritory);
    }

    final JButton okButton =
        new JButtonBuilder().okTitle().actionListener(() -> close(Result.OK)).build();
    getRootPane().setDefaultButton(okButton);

    add(
        new JPanelBuilder()
            .border(10)
            .boxLayoutVertical()
            .add(territoryComboBox)
            .addVerticalStrut(20)
            .add(
                new JPanelBuilder()
                    .boxLayoutHorizontal()
                    .addHorizontalGlue()
                    .add(okButton)
                    .addHorizontalStrut(5)
                    .add(
                        new JButtonBuilder()
                            .cancelTitle()
                            .actionListener(() -> close(Result.CANCEL))
                            .build())
                    .build())
            .build());
    pack();
    setLocation(getInitialLocation());

    SwingComponents.addEscapeKeyListener(this, () -> close(Result.CANCEL));
  }

  private Point getInitialLocation() {
    final Point point = mapPanel.getLocation();
    SwingUtilities.convertPointToScreen(point, mapPanel);
    point.translate(20, 20);
    return point;
  }

  void open() {
    setVisible(true);

    mapPanel.clearHighlightedTerritory();
    if (Result.CANCEL.equals(result)) {
      mapPanel.setTopLeft(originalMapPanelOffset.x, originalMapPanelOffset.y);
    }
  }

  private void close(final Result result) {
    setVisible(false);
    dispose();
    this.result = result;
  }

  private enum Result {
    OK,
    CANCEL
  }
}
