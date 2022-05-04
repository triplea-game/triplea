package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

public class BottomBar extends JPanel {
  private final UiContext uiContext;
  private final GameData data;

  private final ResourceBar resourceBar;
  private final JPanel territoryInfo = new JPanel();

  private final JLabel statusMessage = new JLabel();

  private final JLabel playerLabel = new JLabel("xxxxxx");
  private final JLabel stepLabel = new JLabel("xxxxxx");
  private final JLabel roundLabel = new JLabel("xxxxxx");

  public BottomBar(final UiContext uiContext, final GameData data, final boolean usingDiceServer) {
    this.uiContext = uiContext;
    this.data = data;
    this.resourceBar = new ResourceBar(data, uiContext);

    setLayout(new BorderLayout());
    add(createCenterPanel(), BorderLayout.CENTER);
    add(createStepPanel(usingDiceServer), BorderLayout.EAST);
  }

  private JPanel createCenterPanel() {
    final JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new GridBagLayout());
    final var gridBuilder =
        new GridBagConstraintsBuilder(0, 0).weightY(1).fill(GridBagConstraintsFill.BOTH);

    centerPanel.add(
        resourceBar, gridBuilder.weightX(0).anchor(GridBagConstraintsAnchor.WEST).build());

    territoryInfo.setPreferredSize(new Dimension(0, 0));
    territoryInfo.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    centerPanel.add(
        territoryInfo,
        gridBuilder.gridX(1).weightX(1).anchor(GridBagConstraintsAnchor.CENTER).build());

