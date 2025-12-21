package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;

import java.awt.event.KeyEvent;
import java.util.function.Consumer;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.swing.key.binding.KeyCode;

class JMenuItemCheckBoxBuilderTest {
  @NonNls private static final String TITLE = "title";

  @Test
  void verifyTitle() {
    final var checkBoxMenuItem =
        new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A).actionListener(e -> {}).build();
    assertThat(checkBoxMenuItem.getText(), is(TITLE));
  }

  @Test
  void verifyMnemonic() {
    final var checkBoxMenuItem =
        new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A).actionListener(e -> {}).build();
    assertThat(checkBoxMenuItem.getMnemonic(), is(KeyEvent.VK_A));
  }

  @Test
  void selected() {
    var checkBoxMenuItem =
        new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A).actionListener(e -> {}).build();

    assertThat(checkBoxMenuItem.isSelected(), is(false));

    checkBoxMenuItem =
        new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A)
            .actionListener(e -> {})
            .selected(true)
            .build();

    assertThat(checkBoxMenuItem.isSelected(), is(true));
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  final class VerifyActionHandlingTest {
    @Mock private Consumer<Boolean> booleanConsumer;

    @Test
    void addActionListener() {
      var checkBoxMenuItem =
          new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A).actionListener(e -> {}).build();
      assertThat(checkBoxMenuItem.getActionListeners().length, is(1));

      checkBoxMenuItem =
          new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A)
              .actionListener(selected -> {})
              .selected(true)
              .build();
      assertThat(
          "Adding the first action listener replaces the default swing "
              + "action listener that exists on the component",
          checkBoxMenuItem.getActionListeners().length,
          is(1));
    }

    @Test
    void checkBoxNotSelected() {
      final var checkBoxMenuItem =
          new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A).actionListener(booleanConsumer).build();
      checkBoxMenuItem.getActionListeners()[0].actionPerformed(null);
      verify(booleanConsumer).accept(false);
    }

    @Test
    void checkBoxIsSelected() {
      final var checkBoxMenuItem =
          new JMenuItemCheckBoxBuilder(TITLE, KeyCode.A)
              .selected(true)
              .actionListener(booleanConsumer)
              .build();
      checkBoxMenuItem.getActionListeners()[0].actionPerformed(null);
      verify(booleanConsumer).accept(true);
    }
  }
}
