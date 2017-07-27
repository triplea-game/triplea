package swinglib.jdialog;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.swing.JComponent;
import javax.swing.JFrame;

public class JDialogModel {
  final JFrame parentFrame;
  final String title;
  final JComponent contents;

  private JDialogModel(final JFrame parentFrame, final String title, final JComponent contents) {
    this.parentFrame = parentFrame;
    this.title = title;
    this.contents = contents;
  }

  public static SwingJDialogModelBuilder builder() {
    return new SwingJDialogModelBuilder();
  }

  public static class SwingJDialogModelBuilder {
    private JFrame parentFrame;
    private String title;
    private JComponent contents;

    private SwingJDialogModelBuilder() {

    }

    public SwingJDialogModelBuilder withParentFrame(final JFrame parentFrame) {
      this.parentFrame = parentFrame;
      return this;
    }

    public SwingJDialogModelBuilder withTitle(final String title) {
      this.title = title;
      return this;
    }

    public SwingJDialogModelBuilder withContents(final JComponent contents) {
      this.contents = contents;
      return this;
    }

    public JDialogModel build() {
      return new JDialogModel(
          checkNotNull(parentFrame),
          checkNotNull(title),
          checkNotNull(contents));
    }
  }

}
