package games.strategy.triplea.ui;

import static games.strategy.triplea.image.UnitImageFactory.ImageKey;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.odds.calculator.BattleCalculatorDialog;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import games.strategy.ui.OverlayIcon;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.triplea.swing.key.binding.KeyCode;
import org.triplea.swing.key.binding.SwingKeyBinding;

public class TerritoryDetailPanel extends JPanel {
  private static final long serialVersionUID = 1377022163587438988L;
  private GameData gameData;
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

  TerritoryDetailPanel(final TripleAFrame frame) {
    this.frame = frame;
    this.uiContext = frame.getUiContext();
    this.gameData = frame.getGame().getData();
    frame
        .getMapPanel()
        .addMapSelectionListener(
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

    showOdds.addActionListener(e -> BattleCalculatorDialog.show(frame, currentTerritory, gameData));
    addAttackers.addActionListener(e -> BattleCalculatorDialog.addAttackers(currentTerritory));
    addDefenders.addActionListener(e -> BattleCalculatorDialog.addDefenders(currentTerritory));
    addBattleCalculatorKeyBindings(frame);
    units.setBorder(BorderFactory.createEmptyBorder());
    units.getVerticalScrollBar().setUnitIncrement(20);
    add(showOdds);
    add(addAttackers);
    add(addDefenders);
    add(findTerritoryButton);
    add(territoryInfo);
    add(unitInfo);
    add(units);
    setElementsVisible(false);
  }

  /**
   * Adds the battle calculator key bindings (CTRL-A, CTRL-D, CTRL-B) to the frame {@code jframe}.
   * When triggered the {@code addAttackers(Territory)}, {@code addDefenders(Territory)} and {@code
   * show(TripleAFrame, Territory, History)} methods of the battle calculator are triggered with the
   * TripleAFrame, current territory and history of this TerritoryDetailPanel.
   *
   * @param jframe the frame to add the key bindings to
   */
  public void addBattleCalculatorKeyBindings(final JFrame jframe) {
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        jframe, KeyCode.B, () -> BattleCalculatorDialog.show(frame, currentTerritory, gameData));
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        jframe, KeyCode.A, () -> BattleCalculatorDialog.addAttackers(currentTerritory));
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        jframe, KeyCode.D, () -> BattleCalculatorDialog.addDefenders(currentTerritory));
  }

  /**
   * Same as {@code addBattleCalculatorKeyBindings(JFrame)} but for {@code JDialog}.
   *
   * @param dialog the dialog to add the key bindings to
   */
  public void addBattleCalculatorKeyBindings(final JDialog dialog) {
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        dialog, KeyCode.B, () -> BattleCalculatorDialog.show(frame, currentTerritory, gameData));
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        dialog, KeyCode.A, () -> BattleCalculatorDialog.addAttackers(currentTerritory));
    SwingKeyBinding.addKeyBindingWithMetaAndCtrlMasks(
        dialog, KeyCode.D, () -> BattleCalculatorDialog.addDefenders(currentTerritory));
  }

  private void setElementsVisible(final boolean visible) {
    showOdds.setVisible(visible);
    addAttackers.setVisible(visible);
    addDefenders.setVisible(visible);
    findTerritoryButton.setVisible(visible);
    territoryInfo.setVisible(visible);
    unitInfo.setVisible(visible);
    units.setVisible(visible);
  }

  public void setGameData(final GameData data) {
    gameData = data;
    territoryChanged(null);
  }

  private void territoryChanged(final @Nullable Territory territory) {
    currentTerritory = territory;
    if (territory == null) {
      setElementsVisible(false);
      return;
    }
    setElementsVisible(true);
    final String labelText;
    final String additionalText =
        frame.getAdditionalTerritoryDetails().computeAdditionalText(territory);
    final Optional<TerritoryAttachment> optionalTerritoryAttachment =
        TerritoryAttachment.get(territory);
    labelText =
        optionalTerritoryAttachment
            .map(
                territoryAttachment ->
                    "<html>"
                        + territoryAttachment.toStringForInfo(true, true)
                        + "<br>"
                        + additionalText
                        + "</html>")
            .orElseGet(
                () ->
                    "<html>"
                        + territory.getName()
                        + "<br>Water Territory"
                        + "<br><br>"
                        + additionalText
                        + "</html>");
    territoryInfo.setText(labelText);

    final List<UnitCategory> unitsList;
    final String unitsLabel;

    // Get the unit information under lock as otherwise they may change on the game thread causing a
    // ConcurrentModificationException.
    try (GameData.Unlocker ignored = gameData.acquireReadLock()) {
      unitsList = UnitSeparator.getSortedUnitCategories(territory, uiContext.getMapData());
      unitsLabel =
          "Units: "
              + territory.getUnits().stream()
                  .filter(u -> uiContext.getMapData().shouldDrawUnit(u.getType().getName()))
                  .count();
    }

    unitInfo.setText(unitsLabel);
    units.setViewportView(unitsInTerritoryPanel(unitsList, uiContext));
  }

  private static JPanel unitsInTerritoryPanel(
      final List<UnitCategory> units, final UiContext uiContext) {
    final JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createEmptyBorder(2, 20, 2, 2));
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

    @Nullable GamePlayer currentPlayer = null;
    for (final UnitCategory item : units) {
      // separate players with a separator
      if (!item.isOwnedBy(currentPlayer)) {
        currentPlayer = item.getOwner();
        panel.add(Box.createVerticalStrut(15));
      }
      final ImageIcon unitIcon = uiContext.getUnitImageFactory().getIcon(ImageKey.of(item));
      // overlay flag onto upper-right of icon
      final ImageIcon flagIcon =
          new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(item.getOwner()));
      final Icon flaggedUnitIcon =
          new OverlayIcon(
              unitIcon, flagIcon, unitIcon.getIconWidth() - (flagIcon.getIconWidth() / 2), 0);
      final JLabel label =
          new JLabel("x" + item.getUnits().size(), flaggedUnitIcon, SwingConstants.LEFT);
      final String toolTipText =
          "<html>"
              + item.getType().getName()
              + ": "
              + uiContext.getTooltipProperties().getTooltip(item.getType(), currentPlayer)
              + "</html>";
      label.setToolTipText(toolTipText);
      panel.add(label);
    }
    return panel;
  }
}
