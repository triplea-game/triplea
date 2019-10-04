package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JMenuItemBuilderTest {
  private static final String TITLE = "title";
  private static final KeyCode MNEMONIC = KeyCode.X;

  @Mock private Runnable actionListener;

  @Test
  void verifyTitle() {
    final JMenuItem menuItem =
        new JMenuItemBuilder(TITLE, MNEMONIC).actionListener(() -> {}).build();
    assertThat(menuItem.getText(), is(TITLE));
  }

  @Test
  void verifyMnemonic() {
    final JMenuItem menuItem =
        new JMenuItemBuilder(TITLE, MNEMONIC).actionListener(() -> {}).build();
    assertThat(menuItem.getMnemonic(), is(KeyEvent.VK_X));
  }

  @Test
  void actionListener() {
    final JMenuItem menuItem =
        new JMenuItemBuilder(TITLE, MNEMONIC).actionListener(actionListener).build();
    menuItem.getActionListeners()[0].actionPerformed(null);

    assertThat(menuItem.getActionListeners().length, Is.is(1));
    verify(actionListener).run();
  }
}
