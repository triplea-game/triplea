package swinglib;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.junit.Test;

public class JPanelBuilderTest {

  @Test
  public void testBuild() {
    final JPanel panel = JPanelBuilder.builder()
        .build();
  }
}
