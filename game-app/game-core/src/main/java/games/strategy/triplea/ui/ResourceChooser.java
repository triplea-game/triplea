package games.strategy.triplea.ui;

import games.strategy.engine.data.Resource;
import games.strategy.triplea.Constants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import org.triplea.swing.SwingComponents;

class ResourceChooser extends JOptionPane {
  private static final long serialVersionUID = -7272867474891641839L;
  private JList<Resource> list;
  private final UiContext uiContext;

  ResourceChooser(final List<Resource> resources, final UiContext uiContext) {
    setMessageType(JOptionPane.PLAIN_MESSAGE);
    setOptionType(JOptionPane.OK_CANCEL_OPTION);
    setIcon(null);
    this.uiContext = uiContext;
    createComponents(resources);
  }

  private void createComponents(final List<Resource> resources) {
    list = new JList<>(resources.toArray(new Resource[0]));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedValue(
        resources.stream().filter(r -> r.getName().equals(Constants.PUS)).findFirst().orElseThrow(),
        true);
    list.setFocusable(false);
    list.setCellRenderer(new Renderer(uiContext));
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
    setMessage(SwingComponents.newJScrollPane(list));

    final int maxSize = 700;
    final int suggestedSize = list.getModel().getSize() * 40;
    final int actualSize = Math.min(suggestedSize, maxSize);
    setPreferredSize(new Dimension(300, actualSize));
  }

  /**
   * Returns the selected resource or null, or null if the dialog was closed.
   *
   * @return the resource or null
   */
  @Nullable
  Resource getSelected() {
    if (getValue() != null && getValue().equals(JOptionPane.OK_OPTION)) {
      return list.getSelectedValue();
    }
    return null;
  }

  public Resource showDialog(final Component parent, final String title) {
    final JDialog dialog = createDialog(parent, title);
    dialog.setVisible(true);
    return getSelected();
  }

  private static final class Renderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -2185921124436293304L;
    private final UiContext uiContext;

    Renderer(final UiContext uiContext) {
      this.uiContext = uiContext;
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      super.getListCellRendererComponent(
          list, ((Resource) value).getName(), index, isSelected, cellHasFocus);
      setIcon(uiContext.getResourceImageFactory().getIcon(((Resource) value).getName()));
      return this;
    }
  }
}
