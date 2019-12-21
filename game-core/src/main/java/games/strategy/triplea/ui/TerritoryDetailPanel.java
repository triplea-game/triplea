package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.odds.calculator.BattleCalculatorDialog;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.ui.OverlayIcon;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
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
import org.triplea.swing.SwingComponents;

class TerritoryDetailPanel extends AbstractStatPanel {
  private static final long serialVersionUID = 1377022163587438988L;
  private final UiContext uiContext;
  private final JButton showOdds = new JButton("Battle Calculator (Ctrl-B)");
  private final JButton addAttackers = new JButton("Add Attackers (Ctrl-A)");
  private final JButton addDefenders = new JButton("Add Defenders (Ctrl-D)");
  private final JButton findTerritoryButton;
  private final JLabel territoryInfo = new JLabel();
  private final JLabel unitInfo = new JLabel();
  private final JScrollPane units = new JScrollPane();
  private @Nullable Territory currentTerritory;
  private final TripleAFrame frame;

  TerritoryDetailPanel(
      final MapPanel mapPanel,
      final GameData data,
      final UiContext uiContext,
      final TripleAFrame frame) {
    super(data);
    this.frame = frame;
    this.uiContext = uiContext;
    mapPanel.addMapSelectionListener(
        new DefaultMapSelectionListener() {
          @Override
          public void mouseEntered(final Territory territory) {
            territoryChanged(territory);
          }
        });

    findTerritoryButton = new JButton(new FindTerritoryAction(frame));
    findTerritoryButton.setText(findTerritoryButton.getText() + " (Ctrl-F)");

    initLayout();
  }

  protected void initLayout() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    setBorder(new EmptyBorder(5, 5, 0, 0));

    showOdds.addActionListener(
        e -> BattleCalculatorDialog.show(frame, currentTerritory, gameData.getHistory()));
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(
        frame,
        'B',
        () -> BattleCalculatorDialog.show(frame, currentTerritory, gameData.getHistory()));

    addAttackers.addActionListener(e -> BattleCalculatorDialog.addAttackers(currentTerritory));
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(
        frame, 'A', () -> BattleCalculatorDialog.addAttackers(currentTerritory));

    addDefenders.addActionListener(e -> BattleCalculatorDialog.addDefenders(currentTerritory));
    SwingComponents.addKeyListenerWithMetaAndCtrlMasks(
        frame, 'D', () -> BattleCalculatorDialog.addDefenders(currentTerritory));
    units.setBorder(BorderFactory.createEmptyBorder());
    units.getVerticalScrollBar().setUnitIncrement(20);
    add(showOdds);
    add(addAttackers);
    add(addDefenders);
    add(findTerritoryButton);
    add(territoryInfo);
    add(unitInfo);
    add(units);
    showOdds.setVisible(false);
    addAttackers.setVisible(false);
    addDefenders.setVisible(false);
    findTerritoryButton.setVisible(false);
    territoryInfo.setVisible(false);
    unitInfo.setVisible(false);
    units.setVisible(false);
  }

  public void setGameData(final GameData data) {
    gameData = data;
    territoryChanged(null);
  }

  private void territoryChanged(final Territory territory) {
    if (Objects.equals(currentTerritory, territory)) {
      return;
    }
    currentTerritory = territory;
    if (territory == null) {
      showOdds.setVisible(false);
      addAttackers.setVisible(false);
      addDefenders.setVisible(false);
      findTerritoryButton.setVisible(false);
      territoryInfo.setVisible(false);
      unitInfo.setVisible(false);
      units.setVisible(false);
      return;
    }
    showOdds.setVisible(true);
    addAttackers.setVisible(true);
    addDefenders.setVisible(true);
    findTerritoryButton.setVisible(true);
    territoryInfo.setVisible(true);
    unitInfo.setVisible(true);
    units.setVisible(true);
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    final String labelText;
    if (ta == null) {
      labelText = "<html>" + territory.getName() + "<br>Water Territory" + "<br><br></html>";
    } else {
      labelText = "<html>" + ta.toStringForInfo(true, true) + "<br></html>";
    }
    territoryInfo.setText(labelText);
    unitInfo.setText(
        "Units: "
            + territory.getUnits().stream()
                .filter(u -> uiContext.getMapData().shouldDrawUnit(u.getType().getName()))
                .count());
    units.setViewportView(unitsInTerritoryPanel(territory, uiContext));
  }

  private static JPanel unitsInTerritoryPanel(
      final Territory territory, final UiContext uiContext) {
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 2));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    final List<UnitCategory> units =
        UnitSeparator.getSortedUnitCategories(territory, uiContext.getMapData());
    @Nullable PlayerId currentPlayer = null;
    for (final UnitCategory item : units) {
      // separate players with a separator
      if (!item.getOwner().equals(currentPlayer)) {
        currentPlayer = item.getOwner();
        panel.add(Box.createVerticalStrut(15));
      }
      // TODO Kev determine if we need to identify if the unit is hit/disabled
      final Optional<ImageIcon> unitIcon =
          uiContext
              .getUnitImageFactory()
              .getIcon(
                  item.getType(),
                  item.getOwner(),
                  item.hasDamageOrBombingUnitDamage(),
                  item.getDisabled());
      if (unitIcon.isPresent()) {
        // overlay flag onto upper-right of icon
        final ImageIcon flagIcon =
            new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
        final Icon flaggedUnitIcon =
            new OverlayIcon(
                unitIcon.get(),
                flagIcon,
                unitIcon.get().getIconWidth() - (flagIcon.getIconWidth() / 2),
                0);
        final JLabel label =
            new JLabel("x" + item.getUnits().size(), flaggedUnitIcon, SwingConstants.LEFT);
        final String toolTipText =
            "<html>"
                + item.getType().getName()
                + ": "
                + TooltipProperties.getInstance().getTooltip(item.getType(), currentPlayer)
                + "</html>";
        label.setToolTipText(toolTipText);
        panel.add(label);
      }
    }
    return panel;
  }
}
