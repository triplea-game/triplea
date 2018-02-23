package swinglib;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * 'Composite' builder can be used to create a JComboBox plus an attached component that
 * can be hidden or revealed depending on combo box selection. So for example you could
 * use this composite builder to add an extra settings panel that is shown when a specific
 * value is selected in the combo box.
 */
public class JComboBoxCompositeBuilder {

  private final JComboBoxBuilder comboBoxBuilder;

  private final Map<String, JComponent> componentRevealMap = new HashMap<>();


  private String triggerValueForPanelHide;
  private JComponent componentToHide;

  private String label;

  JComboBoxCompositeBuilder(final JComboBoxBuilder comboBoxBuilder) {
    this.comboBoxBuilder = comboBoxBuilder;
  }

  public JComboBoxCompositeBuilder label(final String label) {
    this.label = label;
    return this;
  }

  public JComboBoxCompositeBuilder onSelectionShowPanel(final String triggerValue, final JComponent componentToReveal) {
    componentRevealMap.put(triggerValue, componentToReveal);
    return this;
  }

  /**
   * Builds the swing component.
   */
  public JComponent build() {

    final JPanel parent = JPanelBuilder.builder()
        .verticalBoxLayout()
        .build();

    final JComboBox<String> comboBox = comboBoxBuilder.build();

    if (label != null) {
      parent.add(JPanelBuilder.builder()
          .horizontalBoxLayout()
          .addHorizontalGlue()
          .addLabel(label)
          .addHorizontalStrut(5)
          .add(comboBox)
          .addHorizontalGlue()
          .build());
    } else {
      parent.add(comboBox);
    }


    for (final Map.Entry<String, JComponent> entry : componentRevealMap.entrySet()) {
      if ((comboBox.getSelectedItem() != null) && comboBox.getSelectedItem().toString().equals(entry.getKey())) {
        parent.add(entry.getValue());
      }
    }


    if ((triggerValueForPanelHide != null) && (comboBox.getSelectedItem() != null)
        && !comboBox.getSelectedItem().toString().equals(triggerValueForPanelHide)) {
      parent.add(componentToHide);
    }
    parent.revalidate();


    comboBox.addItemListener(itemEvent -> {
      for (final Map.Entry<String, JComponent> entry : componentRevealMap.entrySet()) {
        if ((comboBox.getSelectedItem() != null) && comboBox.getSelectedItem().toString().equals(entry.getKey())) {
          parent.add(entry.getValue());
        } else {
          parent.remove(entry.getValue());
        }
      }

      if (triggerValueForPanelHide != null) {
        if ((itemEvent.getItem() != null) && itemEvent.getItem().toString().equals(triggerValueForPanelHide)) {
          parent.remove(componentToHide);
        } else {
          parent.add(componentToHide);
        }
      }
      parent.revalidate();
    });
    return parent;
  }


  /**
   * Convenience method to tie a specific selection to hiding another panel.
   * For example, you could create a list with "none" or "custom" as options,
   * when "none" is selected that could trigger a panel to be hidden.
   */
  public JComboBoxCompositeBuilder onSelectionHidePanel(final String triggerValueForPanelHide,
      final JPanel componentToHide) {
    this.triggerValueForPanelHide = triggerValueForPanelHide;
    this.componentToHide = componentToHide;
    return this;
  }
}
