package org.triplea.swing.jpanel;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.junit.jupiter.api.Test;

class JPanelBuilderTest {

  @Test
  void horizontalAlignmentCenter() {
    final JPanel panel =
        new JPanelBuilder().horizontalAlignmentCenter().add(new JLabel("")).build();
    assertThat(panel.getAlignmentX()).isEqualTo(JComponent.CENTER_ALIGNMENT);
  }

  @Test
  void testAddComponent() {
    final JLabel label = new JLabel("hi");

    final JPanel panel = new JPanelBuilder().add(label).build();

    assertThat(List.of(panel.getComponents()))
        .as("Panel children should contain the label we added.")
        .containsExactly(label);
  }

  @Test
  void defaultLayoutIsFlowLayout() {
    assertThat(new JPanelBuilder().add(new JLabel()).build().getLayout())
        .isInstanceOf(FlowLayout.class);
  }

  @Test
  void testLayouts() {
    final GridLayout result = (GridLayout) new JPanelBuilder().gridLayout(1, 2).build().getLayout();
    assertThat(result.getRows()).isEqualTo(1);
    assertThat(result.getColumns()).isEqualTo(2);

    assertThat(new JPanelBuilder().gridBagLayout().build().getLayout())
        .isInstanceOf(GridBagLayout.class);

    assertThat(new JPanelBuilder().flowLayout().build().getLayout()).isInstanceOf(FlowLayout.class);

    assertThat(new JPanelBuilder().borderLayout().build().getLayout())
        .isInstanceOf(BorderLayout.class);
  }

  @Test
  void emptyBorderWithSingleWidth() {
    final int borderWidth = 100;
    final JPanel panel = new JPanelBuilder().border(borderWidth).add(new JLabel()).build();
    assertThat(panel.getBorder()).isInstanceOf(EmptyBorder.class);
    final Insets insets = panel.getBorder().getBorderInsets(panel);
    assertThat(insets.top).isEqualTo(borderWidth);
    assertThat(insets.bottom).isEqualTo(borderWidth);
    assertThat(insets.left).isEqualTo(borderWidth);
    assertThat(insets.right).isEqualTo(borderWidth);
  }

  @Test
  void emptyBorderWithIndependentWidths() {
    final JPanel panel = new JPanelBuilder().border(1, 2, 3, 4).add(new JLabel()).build();

    assertThat(panel.getBorder()).isInstanceOf(EmptyBorder.class);

    final Insets insets = panel.getBorder().getBorderInsets(panel);
    assertThat(insets.top).isEqualTo(1);
    assertThat(insets.left).isEqualTo(2);
    assertThat(insets.bottom).isEqualTo(3);
    assertThat(insets.right).isEqualTo(4);
  }

  @Test
  void addLabel() {
    final String labelText = "abc";

    final JPanel panel = new JPanelBuilder().add(new JLabel(labelText)).build();

    assertThat(panel.getComponents().length).isEqualTo(1);
    assertThat(panel.getComponents()[0]).isInstanceOf(JLabel.class);
    assertThat(((JLabel) panel.getComponents()[0]).getText()).isEqualTo(labelText);
  }
}
