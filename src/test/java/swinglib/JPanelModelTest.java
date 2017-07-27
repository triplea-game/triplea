package swinglib;

import javax.swing.JPanel;

import org.junit.Test;

public class JPanelModelTest {

  @Test
  public void testBuild() {
    // button action will be to add one to our integer, we'll fire the button action and verify we get the +1
    final JPanel panel = JPanelModel.builder()
        .swingComponent();
  }
}