    statusMessage.setPreferredSize(new Dimension(0, 0));
    statusMessage.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    centerPanel.add(
        statusMessage, gridBuilder.gridX(2).anchor(GridBagConstraintsAnchor.EAST).build());
    return centerPanel;
  }

  private JPanel createStepPanel(boolean usingDiceServer) {
    final JPanel stepPanel = new JPanel();
    stepPanel.setLayout(new GridBagLayout());
    final var gridBuilder = new GridBagConstraintsBuilder(0, 0).fill(GridBagConstraintsFill.BOTH);
    stepPanel.add(playerLabel, gridBuilder.gridX(0).build());
    stepPanel.add(stepLabel, gridBuilder.gridX(1).build());
    stepPanel.add(roundLabel, gridBuilder.gridX(2).build());
    if (usingDiceServer) {
      final JLabel diceServerLabel = new JLabel("Dice Server On");
      diceServerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
      stepPanel.add(diceServerLabel, gridBuilder.gridX(3).build());
    }
    stepLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    roundLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    playerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    stepLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    return stepPanel;
  }

  public void setStatus(final String msg, final Optional<Image> image) {
    statusMessage.setText(msg);

    if (!msg.isEmpty() && image.isPresent()) {
      statusMessage.setIcon(new ImageIcon(image.get()));
    } else {
      statusMessage.setIcon(null);
    }
  }

  public void setTerritory(final @Nullable Territory territory) {
    territoryInfo.removeAll();

    if (territory == null) {
      SwingComponents.redraw(territoryInfo);
      return;
    }

    // Box layout with horizontal glue on both sides achieves the following desirable properties:
    //   1. If the content is narrower than the available space, it will be centered.
    //   2. If the content is wider than the available space, then the beginning will be shown,
    //      which is the more important information (territory name, income, etc).
    //   3. Elements are vertically centered.
    territoryInfo.setLayout(new BoxLayout(territoryInfo, BoxLayout.LINE_AXIS));
    territoryInfo.add(Box.createHorizontalGlue());

    final TerritoryAttachment ta = TerritoryAttachment.get(territory);

    // Display territory effects, territory name, resources and units.
    final StringBuilder territoryEffectText = new StringBuilder();
    final List<TerritoryEffect> territoryEffects = ta != null ? ta.getTerritoryEffect() : List.of();
    for (final TerritoryEffect territoryEffect : territoryEffects) {
      try {
        final JLabel territoryEffectLabel = new JLabel();
        territoryEffectLabel.setToolTipText(territoryEffect.getName());
        territoryEffectLabel.setIcon(
            uiContext.getTerritoryEffectImageFactory().getIcon(territoryEffect.getName()));
        territoryInfo.add(territoryEffectLabel);
        territoryInfo.add(Box.createHorizontalStrut(10));
      } catch (final IllegalStateException e) {
        territoryEffectText.append(territoryEffect.getName()).append(", ");
      }
    }

    territoryInfo.add(createTerritoryNameLabel(territory.getName()));

    if (territoryEffectText.length() > 0) {
      territoryEffectText.setLength(territoryEffectText.length() - 2);
      final JLabel territoryEffectTextLabel = new JLabel("(" + territoryEffectText + ")");
      territoryInfo.add(territoryEffectTextLabel);
    }

    Optional.ofNullable(ta).ifPresent(this::addTerritoryResourceDetails);

    final Collection<UnitCategory> units = UnitSeparator.categorize(territory.getUnits());
    if (!units.isEmpty()) {
      JSeparator separator = new JSeparator(JSeparator.VERTICAL);
      separator.setMaximumSize(new Dimension(40, getHeight()));
      separator.setPreferredSize(separator.getMaximumSize());
      territoryInfo.add(separator);
      territoryInfo.add(createUnitBar(units));
    }

    territoryInfo.add(Box.createHorizontalGlue());
    SwingComponents.redraw(territoryInfo);
  }

  private JLabel createTerritoryNameLabel(String territoryName) {
    final JLabel nameLabel = new JLabel(territoryName);
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
    // Ensure the text position is always the same, regardless of other components, by padding to
    // fill available height.
    final int labelHeight = nameLabel.getPreferredSize().height;
    nameLabel.setBorder(createBorderToFillAvailableHeight(labelHeight, getHeight()));
    return nameLabel;
  }

  private Border createBorderToFillAvailableHeight(int componentHeight, int availableHeight) {
    int extraVerticalSpace = Math.max(availableHeight - componentHeight, 0);
    int topPad = extraVerticalSpace / 2;
    int bottomPad = extraVerticalSpace - topPad; // Might != topPad if extraVerticalSpace is odd.
    return BorderFactory.createEmptyBorder(topPad, 0, bottomPad, 0);
  }

  private void addTerritoryResourceDetails(TerritoryAttachment ta) {
    final IntegerMap<Resource> resources = new IntegerMap<>();
    final int production = ta.getProduction();
    if (production > 0) {
      resources.add(new Resource(Constants.PUS, data), production);
    }
    Optional.ofNullable(ta.getResources()).ifPresent(r -> resources.add(r.getResourcesCopy()));
    for (final Resource resource : resources.keySet()) {
      territoryInfo.add(Box.createHorizontalStrut(10));
      territoryInfo.add(uiContext.getResourceImageFactory().getLabel(resource, resources));
    }
  }

  private SimpleUnitPanel createUnitBar(Collection<UnitCategory> units) {
    final var unitBar = new SimpleUnitPanel(uiContext, SimpleUnitPanel.Style.SMALL_ICONS_ROW);
    unitBar.setScaleFactor(0.5);
    unitBar.setShowCountsForSingleUnits(false);
    unitBar.setUnitsFromCategories(units);
    // Constrain the preferred size to the available size so that unit images that may not fully fit
    // don't cause layout issues.
    final int unitsWidth = unitBar.getPreferredSize().width;
    unitBar.setPreferredSize(new Dimension(unitsWidth, getHeight()));
    return unitBar;
  }

  public void gameDataChanged() {
    resourceBar.gameDataChanged(null);
  }

  public void setRoundIcon(ImageIcon icon) {
    roundLabel.setIcon(icon);
  }

  public void setStepInfo(
      int roundNumber, String stepName, GamePlayer player, boolean isRemotePlayer) {
    roundLabel.setText("Round:" + roundNumber + " ");
    stepLabel.setText(stepName);
    if (player != null) {
      this.playerLabel.setText((isRemotePlayer ? "REMOTE: " : "") + player.getName());
    }
  }
}
