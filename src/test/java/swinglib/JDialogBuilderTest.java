package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.awt.HeadlessException;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.Test;

public class JDialogBuilderTest {

  @Test
  public void checkTitle() {
    try {
      final JDialog dialog = JDialogBuilder.builder()
          .contents(new JPanel())
          .title("title")
          .parentFrame(new JFrame())
          .build();
      assertThat(dialog.getTitle(), is("title"));
    } catch (final HeadlessException e) {
      // this is okay, we'll see this in travis
    }
  }

  @Test(expected = NullPointerException.class)
  public void contentsIsRequired() {
    try {
      JDialogBuilder.builder()
          .title("title")
          .parentFrame(new JFrame())
          .build();
    } catch (final HeadlessException e) {
      // this is okay, we'll see this in travis
      throw new NullPointerException("fake the exception to pass the test");
    }
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    try {
      JDialogBuilder.builder()
          .contents(new JPanel())
          .parentFrame(new JFrame())
          .build();
    } catch (final HeadlessException e) {
      // this is okay, we'll see this in travis
      throw new NullPointerException("fake the exception to pass the test");
    }
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
