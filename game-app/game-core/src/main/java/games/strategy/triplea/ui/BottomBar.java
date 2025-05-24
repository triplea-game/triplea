package games.strategy.triplea.ui;

import static games.strategy.engine.framework.lookandfeel.LookAndFeel.convertColorToHex;
import static games.strategy.engine.framework.lookandfeel.LookAndFeel.getRelationshipTypeAttachmentColor;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.TerritoryListener;
import games.strategy.engine.data.events.ZoomMapListener;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.RelationshipTypeAttachment;
import games.strategy.triplea.attachments.TerritoryAttachment;
import java.awt.Image;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.ObjectUtils;
import org.triplea.java.collections.IntegerMap;
import org.triplea.java.concurrency.AsyncRunner;
import org.triplea.java.concurrency.CompletableFutureUtils;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

@Slf4j
public class BottomBar extends JPanel implements TerritoryListener, ZoomMapListener {
  private final UiContext uiContext;

  private final ResourceBar resourceBar;
  private final JPanel territoryInfo = new JPanel();
  private @Nullable Territory currentTerritory;

  private final JLabel statusMessage = new JLabel();

  private final JLabel playerLabel = new JLabel();
  private final JLabel stepLabel = new JLabel();
  private final JLabel roundLabel = new JLabel();
  private final JLabel zoomLabel = new JLabel();

  public BottomBar(final UiContext uiContext, final GameData data, final boolean usingDiceServer) {
    this.uiContext = uiContext;
    this.resourceBar = new ResourceBar(data, uiContext);

    setLayout(new java.awt.BorderLayout());
    add(createCenterPanel(), java.awt.BorderLayout.CENTER);
    add(createStepPanel(usingDiceServer), java.awt.BorderLayout.EAST);
  }

  private JPanel createCenterPanel() {
    final JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new java.awt.GridBagLayout());
    final var gridBuilder =
        new GridBagConstraintsBuilder().weightY(1).fill(GridBagConstraintsFill.BOTH);

    centerPanel.add(
        resourceBar, gridBuilder.weightX(0).anchor(GridBagConstraintsAnchor.WEST).build());

    territoryInfo.setPreferredSize(new java.awt.Dimension(0, 0));
    territoryInfo.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    centerPanel.add(
        territoryInfo,
        gridBuilder.gridX(1).weightX(1).anchor(GridBagConstraintsAnchor.CENTER).build());

    statusMessage.setVisible(false);
    statusMessage.setPreferredSize(new java.awt.Dimension(0, 0));
    statusMessage.setBorder(new EtchedBorder(EtchedBorder.RAISED));

