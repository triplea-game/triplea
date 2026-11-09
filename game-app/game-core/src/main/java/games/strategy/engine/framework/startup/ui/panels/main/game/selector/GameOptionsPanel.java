package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.startup.ui.IGamePropertiesCache;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.UIManager;
import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.SwingAction;

/**
 * Side panel of the game staging screens listing the selected game's options. They are edited in
 * place while the game can be changed, and shown read-only otherwise.
 */
public final class GameOptionsPanel extends JPanel {
  private static final long serialVersionUID = 4185472381096462011L;

  /** Long option names would otherwise let the panel crowd out the player list. */
  private static final int MAX_WIDTH = 480;

  private final GameSelectorModel model;
  private final Map<String, Object> originalValues = new HashMap<>();
  private final JScrollPane scroll = new JScrollPane();
  private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

  @Nullable private GameData originalValuesOf;
  @Nullable private GameData shownData;
  private boolean shownEditable;

  GameOptionsPanel(final GameSelectorModel model, final IGamePropertiesCache gamePropertiesCache) {
    this.model = model;

    final JLabel title = new JLabel("Game Options");
    title.setFont(title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2));
    final JButton reset =
        new JButtonBuilder()
            .title("Reset")
            .toolTip("Restore the options this game started with")
            .actionListener(this::resetOptions)
            .build();
    final JButton makeDefault =
        new JButtonBuilder()
            .title("Make Default")
            .toolTip("Start this game with these options from now on")
            .actionListener(() -> gamePropertiesCache.cacheGameProperties(model.getGameData()))
            .build();
    buttons.add(reset);
    buttons.add(makeDefault);

    final JPanel heading = new JPanel(new BorderLayout());
    heading.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, separatorColor()),
            BorderFactory.createEmptyBorder(8, 10, 8, 4)));
    heading.add(title, BorderLayout.WEST);
    heading.add(buttons, BorderLayout.EAST);

    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);

    setLayout(new BorderLayout());
    setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, separatorColor()));
    add(heading, BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    setVisible(false);
  }

  static Color separatorColor() {
    return UIManager.getColor("Separator.foreground");
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
