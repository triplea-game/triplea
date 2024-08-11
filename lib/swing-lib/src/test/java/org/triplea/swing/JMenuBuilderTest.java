package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;

@NonNls
class JMenuBuilderTest {

  private static final String TITLE = "title";
  private static final String MENU_ITEM_TITLE = "menu-item-title";
  private static final char MENU_MNEMONIC = 'M';

  @Test
  void verifyTitle() {
    final JMenu menu = new JMenuBuilder(TITLE, MENU_MNEMONIC).build();
    assertThat(menu.getText(), is(TITLE));
  }

  @Test
  void verifyMnemonic() {
    final JMenu menu = new JMenuBuilder(TITLE, 'M').build();
    assertThat(menu.getMnemonic(), is(KeyEvent.VK_M));
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
            .addMenuItemIf(false, MENU_ITEM_TITLE, 'a', () -> {})
            .build();

    assertThat(menu.getItemCount(), is(0));

    menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC)
            .addMenuItemIf(true, MENU_ITEM_TITLE, 'a', () -> {})
            .build();
    assertThat(menu.getItemCount(), is(1));
  }

  @Test
  void testAddMenuItem() {
    final JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC).addMenuItem(MENU_ITEM_TITLE, 'a', () -> {}).build();
    assertThat(menu.getItemCount(), is(1));
    assertThat(menu.getItem(0).getText(), is(MENU_ITEM_TITLE));
    assertThat(menu.getItem(0).getMnemonic(), is(KeyEvent.VK_A));
  }
}