    zoomLabel.setVisible(false);
    zoomLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));

    centerPanel.add(
        statusMessage, gridBuilder.gridX(2).anchor(GridBagConstraintsAnchor.EAST).build());
    centerPanel.add(
        zoomLabel, gridBuilder.weightX(0).anchor(GridBagConstraintsAnchor.EAST).build());
    return centerPanel;
  }

  private JPanel createStepPanel(boolean usingDiceServer) {
    final JPanel stepPanel = new JPanel();
    stepPanel.setLayout(new java.awt.GridBagLayout());
    final var gridBuilder = new GridBagConstraintsBuilder().fill(GridBagConstraintsFill.BOTH);
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

  private void setStatus(final String msg) {
    statusMessage.setVisible(!msg.isEmpty());
    statusMessage.setText(msg);
  }

  public void setStatus(final String msg, final Image image) {
    setStatus(msg);

    if (!msg.isEmpty()) {
      statusMessage.setIcon(new ImageIcon(image));
    } else {
      statusMessage.setIcon(null);
    }
  }

  public void setStatusAndClearIcon(final String msg) {
    setStatus(msg);
    statusMessage.setIcon(null);
  }

  public void setTerritory(final @Nullable Territory territory) {
    listenForTerritoryUpdates(territory);

    if (territory == null) {
      SwingUtilities.invokeLater(
          () -> {
            territoryInfo.removeAll();
            SwingComponents.redraw(territoryInfo);
          });
      return;
    }

    // Get all the needed data while holding a lock, then invoke UI updates on the EDT.
    try (GameData.Unlocker ignored = territory.getData().acquireReadLock()) {
      final Collection<Unit> units =
          uiContext.isShowUnitsInStatusBar() ? territory.getUnits() : List.of();
      final TerritoryAttachment ta = TerritoryAttachment.get(territory);
      final IntegerMap<Resource> resources = new IntegerMap<>();
      final List<String> territoryEffectNames;
      if (ta == null) {
        territoryEffectNames = List.of();
      } else {
        territoryEffectNames =
            ta.getTerritoryEffect().stream().map(TerritoryEffect::getName).toList();
        final int production = ta.getProduction();
        if (production > 0) {
          resources.add(new Resource(Constants.PUS, territory.getData()), production);
        }
        Optional.ofNullable(ta.getResources()).ifPresent(r -> resources.add(r.getResourcesCopy()));
      }

      SwingUtilities.invokeLater(
          () -> updateTerritoryInfo(territory, territoryEffectNames, units, resources));
    }
  }

  private void updateTerritoryInfo(
      Territory territory,
      List<String> territoryEffectNames,
      Collection<Unit> units,
      IntegerMap<Resource> resources) {
    // Box layout with horizontal glue on both sides achieves the following desirable properties:
    //   1. If the content is narrower than the available space, it will be centered.
    //   2. If the content is wider than the available space, then the beginning will be shown,
    //      which is the more important information (territory name, income, etc.).
    //   3. Elements are vertically centered.
    territoryInfo.removeAll();
    territoryInfo.setLayout(new BoxLayout(territoryInfo, BoxLayout.LINE_AXIS));
    territoryInfo.add(Box.createHorizontalGlue());

    // Display territory effects, territory name, resources and units.
    final StringBuilder territoryEffectText = new StringBuilder();
    for (final String effectName : territoryEffectNames) {
      try {
        final JLabel label = new JLabel();
        label.setToolTipText(effectName);
        label.setIcon(uiContext.getTerritoryEffectImageFactory().getIcon(effectName));
        territoryInfo.add(label);
        territoryInfo.add(Box.createHorizontalStrut(6));
      } catch (final IllegalStateException e) {
        territoryEffectText.append(effectName).append(", ");
      }
    }

    territoryInfo.add(createTerritoryNameLabel(territory));

    if (!territoryEffectText.isEmpty()) {
      territoryEffectText.setLength(territoryEffectText.length() - 2);
      final JLabel territoryEffectTextLabel = new JLabel(" (" + territoryEffectText + ")");
      territoryInfo.add(territoryEffectTextLabel);
    }

    for (final Resource resource : resources.keySet()) {
      territoryInfo.add(Box.createHorizontalStrut(6));
      territoryInfo.add(uiContext.getResourceImageFactory().getLabel(resource, resources));
    }

    if (!units.isEmpty()) {
      JSeparator separator = new JSeparator(JSeparator.VERTICAL);
      separator.setMaximumSize(new java.awt.Dimension(40, getHeight()));
      separator.setPreferredSize(separator.getMaximumSize());
      territoryInfo.add(separator);
      territoryInfo.add(createUnitBar(units));
    }

    territoryInfo.add(Box.createHorizontalGlue());
    SwingComponents.redraw(territoryInfo);
  }

  private JLabel createTerritoryNameLabel(Territory territory) {
    String labelTextPattern = getTerritoryLabelTextPattern(territory);
    final JLabel nameLabel =
        new JLabel(MessageFormat.format(labelTextPattern, territory.getName()));
    nameLabel.setFont(nameLabel.getFont().deriveFont(java.awt.Font.BOLD));
    // Ensure the text position is always the same, regardless of other components, by padding to
    // fill available height.
    final int labelHeight = nameLabel.getPreferredSize().height;
    nameLabel.setBorder(createBorderToFillAvailableHeight(labelHeight, getHeight()));
    return nameLabel;
  }

  private @Nonnull String getTerritoryLabelTextPattern(Territory territory) {
    GamePlayer territoryOwner = territory.getOwner();
    if (territoryOwner == null) return "";
    GamePlayer currentPlayer = uiContext.getCurrentPlayer();
    if (currentPlayer == null)
      currentPlayer = territoryOwner.getData().getPlayerList().getNullPlayer();
    if (territoryOwner.equals(currentPlayer)) {
      return "<html>{0} (current player)</html>";
    }
    final RelationshipTypeAttachment relationshipTypeAttachment =
        territoryOwner
            .getData()
            .getRelationshipTracker()
            .getRelationshipType(territoryOwner, currentPlayer)
            .getRelationshipTypeAttachment();
    String strArchType;
    if (relationshipTypeAttachment.isWar()) {
      strArchType = "at War";
    } else if (relationshipTypeAttachment.isAllied()) {
      strArchType = "Allied";
    } else {
      strArchType = relationshipTypeAttachment.getArcheType();
    }
    return MessageFormat.format(
        "<html>'{'0'}' (<font color={0}>{1}</font>)</html>",
        convertColorToHex(getRelationshipTypeAttachmentColor(relationshipTypeAttachment)),
        strArchType);
  }

  private Border createBorderToFillAvailableHeight(int componentHeight, int availableHeight) {
    int extraVerticalSpace = Math.max(availableHeight - componentHeight, 0);
    int topPad = extraVerticalSpace / 2;
    int bottomPad = extraVerticalSpace - topPad; // Might != topPad if extraVerticalSpace is odd.
    return BorderFactory.createEmptyBorder(topPad, 0, bottomPad, 0);
  }

  private SimpleUnitPanel createUnitBar(Collection<Unit> units) {
    final var unitBar = new SimpleUnitPanel(uiContext, SimpleUnitPanel.Style.SMALL_ICONS_ROW);
    unitBar.setScaleFactor(0.5);
    unitBar.setShowCountsForSingleUnits(false);
    unitBar.setUnits(units);
    // Constrain the preferred size to the available size so that unit images that may not fully fit
    // don't cause layout issues.
    final int unitsWidth = unitBar.getPreferredSize().width;
    unitBar.setPreferredSize(new java.awt.Dimension(unitsWidth, getHeight()));
    return unitBar;
  }

  public void gameDataChanged() {
    resourceBar.gameDataChanged(null);
  }

  public void setStepInfo(int roundNumber, String stepName) {
    roundLabel.setText("Round: " + roundNumber + " ");
    stepLabel.setText(stepName);
  }

  public void updateFromCurrentPlayer() {
    GamePlayer player = uiContext.getCurrentPlayer();
    if (player == null) return;
    final CompletableFuture<?> future =
        CompletableFuture.supplyAsync(() -> uiContext.getFlagImageFactory().getFlag(player))
            .thenApplyAsync(ImageIcon::new)
            .thenAccept(icon -> SwingUtilities.invokeLater(() -> roundLabel.setIcon(icon)));
    CompletableFutureUtils.logExceptionWhenComplete(
        future, throwable -> log.error("Failed to set round icon for " + player, throwable));
    playerLabel.setText((uiContext.isCurrentPlayerRemote() ? "REMOTE: " : "") + player.getName());
  }

  public void setMapZoomEnabled(boolean enabled) {
    zoomLabel.setVisible(enabled);
  }

  private void listenForTerritoryUpdates(@Nullable Territory territory) {
    // Run async, as this is called while holding a GameData lock so we shouldn't grab a different
    // data's lock in this case.
    AsyncRunner.runAsync(
            () -> {
              GameData oldGameData = currentTerritory != null ? currentTerritory.getData() : null;
              GameData newGameData = territory != null ? territory.getData() : null;
              // Re-subscribe listener on the right GameData, which could change when toggling
              // between history and the current game.
              if (!ObjectUtils.referenceEquals(oldGameData, newGameData)) {
                if (oldGameData != null) {
                  try (GameData.Unlocker ignored = oldGameData.acquireWriteLock()) {
                    oldGameData.removeTerritoryListener(this);
                  }
                }
                if (newGameData != null) {
                  try (GameData.Unlocker ignored = newGameData.acquireWriteLock()) {
                    newGameData.addTerritoryListener(this);
                  }
                }
              }
              currentTerritory = territory;
            })
        .exceptionally(e -> log.error("Territory listener error:", e));
  }

  @Override
  public void unitsChanged(Territory territory) {
    if (territory.equals(currentTerritory)) {
      setTerritory(territory);
    }
  }

  @Override
  public void ownerChanged(Territory territory) {
    /*interface method*/
  }

  @Override
  public void attachmentChanged(Territory territory) {
    /*interface method*/
  }

  @Override
  public void zoomMapChanged(Integer newZoom) {
    zoomLabel.setText(String.format("Zoom: %d%%", newZoom));
  }
}
