package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SectionHeader;
import org.triplea.swing.SwingAction;

/**
 * Side panel of the game staging screens listing the selected game's options. They are edited in
 * place while the game can be changed, and shown read-only otherwise. Its preferred width fits
 * every label and editor; the staging screen lets the user resize it.
 */
public final class GameOptionsPanel extends JPanel {
  private static final long serialVersionUID = 4185472381096462011L;

  /** Narrower than this, the heading and most option names no longer fit. */
  public static final int MIN_WIDTH = 200;

  private static final int CONFIRMATION_MILLIS = 2000;

  private final GameSelectorModel model;
  private final Map<String, Object> originalValues = new HashMap<>();
  private final JScrollPane scroll = new JScrollPane();
  private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
  private final JLabel readOnlyNote = new JLabel("Set by the host");
  private final JButton reset =
      new JButtonBuilder().title("Reset").actionListener(this::resetOptions).build();

  @Nullable private GameData originalValuesOf;
  @Nullable private GameData shownData;
  private boolean shownEditable;

  GameOptionsPanel(final GameSelectorModel model) {
    this.model = model;

    reset.addActionListener(e -> confirm(reset, "Restored"));
    buttons.setOpaque(false);
    buttons.add(reset);
    readOnlyNote.setForeground(UIManager.getColor("Label.disabledForeground"));

    final JComponent heading = SectionHeader.create("Game Options", readOnlyNote, buttons);
    final JPanel headingHolder = new JPanel(new BorderLayout());
    headingHolder.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
    headingHolder.add(heading);

    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    setLayout(new BorderLayout());
    setBorder(SectionHeader.ruleRight());
    add(headingHolder, BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    setVisible(false);
  }

  /** Shows {@code text} on {@code button} for a moment, then restores its label. */
  private static void confirm(final JButton button, final String text) {
    final String label = button.getText();
    button.setText(text);
    final Timer restore = new Timer(CONFIRMATION_MILLIS, e -> button.setText(label));
    restore.setRepeats(false);
    restore.start();
  }

  /**
   * Shows the selected game's options, rebuilding the editors only when the game changed. Call it
   * as soon as a game is loaded: it records the values {@code Reset} restores, so the options
   * remembered from the last time the game was played must be applied only after it.
   */
  void update() {
    final GameData data = model.getGameData();
    synchronized (originalValues) {
      // a network client drops its game while connected and gets it back on leaving
      if (data != null && data != originalValuesOf) {
        originalValuesOf = data;
        originalValues.clear();
        for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
          originalValues.put(property.getName(), property.getValue());
        }
      }
    }
    SwingAction.invokeNowOrLater(
        () -> {
          if (model.getGameData() != shownData || model.isCanSelect() != shownEditable) {
            rebuild();
          }
        });
  }

  /** Rebuilds the editors, eg: after option values changed underneath them. */
  void refresh() {
    SwingAction.invokeNowOrLater(this::rebuild);
  }

  private void rebuild() {
    shownData = model.getGameData();
    shownEditable = model.isCanSelect();
    final List<IEditableProperty<?>> properties =
        shownData == null ? List.of() : shownData.getProperties().getEditableProperties();
    setVisible(!properties.isEmpty());
    buttons.setVisible(shownEditable);
    readOnlyNote.setVisible(!shownEditable);
    reset.setToolTipText(
        shownData != null && shownData.getSaveGameFileName().isPresent()
            ? "Restore the options stored in the saved game"
            : "Restore the map's own options. The options you last played this game with load"
                + " automatically.");
    final JComponent view =
        properties.isEmpty()
            ? null
            : PropertiesUi.singleColumn(shownData.getProperties(), shownEditable);
    scroll.setViewportView(view);
    if (view != null) {
      // the list is usually taller than the screen, so leave room for the vertical scroll bar
      final int contentWidth =
          view.getPreferredSize().width
              + scroll.getVerticalScrollBar().getPreferredSize().width
              + getInsets().left
              + getInsets().right;
      final int headingWidth = getComponent(0).getPreferredSize().width;
      setPreferredSize(new Dimension(Math.max(contentWidth, headingWidth), 0));
    }
    revalidate();
    repaint();
  }

  private void resetOptions() {
    final GameData data = model.getGameData();
    synchronized (originalValues) {
      if (data == null || data != originalValuesOf) {
        return;
      }
      for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
        property.validateAndSet(originalValues.get(property.getName()));
      }
    }
    refresh();
  }
}
