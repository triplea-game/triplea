package org.triplea.swing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JComboBoxBuilderTest {
  @Test
  void builderNoItemsSpecified() {
    assertThrows(IllegalStateException.class, JComboBoxBuilder.builder(Object.class)::build);
  }

  @Test
  void basicBuilderWithItems() {
    final JComboBox<String> box =
        JComboBoxBuilder.builder(String.class)
            .item("option 1")
            .item("option 2")
            .item("option 3")
            .build();

    assertThat(box.getSelectedIndex()).isEqualTo(0);
    assertThat(box.getItemCount()).isEqualTo(3);
    assertThat(box.getItemAt(0)).isEqualTo("option 1");
    assertThat(box.getItemAt(1)).isEqualTo("option 2");
    assertThat(box.getItemAt(2)).isEqualTo("option 3");
    assertThat(box.getSelectedItem()).isEqualTo("option 1");
  }

  @Test
  void itemSelectedAction() {
    final AtomicInteger triggerCount = new AtomicInteger(0);
    final String secondOption = "option 2";
    final JComboBox<String> box =
        JComboBoxBuilder.builder(String.class)
            .item("option 1")
            .item(secondOption)
            .item("option 3")
            .itemSelectedAction(
                value -> {
                  if (value.equals(secondOption)) {
                    triggerCount.incrementAndGet();
                  }
                })
            .build();
    box.setSelectedIndex(1);
    assertThat(triggerCount.get()).isEqualTo(1);
  }

  @Nested
  final class EnableAutoCompleteTest {
    @Test
    void shouldChangeComboBoxEditorComponentDocumentType() {
      final JComboBox<Object> comboBox =
          JComboBoxBuilder.builder(Object.class).item(new Object()).enableAutoComplete().build();

      final Component editorComponent = comboBox.getEditor().getEditorComponent();
      assertThat(editorComponent).isInstanceOf(JTextComponent.class);
      assertThat(((JTextComponent) editorComponent).getDocument())
          .isInstanceOf(AutoCompletion.class);
    }
  }

  @Nested
  final class SelectedItemTest {
    @Test
    void shouldSetSelectedItem() {
      final JComboBox<String> comboBox =
          JComboBoxBuilder.builder(String.class)
              .items(List.of("A", "B", "C"))
              .selectedItem("B")
              .build();

      assertThat(comboBox.getSelectedItem()).isEqualTo("B");
    }
  }

  @Nested
  final class NullableSelectedItemTest {
    @Test
    void shouldSetSelectedItemWhenNonNull() {
      final JComboBox<String> comboBox =
          JComboBoxBuilder.builder(String.class)
              .items(List.of("A", "B", "C"))
              .nullableSelectedItem("B")
              .build();

      assertThat(comboBox.getSelectedItem()).isEqualTo("B");
    }

    @Test
    void shouldNotSetSelectedItemWhenNull() {
      final JComboBox<String> comboBox =
          JComboBoxBuilder.builder(String.class)
              .items(List.of("A", "B", "C"))
              .nullableSelectedItem(null)
              .build();

      assertThat(comboBox.getSelectedItem()).isEqualTo("A");
    }
  }
}
