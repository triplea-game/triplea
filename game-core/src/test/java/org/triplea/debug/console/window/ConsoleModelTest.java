package org.triplea.debug.console.window;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsoleModelTest extends AbstractClientSettingTestCase {

  private static final String SAMPLE_DATA = "Mensa, brodium, et epos.";

  @Mock private ConsoleWindow defaultConsole;

  @InjectMocks private ConsoleModel consoleModel;

  @Mock private Consumer<String> mockClipboard;

  @Test
  void getCurrentLogLevel() {
    assertThat(
        "By default we should have a 'normal' log level",
        ConsoleModel.getCurrentLogLevel(),
        is(ConsoleModel.LogLevelItem.NORMAL.getLabel()));

    ClientSetting.loggingVerbosity.setValue(ConsoleModel.LogLevelItem.DEBUG.getLevel().getName());
    assertThat(ConsoleModel.getCurrentLogLevel(), is(ConsoleModel.LogLevelItem.DEBUG.getLabel()));

    ClientSetting.loggingVerbosity.setValue(ConsoleModel.LogLevelItem.NORMAL.getLevel().getName());
    assertThat(ConsoleModel.getCurrentLogLevel(), is(ConsoleModel.LogLevelItem.NORMAL.getLabel()));
  }

  @Test
  void getLogLevelOptions() {
    assertThat(ConsoleModel.getLogLevelOptions(), hasSize(2));
    assertThat(
        ConsoleModel.getLogLevelOptions(), hasItem(ConsoleModel.LogLevelItem.NORMAL.getLabel()));
    assertThat(
        ConsoleModel.getLogLevelOptions(), hasItem(ConsoleModel.LogLevelItem.DEBUG.getLabel()));
  }

  @Test
  void copyToClipboardAction() {
    when(defaultConsole.readText()).thenReturn(SAMPLE_DATA);

    consoleModel.copyToClipboardAction(defaultConsole);

    verify(mockClipboard).accept(SAMPLE_DATA);
  }

  @Test
  void clearAction() {
    ConsoleModel.clearAction(defaultConsole);

    verify(defaultConsole).setText("");
  }

  @Test
  void enumerateThreadsAction() {
    ConsoleModel.enumerateThreadsAction(defaultConsole);

    verify(defaultConsole).append(anyString());
  }

  @Test
  void memoryButtonAction() {
    ConsoleModel.memoryAction(defaultConsole);

    verify(defaultConsole).append(anyString());
  }

  @Test
  void propertiesButtonAction() {
    ConsoleModel.propertiesAction(defaultConsole);

    verify(defaultConsole).append(DebugUtils.getProperties());
  }

  @Test
  void setLogLevel() {
    ConsoleModel.setLogLevel(ConsoleModel.LogLevelItem.NORMAL.getLabel());
    assertThat(ClientSetting.loggingVerbosity.getValueOrThrow(), is(Level.INFO.getName()));

    ConsoleModel.setLogLevel(ConsoleModel.LogLevelItem.DEBUG.getLabel());
    assertThat(ClientSetting.loggingVerbosity.getValueOrThrow(), is(Level.ALL.getName()));
  }
}
