package games.strategy.triplea.settings;

import java.awt.Color;
import java.io.Serializable;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import games.strategy.ui.SwingComponents;

abstract class SelectionComponent implements Serializable {
  private static final long serialVersionUID = -2224094425526210088L;

  static SelectionComponent intValueRange(final int lo, final int hi) {
    final JTextField component = new JTextField(String.valueOf(hi).length());

    return new SelectionComponent() {
      private static final long serialVersionUID = 8195633990481917808L;

      @Override
      JComponent getJComponent() {
        component.setToolTipText(validValueDescription());

        SwingComponents.addTextFieldFocusLostListener(component, () -> {
          if (isValid()) {
            clearError();
          } else {
            indicateError();
          }
        });

        return component;
      }

      @Override
      boolean isValid() {
        final String value = component.getText();

        try {
          final int intValue = Integer.parseInt(value);
          return intValue >= lo && intValue <= hi;
        } catch (final NumberFormatException e) {
          return false;
        }
      }

      @Override
      String validValueDescription() {
        return "Number between " + lo + " and " + hi;
      }

      @Override
      void setValue(final String valueToSet) {
        component.setText(valueToSet);
      }

      @Override
      String readValue() {
        return component.getText();
      }

      @Override
      void indicateError() {
        component.setBackground(Color.RED);
      }

      @Override
      void clearError() {
        component.setBackground(Color.WHITE);
      }
    };
  }

  /**
   * Radio button
   */
  static SelectionComponent booleanValue(final boolean initialValue) {
    final JRadioButton yesButton = new JRadioButton("True");
    yesButton.setSelected(initialValue);
    final JRadioButton noButton = new JRadioButton("False");
    noButton.setSelected(!initialValue);
    SwingComponents.createButtonGroup(yesButton, noButton);

    return new ValidInputOnlySelectionComponent() {
      private static final long serialVersionUID = 6104513062312556269L;

      @Override
      JComponent getJComponent() {
        return SwingComponents.horizontalJPanel(yesButton, noButton);
      }

      @Override
      void setValue(final String valueToSet) {
        if (Boolean.valueOf(valueToSet)) {
          yesButton.setSelected(true);
        } else {
          noButton.setSelected(true);
        }
      }

      @Override
      String readValue() {
        if (yesButton.isSelected()) {
          return String.valueOf(true);
        } else {
          return String.valueOf(false);
        }
      }
    };
  }

  /**
   * Folder selection prompt, returns nothing when user cancels or closes window
   */
  static SelectionComponent folderPath() {
    final int expectedLength = 20;
    final JTextField field = new JTextField(expectedLength);
    field.setEditable(false);

    final JButton button = SwingComponents.newJButton(
        "Select",
        action -> SwingComponents.showJFileChooserForFolders().ifPresent(file -> {
          field.setText(file.getAbsolutePath());
        }));

    return new ValidInputOnlySelectionComponent() {
      private static final long serialVersionUID = -1775099967925891332L;

      @Override
      JComponent getJComponent() {
        final JPanel panel = SwingComponents.newJPanelWithHorizontalBoxLayout();
        panel.add(field);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(button);
        return panel;
      }

     @Override
      void setValue(final String valueToSet) {
        field.setText(valueToSet);
      }

      @Override
      String readValue() {
        return field.getText();
      }
    };
  }

  abstract JComponent getJComponent();

  abstract boolean isValid();

  abstract String validValueDescription();

  abstract void setValue(String valueToSet);

  abstract String readValue();

  /**
   * UI component should update to show an error, eg: background turn red.
   */
  abstract void indicateError();

  /**
   * UI component should revert back to a normal state, clearing any changes from {@code indicateError}
   */
  abstract void clearError();


  private abstract static class ValidInputOnlySelectionComponent extends SelectionComponent {
    private static final long serialVersionUID = 6848335387637901069L;

    @Override
    void indicateError() {
      // no-op, component only allows valid selections
    }

    @Override
    void clearError() {
      // also a no-op
    }

    @Override
    boolean isValid() {
      return true;
    }

    @Override
    String validValueDescription() {
      return "";
    }
  }
}

