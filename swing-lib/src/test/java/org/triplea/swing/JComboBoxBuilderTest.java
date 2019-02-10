package org.triplea.swing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JComboBoxBuilderTest {
  @Test
  void builderNoItemsSpecified() {
    assertThrows(IllegalStateException.class, JComboBoxBuilder.builder(Object.class)::build);
  }

  @Test
  void basicBuilderWithItems() {
    final JComboBox<String> box = JComboBoxBuilder.builder(String.class)
        .item("option 1")
        .item("option 2")
        .item("option 3")
        .build();

    MatcherAssert.assertThat(box.getSelectedIndex(), Is.is(0));
    MatcherAssert.assertThat(box.getItemCount(), Is.is(3));
    MatcherAssert.assertThat(box.getItemAt(0), Is.is("option 1"));
    MatcherAssert.assertThat(box.getItemAt(1), Is.is("option 2"));
    MatcherAssert.assertThat(box.getItemAt(2), Is.is("option 3"));
    MatcherAssert.assertThat(box.getSelectedItem(), Is.is("option 1"));
  }

  @Test
  void itemSelectedAction() {
    final AtomicInteger triggerCount = new AtomicInteger(0);
    final String secondOption = "option 2";
    final JComboBox<String> box = JComboBoxBuilder.builder(String.class)
        .item("option 1")
        .item(secondOption)
        .item("option 3")
        .itemSelectedAction(value -> {
          if (value.equals(secondOption)) {
            triggerCount.incrementAndGet();
          }
        }).build();
    box.setSelectedIndex(1);
    MatcherAssert.assertThat(triggerCount.get(), Is.is(1));
  }

  @Nested
  final class EnableAutoCompleteTest {
    @Test
    void shouldChangeComboBoxEditorComponentDocumentType() {
      final JComboBox<Object> comboBox = JComboBoxBuilder.builder(Object.class)
          .item(new Object())
          .enableAutoComplete()
          .build();

      final Component editorComponent = comboBox.getEditor().getEditorComponent();
      assertThat(editorComponent, is(instanceOf(JTextComponent.class)));
      assertThat(((JTextComponent) editorComponent).getDocument(), is(instanceOf(AutoCompletion.class)));
    }
  }

  @Nested
  final class SelectedItemTest {
    @Test
    void shouldSetSelectedItem() {
      final JComboBox<String> comboBox = JComboBoxBuilder.builder(String.class)
          .items(Arrays.asList("A", "B", "C"))
          .selectedItem("B")
          .build();

      assertThat(comboBox.getSelectedItem(), is("B"));
    }
  }

  @Nested
  final class NullableSelectedItemTest {
    @Test
    void shouldSetSelectedItemWhenNonNull() {
      final JComboBox<String> comboBox = JComboBoxBuilder.builder(String.class)
          .items(Arrays.asList("A", "B", "C"))
          .nullableSelectedItem("B")
          .build();

      assertThat(comboBox.getSelectedItem(), is("B"));
    }

    @Test
    void shouldNotSetSelectedItemWhenNull() {
      final JComboBox<String> comboBox = JComboBoxBuilder.builder(String.class)
          .items(Arrays.asList("A", "B", "C"))
          .nullableSelectedItem(null)
          .build();

      assertThat(comboBox.getSelectedItem(), is("A"));
    }
  }
}
