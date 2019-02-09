package games.strategy.debug.error.reporting;

import javax.swing.JFrame;

import org.triplea.swing.JButtonBuilder;
import org.triplea.swing.JFrameBuilder;
import org.triplea.swing.JPanelBuilder;
import org.triplea.swing.JTextAreaBuilder;
import org.triplea.swing.SwingComponents;

import com.google.common.annotations.VisibleForTesting;

/**
 * A simple window printing a representation of the bug report upload payload.
 */
final class PreviewWindow {
  private PreviewWindow() {}

  static JFrame build(final JFrame parent, final String previewText) {
    final JFrame frame = JFrameBuilder.builder()
        .locateRelativeTo(parent)
        .alwaysOnTop()
        .escapeKeyClosesFrame()
        .minSize(500, 500)
        .build();
    frame.add(JPanelBuilder.builder()
        .borderLayout()
        .addCenter(JPanelBuilder.builder()
            .borderEmpty(5)
            .add(SwingComponents.newJScrollPane(
                JTextAreaBuilder.builder()
                    .text(previewText)
                    .columns(40)
                    .rows(20)
                    .readOnly()
                    .componentName(ComponentNames.PREVIEW_AREA.toString())
                    .build()))
            .build())
        .addSouth(JPanelBuilder.builder()
            .borderEmpty(2)
            .flowLayout()
            .addHorizontalGlue()
            .add(JButtonBuilder.builder()
                .title("Close")
                .actionListener(frame::dispose)
                .build())
            .addHorizontalGlue()
            .build())
        .build());
    frame.pack();
    return frame;
  }

  @VisibleForTesting
  enum ComponentNames {
    PREVIEW_AREA
  }
}
