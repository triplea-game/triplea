package games.strategy.engine.framework.map.download;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import games.strategy.ui.SwingComponents;


/** A small non-modal window that holds the progress bars for the current and pending map downloads */
public final class MapDownloadProgressPanel extends JPanel {

  private static final long serialVersionUID = -7288639737337542689L;

  private final DownloadCoordinator downloadCoordinator;

  /*
   * Maintain grids that are placed east and west.
   * This gives us a minimal and uniform width for each column.
   */
  private final JPanel labelGrid = SwingComponents.gridPanel(0, 1);
  private final JPanel progressGrid = SwingComponents.gridPanel(0, 1);

  private final List<DownloadFileDescription> downloadList = new ArrayList<>();
  private final Map<DownloadFileDescription, JLabel> labels = Maps.newHashMap();
  private final Map<DownloadFileDescription, JProgressBar> progressBars = Maps.newHashMap();



  public MapDownloadProgressPanel(final JFrame parent) {
    downloadCoordinator = new DownloadCoordinator();
  }


  public void cancel() {
    downloadCoordinator.cancelDownloads();
  }


  public void download(List<DownloadFileDescription> newDownloads) {
    final List<DownloadFileDescription> brandNewDownloads = new ArrayList<>();
    for (final DownloadFileDescription download : newDownloads) {
      if (!downloadList.contains(download) && !download.getMapName().isEmpty()) {
        brandNewDownloads.add(download);
      }
    }
    newDownloads = brandNewDownloads;

    if (newDownloads.isEmpty()) {
      return;
    }

    final int itemCount = newDownloads.size() + downloadList.size();
    this.removeAll();
    add(SwingComponents.horizontalJPanel(labelGrid, progressGrid));

    labelGrid.setLayout(new GridLayout(itemCount, 1));
    progressGrid.setLayout(new GridLayout(itemCount, 1));



    for (final DownloadFileDescription download : newDownloads) {
      if (download.getMapName().isEmpty()) {
        continue;
      }
      // space at the end of the label so the text does not end right at the progress bar
      labels.put(download, new JLabel(download.getMapName() + "  "));
      final JProgressBar progressBar = new JProgressBar();
      progressBar.setStringPainted(true);
      progressBar.setToolTipText("Installing to: " + download.getInstallLocation().getAbsolutePath());

      progressBars.put(download, progressBar);
    }

    for (int i = newDownloads.size() - 1; i >= 0; i--) {
      // add new downoads to the head of the list, this will allow the user to see newly added items directly,
      // rather than having to scroll down to see new items.
      downloadList.add(0, newDownloads.get(i));
    }

    for (final DownloadFileDescription download : downloadList) {
      labelGrid.add(labels.get(download));
      progressGrid.add(progressBars.get(download));
    }

    revalidate();
    repaint();

    for (final DownloadFileDescription download : newDownloads) {
      if (download.getMapName().isEmpty()) {
        continue;
      }
      final JProgressBar progressBar = progressBars.get(download);
      final Consumer<Integer> progressListener = s -> SwingUtilities.invokeLater(() -> progressBar.setValue(s));
      final Runnable completionListener =
          () -> SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getMaximum()));


      downloadCoordinator.accept(download, progressListener, completionListener, progressBar);
    }
  }
}
