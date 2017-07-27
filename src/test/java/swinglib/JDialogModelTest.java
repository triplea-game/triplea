package swinglib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

import org.junit.Test;

public class JDialogModelTest {

  @Test
  public void testBuild() {
    final JDialog dialog = JDialogModel.builder()
        .withContents(new JPanel())
        .withTitle("title")
        .swingComponent();

    assertThat(dialog.getTitle(), Is.is("title"));
  }

  @Test
  public void buildWithAllParams() {
    MatcherAssert.assertThat(
        JDialogModel.builder()
            .withContents(new JPanel())
            .withTitle("abc")
            .withParentFrame(new JFrame())
            .swingComponent(),
        notNullValue());
  }

  @Test(expected = NullPointerException.class)
  public void nullParentFrameNotAllowed() {
    JDialogModel.builder()
        .withParentFrame(null)
        .withTitle("title")
        .withContents(new JPanel())
        .swingComponent();
  }
  @Test(expected = NullPointerException.class)
  public void contentsIsRequired() {
    JDialogModel.builder()
        .withTitle("title")
        .swingComponent();
  }

  @Test(expected = NullPointerException.class)
  public void titleIsRequired() {
    JDialogModel.builder()
        .withContents(new JPanel())
        .swingComponent();
  }
}
