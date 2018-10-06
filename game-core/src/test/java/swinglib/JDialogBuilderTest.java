package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JDialogBuilderTest {

  @BeforeAll
  static void skipIfHeadless() {
    assumeFalse(GraphicsEnvironment.isHeadless());
  }

  @Test
  public void checkTitle() {
    final JDialog dialog = JDialogBuilder.builder()
        .contents(new JPanel())
        .title("title")
        .build();
    assertThat(dialog.getTitle(), is("title"));
  }

  @Test
  public void checkParentFrame() {
    final JFrame parentFrame = new JFrame();

    final JDialog dialog = JDialogBuilder.builder()
        .contents(new JPanel())
        .parentFrame(parentFrame)
        .title("title")
        .build();

    assertThat(dialog.getParent(), is(sameInstance(parentFrame)));
  }

  @Test
  public void contentsIsRequired() {
    assertThrows(NullPointerException.class, () -> JDialogBuilder.builder()
        .title("title")
        .build());
  }

  @Test
  public void titleIsRequired() {
    assertThrows(NullPointerException.class, () -> JDialogBuilder.builder()
        .contents(new JPanel())
        .build());
  }

  @Test
  public void contentsCanNotBeNull() {
    assertThrows(NullPointerException.class, () -> JDialogBuilder.builder().contents(null));
  }

  @Test
  public void parentFrameCanNotBeNull() {
    assertThrows(NullPointerException.class, () -> JDialogBuilder.builder().parentFrame(null));
  }

  @Test
  public void titleCanNotBeEmpty() {
    assertThrows(IllegalArgumentException.class, () -> JDialogBuilder.builder().title(" "));
  }
}
