package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assume.assumeFalse;

import java.awt.GraphicsEnvironment;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.Test;

public class JDialogBuilderTest {

  @Test
  public void checkTitle() {
    assumeHeadedGraphicsEnvironment();

    final JDialog dialog = JDialogBuilder.builder()
        .contents(new JPanel())
        .title("title")
        .parentFrame(new JFrame())
        .build();
    assertThat(dialog.getTitle(), is("title"));
  }

  private static void assumeHeadedGraphicsEnvironment() {
    assumeFalse("requires headed graphics environment", GraphicsEnvironment.isHeadless());
  }

  @Test(expected = NullPointerException.class)
  public void contentsIsRequired() {
    assumeHeadedGraphicsEnvironment();

    JDialogBuilder.builder()
        .title("title")
        .parentFrame(new JFrame())
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    assumeHeadedGraphicsEnvironment();

    JDialogBuilder.builder()
        .contents(new JPanel())
        .parentFrame(new JFrame())
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void titleCanNotBeEmpty() {
    JDialogBuilder.builder().title(" ");
  }

  @Test(expected = NullPointerException.class)
  public void parentCanNotBeNull() {
    JDialogBuilder.builder().parentFrame(null);
  }
}
