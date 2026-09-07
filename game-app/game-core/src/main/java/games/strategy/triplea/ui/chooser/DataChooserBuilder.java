package games.strategy.triplea.ui.chooser;

import static javax.swing.JOptionPane.OK_OPTION;

import games.strategy.engine.data.NamedAttachable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import org.triplea.swing.SwingComponents;

public class DataChooserBuilder<T extends NamedAttachable> {
  final JOptionPane optionPane = new JOptionPane();
  private JList<T> list;

  public DataChooserBuilder(
      final Collection<T> collection,
      final T initialSelectedValue,
      final Function<Object, Icon> valueToIconFunction) {
    optionPane.setMessageType(JOptionPane.PLAIN_MESSAGE);
    optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
    optionPane.setIcon(null);
    createComponents(collection, initialSelectedValue, valueToIconFunction);
  }

  private void createComponents(
      final Collection<T> dataList,
      final T initialSelectedValue,
      final Function<Object, Icon> valueToIconFunction) {
    final DefaultListModel<T> model = new DefaultListModel<>();
    dataList.forEach(model::addElement);
    list = new JList<>(model);
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
              optionPane.setValue(OK_OPTION);
            }
          }
        });
    JScrollPane scrollPane = SwingComponents.newJScrollPane(list);

    final int maxSize = 700;
    final int suggestedSize = list.getPreferredSize().height;
    final int actualSize = Math.min(suggestedSize, maxSize);
    scrollPane.setPreferredSize(new Dimension(300, actualSize));
    optionPane.setMessage(scrollPane);
  }

  /**
   * @return the selected entry
   */
  public Optional<T> getSelected() {
    if (optionPane.getValue() != null && optionPane.getValue().equals(OK_OPTION)) {
      return Optional.of(list.getSelectedValue());
    }
    return Optional.empty();
  }

  public Optional<T> showDialog(final Component parent, final String title) {
    final JDialog dialog = optionPane.createDialog(parent, title);
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
