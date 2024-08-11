package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import javax.swing.JCheckBox;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JCheckBoxBuilderTest {

  @NonNls
  private static final String TITLE = "title-text";
  @Mock private SettingPersistence settingPersistence;
  @Mock private Consumer<Boolean> actionListener;

  @Test
  void verifyDefaults() {
    final JCheckBox checkBox = new JCheckBoxBuilder().build();

    assertThat(checkBox.isEnabled(), is(true));
    assertThat(checkBox.isSelected(), is(true));
  }

  @Nested
  class Selected {

    @Test
    void selected() {
      assertThat(new JCheckBoxBuilder().selected(false).build().isSelected(), is(false));

      assertThat(new JCheckBoxBuilder().selected(true).build().isSelected(), is(true));
    }

    @Test
    void boundToSettingReturningFalse() {
      final JCheckBox checkBox = givenBoxBoundToSetting(false);

      final boolean result = checkBox.isSelected();

      assertThat(result, is(false));
    }

    private JCheckBox givenBoxBoundToSetting(final boolean settingValue) {
      when(settingPersistence.getSetting()).thenReturn(settingValue);
      return new JCheckBoxBuilder().bind(settingPersistence).build();
    }

    @Test
    void boundToSettingReturningTrue() {
      final JCheckBox checkBox = givenBoxBoundToSetting(true);

      final boolean result = checkBox.isSelected();

      assertThat(result, is(true));
    }
  }

  @Nested
  class ActionListener {
    @Test
    void firesActionListenerWithSelectedCheckBox() {
      final JCheckBox checkBox = new JCheckBoxBuilder().actionListener(actionListener).build();

      checkBox.getActionListeners()[0].actionPerformed(null);

      verify(actionListener).accept(true);
    }

    @Test
    void firesActionListenerWithUnSelectedCheckBox() {
      final JCheckBox checkBox =
          new JCheckBoxBuilder().selected(false).actionListener(actionListener).build();

      checkBox.getActionListeners()[0].actionPerformed(null);

      verify(actionListener).accept(false);
    }

    @Test
    void actionListenerSavesSettingsWhenBound() {
      final JCheckBox checkBox = givenBoundCheckBoxWithSetting(false);

      checkBox.getActionListeners()[0].actionPerformed(null);

      verify(settingPersistence).saveSetting(false);
    }

    private JCheckBox givenBoundCheckBoxWithSetting(final boolean setting) {
      when(settingPersistence.getSetting()).thenReturn(setting);
      return new JCheckBoxBuilder().bind(settingPersistence).actionListener(actionListener).build();
    }

    @Test
    void actionListenerSavesSettingsWhenBoundPositiveCase() {
      final JCheckBox checkBox = givenBoundCheckBoxWithSetting(true);

      checkBox.getActionListeners()[0].actionPerformed(null);

      verify(settingPersistence).saveSetting(true);
    }
  }

  @Nested
  class Title {
    @Test
    void titleIsSet() {
      final JCheckBox checkBox = new JCheckBoxBuilder(TITLE).build();

      final String result = checkBox.getText();

      assertThat(result, is(TITLE));
    }

    @Test
    void noTitle() {
      final JCheckBox checkBox = new JCheckBoxBuilder().build();

      final String result = checkBox.getText();

      assertThat(result, emptyString());
    }
  }
}
