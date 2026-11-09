package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.startup.ui.IGamePropertiesCache;
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
 * place while the game can be changed, and shown read-only otherwise.
 */
public final class GameOptionsPanel extends JPanel {
  private static final long serialVersionUID = 4185472381096462011L;

  /** Long option names would otherwise let the panel crowd out the player list. */
  private static final int MAX_WIDTH = 480;

  private static final int CONFIRMATION_MILLIS = 2000;

  private final GameSelectorModel model;
  private final Map<String, Object> originalValues = new HashMap<>();
  private final JScrollPane scroll = new JScrollPane();
  private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
  private final JLabel readOnlyNote = new JLabel("Set by the host");

  @Nullable private GameData originalValuesOf;
  @Nullable private GameData shownData;
  private boolean shownEditable;

  GameOptionsPanel(final GameSelectorModel model, final IGamePropertiesCache gamePropertiesCache) {
    this.model = model;

    final JButton reset =
        new JButtonBuilder()
            .title("Reset")
            .toolTip("Restore the options this game started with")
            .actionListener(this::resetOptions)
            .build();
    reset.addActionListener(e -> confirm(reset, "Restored"));
    final JButton makeDefault =
        new JButtonBuilder()
            .title("Make Default")
            .toolTip("Start this game with these options from now on")
            .build();
    makeDefault.addActionListener(
        e -> {
          gamePropertiesCache.cacheGameProperties(model.getGameData());
          confirm(makeDefault, "Saved");
        });
    buttons.setOpaque(false);
    buttons.add(reset);
    buttons.add(makeDefault);
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
   * as soon as a game is loaded: it records the values {@code Reset} restores, so a game's cached
   * default options must be applied only after it.
   */
  void update() {
    final GameData data = model.getGameData();
    synchronized (originalValues) {
      if (data != originalValuesOf) {
        originalValuesOf = data;
        originalValues.clear();
        if (data != null) {
          for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
            originalValues.put(property.getName(), property.getValue());
          }
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
    final JComponent view =
        properties.isEmpty()
            ? null
            : PropertiesUi.singleColumn(shownData.getProperties(), shownEditable);
    scroll.setViewportView(view);
    if (view != null) {
      final int contentWidth =
          view.getPreferredSize().width
              + scroll.getVerticalScrollBar().getPreferredSize().width
              + getInsets().left
              + getInsets().right;
      final int headingWidth = getComponent(0).getPreferredSize().width;
      setPreferredSize(new Dimension(Math.min(Math.max(contentWidth, headingWidth), MAX_WIDTH), 0));
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
