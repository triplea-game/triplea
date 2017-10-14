package swinglib;

import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComboBox;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.triplea.settings.ClientSetting;


@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class JComboBoxBuilderTest {
  @Mock
  private ItemEvent mockItemEvent;

  @Before
  public void setUp() {}

  @Test(expected = IllegalArgumentException.class)
  public void buildInvalidEmptyOptionSet() {
    JComboBoxBuilder.builder()
        .menuOptions()
        .build();
  }

  @Test(expected = IllegalStateException.class)
  public void builderNoMenuOptionSpecified() {
    JComboBoxBuilder.builder()
        .build();
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
    MatcherAssert.assertThat(
        // TODO: document which item listener is attached by default, why is this not zero?
        box.getItemListeners().length, Is.is(1));
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

    MatcherAssert.assertThat(
        "There is an existing listener (TODO: learn more about this), and there"
            + "is the one we added, so we expect 2 in total at this point",
        box.getItemListeners().length, Is.is(2));

    box.setSelectedIndex(1);
    MatcherAssert.assertThat(triggerCount.get(), Is.is(1));
  }

  @Test
  public void useLastSelectionAsFutureDefaultWithStringKey() throws Exception {
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
  public void useLastSelectionAsFutureDefaultWithClientKey() throws Exception {
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
