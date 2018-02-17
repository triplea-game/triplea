package games.strategy.triplea.ui;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.oddsCalculator.ta.OddsCalculatorDialog;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeperator;
import games.strategy.ui.OverlayIcon;
import games.strategy.ui.SwingComponents;

public class TerritoryDetailPanel extends AbstractStatPanel {
  private static final long serialVersionUID = 1377022163587438988L;
  private final UiContext uiContext;
  private final JButton showOdds = new JButton("Battle Calculator (Ctrl-B)");
  private Territory currentTerritory;
  private final TripleAFrame frame;

  public static String getHoverText() {
    return "Hover over or drag and drop from a territory to list those units in this panel";
  }

  TerritoryDetailPanel(final MapPanel mapPanel, final GameData data, final UiContext uiContext,
      final TripleAFrame frame) {
    super(data);
    this.frame = frame;
    this.uiContext = uiContext;
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
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));

    showOdds.addActionListener(e -> OddsCalculatorDialog.show(frame, currentTerritory));
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(
        frame, 'B', () -> OddsCalculatorDialog.show(frame, currentTerritory));
  }

  @Override
  public void setGameData(final GameData data) {
    gameData = data;
    territoryChanged(null);
  }

  private void territoryChanged(final Territory territory) {
    currentTerritory = territory;
    removeAll();
    refresh();
    if (territory == null) {
      return;
    }
    add(showOdds);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    final String labelText;
    if (ta == null) {
      labelText = "<html>" + territory.getName() + "<br>Water Territory" + "<br><br></html>";
    } else {
      labelText = "<html>" + ta.toStringForInfo(true, true) + "<br></html>";
    }
    add(new JLabel(labelText));
    Collection<Unit> unitsInTerritory;
    gameData.acquireReadLock();
    try {
      unitsInTerritory = territory.getUnits().getUnits();
    } finally {
      gameData.releaseReadLock();
    }
    add(new JLabel("Units: " + unitsInTerritory.size()));
    final JScrollPane scroll = new JScrollPane(unitsInTerritoryPanel(unitsInTerritory, uiContext));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    add(scroll);
    refresh();
  }

  private static JPanel unitsInTerritoryPanel(final Collection<Unit> unitsInTerritory, final UiContext uiContext) {
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 2));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    final Set<UnitCategory> units = UnitSeperator.categorize(unitsInTerritory);
    PlayerID currentPlayer = null;
    for (final UnitCategory item : units) {
      // seperate players with a seperator
      if (item.getOwner() != currentPlayer) {
        currentPlayer = item.getOwner();
        panel.add(Box.createVerticalStrut(15));
      }
      // TODO Kev determine if we need to identify if the unit is hit/disabled
      final Optional<ImageIcon> unitIcon =
          uiContext.getUnitImageFactory().getIcon(item.getType(), item.getOwner(),
              item.hasDamageOrBombingUnitDamage(), item.getDisabled());
      if (unitIcon.isPresent()) {
        // overlay flag onto upper-right of icon
        final ImageIcon flagIcon = new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
        final Icon flaggedUnitIcon =
            new OverlayIcon(unitIcon.get(), flagIcon, unitIcon.get().getIconWidth() - (flagIcon.getIconWidth() / 2), 0);
        final JLabel label = new JLabel("x" + item.getUnits().size(), flaggedUnitIcon, SwingConstants.LEFT);
        final String toolTipText =
            "<html>" + item.getType().getName() + ": " + item.getType().getTooltip(currentPlayer) + "</html>";
        label.setToolTipText(toolTipText);
        panel.add(label);
      }
    }
    return panel;
  }

  private void refresh() {
    validate();
    repaint();
  }
}
