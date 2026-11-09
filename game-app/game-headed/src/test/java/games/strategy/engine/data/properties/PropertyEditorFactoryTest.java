package games.strategy.engine.data.properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PropertyEditorFactoryTest {

  @Test
  void booleanEditorWritesSelectionToProperty() {
    final BooleanProperty property = new BooleanProperty("name", "description", false);

    final JCheckBox editor = (JCheckBox) PropertyEditorFactory.newEditorComponent(property);
    editor.doClick();

    assertThat(property.getValue()).isTrue();
  }

  @Test
  void stringEditorWritesTextToPropertyOnAction() {
    final StringProperty property = new StringProperty("name", "description", "old");

    final JTextField editor = (JTextField) PropertyEditorFactory.newEditorComponent(property);
    editor.setText("new");
    editor.postActionEvent();

    assertThat(property.getValue()).isEqualTo("new");
  }

  @Test
  void viewComponentIsDisabled() {
    final BooleanProperty property = new BooleanProperty("name", "description", false);

    final JComponent view = PropertyEditorFactory.newViewComponent(property);

    assertThat(view.isEnabled()).isFalse();
  }

  @Test
  void mapPropertyIsEditedThroughNestedProperties() {
    final MapProperty<Boolean> property =
        new MapProperty<>("name", "description", Map.of("key", true));

    assertThat(PropertyEditorFactory.newEditorComponent(property)).isInstanceOf(PropertiesUi.class);
  }

  @Test
  void unknownPropertyTypeIsRejected() {
    final IEditableProperty<?> property = Mockito.mock(IEditableProperty.class);

    assertThrows(
        IllegalArgumentException.class, () -> PropertyEditorFactory.newEditorComponent(property));
  }
}
