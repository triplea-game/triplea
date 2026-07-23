package org.triplea.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.awt.Toolkit;
import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.swing.key.binding.KeyCode;

class JMenuItemBuilderTest {

  private static final String MENU_ITEM_TITLE = "Test Menu Item";

  private JMenuItemBuilder menuItemBuilder;
  private Runnable runnable;

  @BeforeEach
  void setUp() {
    runnable = mock(Runnable.class);
    menuItemBuilder = new JMenuItemBuilder(MENU_ITEM_TITLE, KeyCode.E).actionListener(runnable);
  }

  @Test
  void buildCreatesMenuItemWithKeyCode() {
    JMenuItem menuItem = menuItemBuilder.build();
    assertEquals(MENU_ITEM_TITLE, menuItem.getText());
    assertEquals(KeyCode.E.getInputEventCode(), menuItem.getMnemonic());
    assertTrue(menuItem.isEnabled());
    menuItem.doClick();
    verify(runnable).run();
  }

  @Test
  void buildRadioAddsButtonToGroup() {
    ButtonGroup buttonGroup = new ButtonGroup();
    JRadioButtonMenuItem radioButtonMenuItem = menuItemBuilder.buildRadio(buttonGroup);
    assertEquals(MENU_ITEM_TITLE, radioButtonMenuItem.getText());
    assertFalse(radioButtonMenuItem.isSelected());
    assertTrue(radioButtonMenuItem.isEnabled());
    assertEquals(1, buttonGroup.getButtonCount());
    radioButtonMenuItem.doClick();
    verify(runnable).run();
    assertTrue(radioButtonMenuItem.isSelected());
  }

  @Test
  void acceleratorSetsAccelerator() {
    JMenuItem menuItem = menuItemBuilder.accelerator(KeyCode.F).build();
    KeyStroke expectedKeyStroke =
        KeyStroke.getKeyStroke(
            KeyCode.F.getInputEventCode(), Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    assertEquals(expectedKeyStroke, menuItem.getAccelerator());
    assertEquals(KeyCode.E.getInputEventCode(), menuItem.getMnemonic());
  }

  @Test
  void selectedMarksMenuItemSelected() {
    ButtonGroup buttonGroup = new ButtonGroup();
    JRadioButtonMenuItem radioButtonMenuItem =
        menuItemBuilder.selected(true).buildRadio(buttonGroup);
    assertTrue(radioButtonMenuItem.isSelected());
  }

  @Test
  void disabledDisablesMenuItemAndSetsTooltip() {
    final String disabled_tooltip = "Test Menu Item is disabled for testing purposes";
    JMenuItem menuItem = menuItemBuilder.disabled(disabled_tooltip).build();
    assertEquals(disabled_tooltip, menuItem.getToolTipText());
    assertFalse(menuItem.isEnabled());
    menuItem.doClick();
    verify(runnable, never()).run();
  }
}
