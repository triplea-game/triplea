package games.strategy.engine.framework.startup.ui.editors;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.MutableComboBoxModel;

import games.strategy.triplea.help.HelpSupport;
import games.strategy.triplea.ui.JButtonDialog;

/**
 * Allows you put multiple beans in a list and use drop down to select which bean to configure.
 * The bean's editor is displayed below the dropdown.
 * Use <code>setBeans</code> to set the beans edited by this editor, and <code>setSelectedBean</code> to select a
 * specific bean
 * The editor automatically realigns the label of nested editors
 */
public class SelectAndViewEditor extends EditorPanel {
  private static final long serialVersionUID = 1580648148539524876L;
  private final JComboBox<IBean> selector = new JComboBox<>();
  private final JPanel view = new JPanel();
  private final PropertyChangeListener propertyChangeListener;
  private EditorPanel editor;
  private final JLabel selectorLabel;
  private final JEditorPane helpPanel;
  private final String defaultHelp;

  /**
   * creates a new editor.
   *
   * @param labelTitle
   *        the title in front of the combo box
   * @param defaultHelp
   *        the name of the Help file to use when no bean is selected (when disabled)
   */
  public SelectAndViewEditor(final String labelTitle, final String defaultHelp) {
    super();
    this.defaultHelp = defaultHelp;
    final JButton helpButton = new JButton("Help?");
    final Font oldFont = helpButton.getFont();
    helpButton.setFont(new Font(oldFont.getName(), Font.BOLD, oldFont.getSize()));
    view.setLayout(new GridBagLayout());
    selectorLabel = new JLabel(labelTitle + ":");
    add(selectorLabel, new GridBagConstraints(0, 0, 1, 1, 0d, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.NONE, new Insets(0, 0, 1, 2), 0, 0));
    add(selector, new GridBagConstraints(1, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 1, 0), 0, 0));
    add(helpButton, new GridBagConstraints(2, 0, 1, 1, 0d, 0, GridBagConstraints.NORTHEAST, GridBagConstraints.NONE,
        new Insets(0, 0, 1, 0), 0, 0));
    add(view, new GridBagConstraints(0, 1, 3, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0));
    selector.setRenderer(new DisplayNameComboBoxRender());
    selector.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateView();
        fireEditorChanged();
      }
    });
    propertyChangeListener = evt -> fireEditorChanged();
    helpPanel = new JEditorPane();
    helpPanel.setEditable(false);
    helpPanel.setContentType("text/html");
    helpPanel.setAutoscrolls(true);
    helpPanel.setBackground(selectorLabel.getBackground());
    final Dimension preferredSize = new Dimension(500, 500);
    helpPanel.setPreferredSize(preferredSize);
    helpPanel.setSize(preferredSize);
    final JScrollPane notesScroll = new JScrollPane();
    notesScroll.setViewportView(helpPanel);
    notesScroll.setBorder(null);
    notesScroll.getViewport().setBorder(null);
    helpButton.addActionListener(e -> {
      final String helpText;
      if (getBean() == null) {
        helpText = HelpSupport.loadHelp(this.defaultHelp);
      } else {
        helpText = getBean().getHelpText();
      }
      helpPanel.setText(helpText);
      helpPanel.setCaretPosition(0);
      JButtonDialog.showDialog(SelectAndViewEditor.this, "Help", notesScroll, "Close");
    });
  }

  /**
   * Updates the view panel below the combo box.
   */
  private void updateView() {
    // todo(kg) Have the View use a card layout instead of removing all content
    // remove listeners from old editor, to avoid memory leak
    if (editor != null) {
      editor.removePropertyChangeListener(propertyChangeListener);
    }
    view.removeAll();
    final IBean item = (IBean) selector.getSelectedItem();
    editor = item.getEditor();
    if (editor != null) {
      // register a property change listener so we can re-notify our listeners
      editor.addPropertyChangeListener(propertyChangeListener);
      view.add(editor, new GridBagConstraints(0, 0, 1, 1, 1.0, 0, GridBagConstraints.NORTHWEST,
          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
      editor.isBeanValid();
    }
    revalidate();
    alignLabels();
  }

  /**
   * Aligns label of this editor with the nested editor, by either resizing this (if it is smaller)
   * or resizing the labels on the nested editor (if it is bigger).
   */
  private void alignLabels() {
    // resize the label to align with the nested editors labels
    final int height = selectorLabel.getPreferredSize().height;
    int width = selectorLabel.getPreferredSize().width;
    if (editor != null) {
      final int labelWidth = editor.getLabelWidth();
      if (width < labelWidth) {
        // resize this editors label
        width = labelWidth;
      } else {
        // resize nested editors labels
        editor.setLabelWidth(width);
      }
    }
    final Dimension dimension = new Dimension(width, height);
    selectorLabel.setPreferredSize(dimension);
    selectorLabel.setSize(dimension);
  }

  /**
   * Sets the list of possible beans to choose from.
   *
   * @param beans the list of beans
   */
  public void setBeans(final Collection<? extends IBean> beans) {
    selector.setModel(new DefaultComboBoxModel<>(beans.toArray(new IBean[beans.size()])));
    updateView();
  }

  @Override
  public boolean isBeanValid() {
    return (editor == null) || editor.isBeanValid();
  }

  /**
   * Returns the bean being edited.
   *
   * @return the current bean, or null if the bean doesn't have an editor (is disabled)
   */
  @Override
  public IBean getBean() {
    if (editor == null) {
      return null;
    }
    return editor.getBean();
  }

  /**
   * Sets the bean on this editor.
   * If an editor of the same class is found, it is selected an modified to match
   * If no bean of this type is found, it is added to the list
   *
   * @param bean the bean
   */
  public void setSelectedBean(final IBean bean) {
    final MutableComboBoxModel<IBean> model = (MutableComboBoxModel<IBean>) selector.getModel();
    final DefaultComboBoxModel<IBean> newModel = new DefaultComboBoxModel<>();
    boolean found = false;
    for (int i = 0; i < model.getSize(); i++) {
      final IBean candidate = model.getElementAt(i);
      if (candidate.equals(bean)) {
        found = true;
        newModel.addElement(bean);
      } else {
        newModel.addElement(candidate);
      }
    }
    if (found) {
      selector.setModel(newModel);
    } else {
      model.addElement(bean);
    }
    selector.setSelectedItem(bean);
    updateView();
  }
}
