package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.triplea.swing.key.binding.KeyCode;

@NonNls
class JMenuBuilderTest {

  private static final String TITLE = "title";
  private static final String MENU_ITEM_TITLE = "menu-item-title";
  private static final KeyCode MENU_MNEMONIC = KeyCode.M;

  @Test
  void verifyTitle() {
    final JMenu menu = new JMenuBuilder(TITLE, MENU_MNEMONIC).build();
    assertThat(menu.getText(), is(TITLE));
  }

  @Test
  void verifyMnemonic() {
    final JMenu menu = new JMenuBuilder(TITLE, KeyCode.M).build();
    assertThat(menu.getMnemonic(), is(KeyCode.M));
  }

  @Test
  void addMenuItem() {
    final JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC).addMenuItem(new JMenuItem(MENU_ITEM_TITLE)).build();

    assertThat(menu.getItemCount(), is(1));
    assertThat(menu.getItem(0).getText(), is(MENU_ITEM_TITLE));
  }

  @Test
  void addMenuItemIf() {
    JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC)
            .addMenuItemIf(false, MENU_ITEM_TITLE, KeyCode.A, () -> {})
            .build();

    assertThat(menu.getItemCount(), is(0));

    menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC)
            .addMenuItemIf(true, MENU_ITEM_TITLE, KeyCode.A, () -> {})
            .build();
    assertThat(menu.getItemCount(), is(1));
  }

  @Test
  void testAddMenuItem() {
    final JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC)
            .addMenuItem(MENU_ITEM_TITLE, KeyCode.A, () -> {})
            .build();
    assertThat(menu.getItemCount(), is(1));
    assertThat(menu.getItem(0).getText(), is(MENU_ITEM_TITLE));
    assertThat(menu.getItem(0).getMnemonic(), is(KeyCode.A));
  }
}
