package swinglib;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComboBox;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import games.strategy.triplea.settings.ClientSetting;


@ExtendWith(MockitoExtension.class)
public class JComboBoxBuilderTest extends AbstractClientSettingTestCase {
  @Mock
  private ItemEvent mockItemEvent;

  @Test
  public void buildInvalidEmptyOptionSet() {
    assertThrows(IllegalArgumentException.class, () -> JComboBoxBuilder.builder()
        .menuOptions()
        .build());
  }

  @Test
  public void builderNoMenuOptionSpecified() {
    assertThrows(IllegalStateException.class, JComboBoxBuilder.builder()::build);
  }

  @Test
  public void basicBuilderWithMenuOptions() {
    final JComboBox<String> box = JComboBoxBuilder.builder()
        .menuOptions("option 1", "option 2", "option 3")
        .build();

    MatcherAssert.assertThat(box.getSelectedIndex(), Is.is(0));
    MatcherAssert.assertThat(box.getItemCount(), Is.is(3));
    MatcherAssert.assertThat(box.getItemAt(0), Is.is("option 1"));
    MatcherAssert.assertThat(box.getItemAt(1), Is.is("option 2"));
    MatcherAssert.assertThat(box.getItemAt(2), Is.is("option 3"));
    MatcherAssert.assertThat(box.getSelectedItem(), Is.is("option 1"));
  }

  @Test
  public void itemListener() {

    final AtomicInteger triggerCount = new AtomicInteger(0);

    final String secondOption = "option 2";
    final JComboBox<String> box = JComboBoxBuilder.builder()
        .menuOptions("option 1", secondOption, "option 3")
        .itemListener(value -> {
          if (value.equals(secondOption)) {
            triggerCount.incrementAndGet();
          }
        }).build();
    box.setSelectedIndex(1);
    MatcherAssert.assertThat(triggerCount.get(), Is.is(1));
  }

  @Test
  public void useLastSelectionAsFutureDefaultWithStringKey() {
    final String settingKey = "settingKey";
    ClientSetting.save(settingKey, "");
    MatcherAssert.assertThat("establish a preconditions state to avoid pollution between runs",
        ClientSetting.load(settingKey), Is.is(""));

    final JComboBox<String> box = JComboBoxBuilder.builder()
        .menuOptions("option 1", "option 2")
        .useLastSelectionAsFutureDefault(settingKey)
        .build();
    Mockito.when(mockItemEvent.getStateChange()).thenReturn(ItemEvent.SELECTED);
    Mockito.when(mockItemEvent.getSource()).thenReturn(box);
    final String valueFromEvent = "test value";
    Mockito.when(mockItemEvent.getItem()).thenReturn(valueFromEvent);
    Arrays.stream(box.getItemListeners())
        .forEach(listener -> listener.itemStateChanged(mockItemEvent));

    MatcherAssert.assertThat(
        "selecting the 1st index should be 'option 2', we expect that to "
            + "have been flushed to client settings",
        ClientSetting.load(settingKey), Is.is(valueFromEvent));
  }

  @Test
  public void useLastSelectionAsFutureDefaultWithClientKey() {
    ClientSetting.TEST_SETTING.saveAndFlush("");

    final JComboBox<String> box = JComboBoxBuilder.builder()
        .menuOptions("option 1", "option 2")
        .useLastSelectionAsFutureDefault(ClientSetting.TEST_SETTING)
        .build();

    Mockito.when(mockItemEvent.getStateChange()).thenReturn(ItemEvent.SELECTED);
    Mockito.when(mockItemEvent.getSource()).thenReturn(box);
    final String valueFromEvent = "test value";
    Mockito.when(mockItemEvent.getItem()).thenReturn(valueFromEvent);
    Arrays.stream(box.getItemListeners())
        .forEach(listener -> listener.itemStateChanged(mockItemEvent));

    MatcherAssert.assertThat("We expect any selected value to have been bound to our test setting",
        ClientSetting.TEST_SETTING.value(), Is.is(valueFromEvent));
  }
}
