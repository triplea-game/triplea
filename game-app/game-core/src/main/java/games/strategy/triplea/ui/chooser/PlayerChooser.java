package games.strategy.triplea.ui.chooser;

import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.PlayerList;
import games.strategy.triplea.ui.UiContext;
import games.strategy.ui.Util;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import org.triplea.swing.SwingComponents;

public class PlayerChooser extends JOptionPane {
  private static final long serialVersionUID = -7272867474891641839L;
  private JList<GamePlayer> list;

  public PlayerChooser(
      final PlayerList players,
      final GamePlayer defaultPlayer,
      final UiContext uiContext,
      final boolean allowNeutral) {
    this(createDataList(players, allowNeutral), defaultPlayer, uiContext);
  }

  private static Collection<GamePlayer> createDataList(PlayerList players, boolean allowNeutral) {
    Collection<GamePlayer> dataList = new ArrayList<>(players.getPlayers());
    if (allowNeutral) {
      dataList.add(players.getNullPlayer());
    }
    return dataList;
  }

  PlayerChooser(
      final Collection<GamePlayer> collection,
      final GamePlayer initialSelectedValue,
      final UiContext uiContext) {
    setMessageType(JOptionPane.PLAIN_MESSAGE);
    setOptionType(JOptionPane.OK_CANCEL_OPTION);
    setIcon(null);
    createComponents(
        collection,
        initialSelectedValue,
        value ->
            (uiContext == null || ((GamePlayer) value).isNull()
                ? new ImageIcon(Util.newImage(32, 32, true))
                : new ImageIcon(uiContext.getFlagImageFactory().getFlag((GamePlayer) value))));
  }

  private void createComponents(
      final Collection<GamePlayer> dataList,
      final GamePlayer initialSelectedValue,
      final Function<Object, Icon> valueToIconFunction) {
    list = new JList<>(dataList.toArray(new GamePlayer[0]));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedValue(initialSelectedValue, true);
    list.setFocusable(false);
    list.setCellRenderer(new Renderer(valueToIconFunction));
    list.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(final MouseEvent evt) {
            if (evt.getClickCount() == 2) {
              // set OK_OPTION on DoubleClick, this fires a property change which causes the dialog
              // to close()
              setValue(OK_OPTION);
            }
          }
        });
    JScrollPane scrollPane = SwingComponents.newJScrollPane(list);

    final int maxSize = 700;
    final int suggestedSize = list.getPreferredSize().height;
    final int actualSize = Math.min(suggestedSize, maxSize);
    scrollPane.setPreferredSize(new Dimension(300, actualSize));
    setMessage(scrollPane);
  }

  /**
   * Returns the selected entry or null, or null if the dialog was closed.
   *
   * @return the entry or null
   */
  public Optional<GamePlayer> getSelected() {
    if (getValue() != null && getValue().equals(JOptionPane.OK_OPTION)) {
      return Optional.of(list.getSelectedValue());
    }
    return Optional.empty();
  }

  public Optional<GamePlayer> showDialog(final Component parent, final String title) {
    final JDialog dialog = createDialog(parent, title);
    dialog.setVisible(true);
    return getSelected();
  }

  protected static final class Renderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -2185921124436293304L;
    private final Function<Object, Icon> iconProvider;

    Renderer(final Function<Object, Icon> iconProvider) {
      this.iconProvider = iconProvider;
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      super.getListCellRendererComponent(
          list, ((NamedAttachable) value).getName(), index, isSelected, cellHasFocus);
      setIcon(iconProvider.apply(value));
      return this;
    }
  }
}
