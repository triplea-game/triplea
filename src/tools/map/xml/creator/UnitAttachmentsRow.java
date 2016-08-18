package tools.map.xml.creator;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


class UnitAttachmentsRow extends DynamicRow {

  private JTextField textFieldAttachmentName;
  private JTextField textFieldValue;

  public UnitAttachmentsRow(final DynamicRowsPanel parentRowPanel, final JPanel stepActionPanel, final String unitName,
      final String attachmentName, final String value) {
    super(attachmentName + "_" + unitName, parentRowPanel, stepActionPanel);

    textFieldAttachmentName = new JTextField(attachmentName);
    Dimension dimension = textFieldAttachmentName.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_LARGE;
    textFieldAttachmentName.setPreferredSize(dimension);
    textFieldAttachmentName.addFocusListener(new FocusListener() {

      String currentValue = attachmentName;

      @Override
      public void focusLost(final FocusEvent e) {
        final String newAttachmentName = textFieldAttachmentName.getText().trim();
        if (!currentValue.equals(newAttachmentName)) {
          final String newUnitAttachmentKey = newAttachmentName + "_" + unitName;
          if (MapXmlHelper.getUnitAttachmentsMap().containsKey(newUnitAttachmentKey)) {
            JOptionPane.showMessageDialog(stepActionPanel,
                "Attachment '" + newAttachmentName + "' already exists for unit '" + unitName + "'.", "Input error",
                JOptionPane.ERROR_MESSAGE);
            parentRowPanel.setDataIsConsistent(false);
            // UI Update
            SwingUtilities.invokeLater(() -> {
              stepActionPanel.revalidate();
              stepActionPanel.repaint();
              textFieldAttachmentName.requestFocus();
            });
            return;
          } else {
            final String oldUnitAttachmentKey = currentValue + "_" + unitName;
            MapXmlHelper.getUnitAttachmentsMap().put(newAttachmentName,
                MapXmlHelper.getUnitAttachmentsMap().get(oldUnitAttachmentKey));
            MapXmlHelper.getUnitAttachmentsMap().remove(oldUnitAttachmentKey);
            currentValue = newAttachmentName;
          }
          parentRowPanel.setDataIsConsistent(true);
        }
      }

      @Override
      public void focusGained(final FocusEvent e) {
        textFieldAttachmentName.selectAll();
      }
    });


    textFieldValue = new JTextField(value);
    dimension = textFieldValue.getPreferredSize();
    dimension.width = INPUT_FIELD_SIZE_SMALL;
    textFieldValue.setPreferredSize(dimension);
    textFieldValue.addFocusListener(new FocusListener() {

      String prevValue = value;

      @Override
      public void focusLost(final FocusEvent e) {
        final String inputText = textFieldValue.getText().trim().toLowerCase();
        try {
          if (Integer.parseInt(inputText) < 0) {
            throw new NumberFormatException();
          }
        } catch (final NumberFormatException nfe) {
          JOptionPane.showMessageDialog(stepActionPanel, "'" + inputText + "' is no integer value.", "Input error",
              JOptionPane.ERROR_MESSAGE);
          textFieldValue.setText(prevValue);
          SwingUtilities.invokeLater(() -> {
            textFieldValue.updateUI();
            textFieldValue.requestFocus();
            textFieldValue.selectAll();
          });
          return;
        }
        prevValue = inputText;

        // everything is okay with the new value name, lets rename everything
        final List<String> newValues = MapXmlHelper.getUnitAttachmentsMap().get(currentRowName);
        newValues.set(1, inputText);
      }

      @Override
      public void focusGained(final FocusEvent e) {
        textFieldValue.selectAll();
      }
    });
  }

  @Override
  protected ArrayList<JComponent> getComponentList() {
    final ArrayList<JComponent> componentList = new ArrayList<>();
    componentList.add(textFieldAttachmentName);
    componentList.add(textFieldValue);
    return componentList;
  }

  @Override
  public void addToParentComponent(final JComponent parent, final GridBagConstraints gbc_template) {
    parent.add(textFieldAttachmentName, gbc_template);

    final GridBagConstraints gbc_tValue = (GridBagConstraints) gbc_template.clone();
    gbc_tValue.gridx = 1;
    parent.add(textFieldValue, gbc_tValue);

    final GridBagConstraints gridBadConstButtonRemove = (GridBagConstraints) gbc_template.clone();
    gridBadConstButtonRemove.gridx = 2;
    parent.add(buttonRemovePerRow, gridBadConstButtonRemove);
  }

  @Override
  protected void adaptRowSpecifics(final DynamicRow newRow) {
    final UnitAttachmentsRow newRowPlayerAndAlliancesRow = (UnitAttachmentsRow) newRow;
    this.textFieldAttachmentName.setText(newRowPlayerAndAlliancesRow.textFieldAttachmentName.getText());
    this.textFieldValue.setText(newRowPlayerAndAlliancesRow.textFieldValue.getText());
  }

  @Override
  protected void removeRowAction() {
    MapXmlHelper.getUnitAttachmentsMap().remove(currentRowName);
  }
}
