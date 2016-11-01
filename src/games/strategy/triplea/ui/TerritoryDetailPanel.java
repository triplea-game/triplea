package games.strategy.triplea.ui;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.OverlayIcon;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

public class TerritoryDetailPanel extends AbstractStatPanel {
  private final IUIContext m_uiContext;
  private final Button m_showOdds = new Button("Battle Calculator (Ctrl-B)");
  private Territory m_currentTerritory;
  private final TripleAFrame m_frame;
  private final VBox content = new VBox();

  public static String getHoverText() {
    return "Hover over or drag and drop from a territory to list those units in this panel";
  }

  public TerritoryDetailPanel(final MapPanel mapPanel, final GameData data, final IUIContext uiContext,
      final TripleAFrame frame) {
    super(data);
    m_frame = frame;
    m_uiContext = uiContext;
    mapPanel.addMapSelectionListener(new DefaultMapSelectionListener() {
      @Override
      public void mouseEntered(final Territory territory) {
        territoryChanged(territory);
      }
    });
    initLayout();
  }

  @Override
  protected void initLayout() {
    getChildren().add(content);
    content.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.B && (e.isMetaDown() || e.isControlDown())) {
        OddsCalculatorDialog.show(m_frame, m_currentTerritory);
      }
    });
  }

  @Override
  public void setGameData(final GameData data) {
    m_data = data;
    territoryChanged(null);
  }

  private void territoryChanged(final Territory territory) {
    m_currentTerritory = territory;
    content.getChildren().clear();
    if (territory == null) {
      return;
    }
    content.getChildren().add(m_showOdds);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    String labelText;
    if (ta == null) {
      labelText = "<html>" + territory.getName() + "<br>Water Territory" + "<br><br></html>";
    } else {
      labelText = "<html>" + ta.toStringForInfo(true, true) + "<br></html>";
    }
    content.getChildren().add(new Label(labelText));
    Collection<Unit> unitsInTerritory;
    m_data.acquireReadLock();
    try {
      unitsInTerritory = territory.getUnits().getUnits();
    } finally {
      m_data.releaseReadLock();
    }
    content.getChildren().add(new Label("Units: " + unitsInTerritory.size()));
    final ScrollPane scroll = new ScrollPane(unitsInTerritoryPanel(unitsInTerritory, m_uiContext, m_data));
    content.getChildren().add(scroll);
  }

  private static VBox unitsInTerritoryPanel(final Collection<Unit> unitsInTerritory, final IUIContext uiContext,
      final GameData data) {
    final VBox panel = new VBox();
    // panel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 2));TODO CSS
    final Set<UnitCategory> units = UnitSeperator.categorize(unitsInTerritory);
    final Iterator<UnitCategory> iter = units.iterator();
    PlayerID currentPlayer = null;
    while (iter.hasNext()) {
      // seperate players with a seperator
      final UnitCategory item = iter.next();
      if (item.getOwner() != currentPlayer) {
        currentPlayer = item.getOwner();
      }
      // TODO Kev determine if we need to identify if the unit is hit/disabled
      final Optional<ImageIcon> unitIcon =
          uiContext.getUnitImageFactory().getIcon(item.getType(), item.getOwner(), data,
              item.hasDamageOrBombingUnitDamage(), item.getDisabled());
      if (unitIcon.isPresent()) {
        // overlay flag onto upper-right of icon
        final ImageIcon flagIcon = new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
        @SuppressWarnings("unused")
        final Icon flaggedUnitIcon =
            new OverlayIcon(unitIcon.get(), flagIcon, unitIcon.get().getIconWidth() - (flagIcon.getIconWidth() / 2), 0);
        final Label label = new Label("x" + item.getUnits().size());
        label.setGraphic(new ImageView(/* flaggedUnitIcon belongs here */));
        final String toolTipText =
            "<html>" + item.getType().getName() + ": " + item.getType().getTooltip(currentPlayer) + "</html>";
        label.setTooltip(new Tooltip(toolTipText));
        panel.getChildren().add(label);
      }
    }
    return panel;
  }
}
