package swinglib;

import java.awt.GridBagLayout;

import javax.swing.JPanel;

public class JPanelModel {

  private final Layout layout;

  public JPanelModel(final Layout layout) {
    this.layout = layout;
  }

  public static JPanelModelBuilder builder() {
    return new JPanelModelBuilder();
  }

  private JPanel swingComponent() {
    final JPanel panel = new JPanel();
    switch (layout) {
      case GRID_BAG:
        panel.setLayout(new GridBagLayout());
        break;
      case DEFAULT:
        break;
    }
    return panel;
  }

  private enum Layout {
    DEFAULT, GRID_BAG
  }


  public static class JPanelModelBuilder {

    private Layout layout = Layout.DEFAULT;

    public JPanelModelBuilder withGridBagLayout() {
      layout = Layout.GRID_BAG;
      return this;
    }

    public JPanel swingComponent() {
      return new JPanelModel(layout)
          .swingComponent();
    }
  }
}
