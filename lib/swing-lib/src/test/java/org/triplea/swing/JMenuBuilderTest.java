package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;

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
    assertThat(menu.getText()).isEqualTo(TITLE);
  }

  @Test
  void verifyMnemonic() {
    final JMenu menu = new JMenuBuilder(TITLE, KeyCode.M).build();
    assertThat(menu.getMnemonic()).isEqualTo(KeyCode.M.getInputEventCode());
  }

  @Test
  void addMenuItem() {
    final JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC).addMenuItem(new JMenuItem(MENU_ITEM_TITLE)).build();

    assertThat(menu.getItemCount()).isEqualTo(1);
    assertThat(menu.getItem(0).getText()).isEqualTo(MENU_ITEM_TITLE);
  }

  @Test
  void testAddMenuItem() {
    final JMenu menu =
        new JMenuBuilder(TITLE, MENU_MNEMONIC)
            .addMenuItem(MENU_ITEM_TITLE, KeyCode.A, () -> {})
            .build();
    assertThat(menu.getItemCount()).isEqualTo(1);
    assertThat(menu.getItem(0).getText()).isEqualTo(MENU_ITEM_TITLE);
    assertThat(menu.getItem(0).getMnemonic()).isEqualTo(KeyCode.A.getInputEventCode());
  }
}
