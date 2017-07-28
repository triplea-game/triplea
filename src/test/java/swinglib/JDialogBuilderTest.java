package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;

import java.awt.HeadlessException;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.hamcrest.core.Is;

import org.junit.Test;

public class JDialogBuilderTest {

  @Test
  public void checkTitle() {
    try {
      final JDialog dialog = JDialogBuilder.builder()
          .contents(new JPanel())
          .title("title")
          .build();

      assertThat(dialog.getTitle(), Is.is("title"));
    } catch (final HeadlessException e) {
      // this is okay, we'll see this in travis
    }
  }

  @Test(expected = NullPointerException.class)
  public void contentsIsRequired() {
    JDialogBuilder.builder()
        .title("title")
        .build();
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    JDialogBuilder.builder()
        .contents(new JPanel())
        .build();
  }
}
