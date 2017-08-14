package swinglib;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
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
        .build();
    assertThat(dialog.getTitle(), is("title"));
  }

  private static void assumeHeadedGraphicsEnvironment() {
    assumeFalse("requires headed graphics environment", GraphicsEnvironment.isHeadless());
  }

  @Test
  public void checkParentFrame() {
    assumeHeadedGraphicsEnvironment();

    final JFrame parentFrame = new JFrame();

    final JDialog dialog = JDialogBuilder.builder()
        .contents(new JPanel())
        .parentFrame(parentFrame)
        .title("title")
        .build();

    assertThat(dialog.getParent(), is(sameInstance(parentFrame)));
  }

  @Test(expected = NullPointerException.class)
  public void contentsIsRequired() {
    JDialogBuilder.builder()
        .title("title")
        .build();
  }

  @Test
  public void parentFrameIsNotRequired() {
    assumeHeadedGraphicsEnvironment();

    final JDialog dialog = JDialogBuilder.builder()
        .contents(new JPanel())
        .title("title")
        .build();

    assertThat(dialog.getParent(), is(not(nullValue())));
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    JDialogBuilder.builder()
        .contents(new JPanel())
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void contentsCanNotBeNull() {
    JDialogBuilder.builder().contents(null);
  }

  @Test(expected = NullPointerException.class)
  public void parentFrameCanNotBeNull() {
    JDialogBuilder.builder().parentFrame(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void titleCanNotBeEmpty() {
    JDialogBuilder.builder().title(" ");
  }
}
