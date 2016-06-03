package games.strategy.engine.framework.map.download;

import java.awt.GridLayout;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import games.strategy.common.swing.SwingComponents;


/** A small non-modal window that holds the progress bars for the current and pending map downloads */
public final class MapDownloadProgressPanel extends JPanel {

  private static final long serialVersionUID = -7288639737337542689L;

  private final DownloadCoordinator downloadCoordinator;

  /*
   * Maintain grids that are placed east and west.
   * This gives us a minimal and uniform width for each column.
   */
  private JPanel labelGrid = SwingComponents.gridPanel(0, 1);
  private JPanel progressGrid = SwingComponents.gridPanel(0, 1);

  private List<DownloadFileDescription> downloadList = Lists.newArrayList();
  private Map<DownloadFileDescription, JLabel> labels = Maps.newHashMap();
  private Map<DownloadFileDescription, JProgressBar> progressBars = Maps.newHashMap();



  public MapDownloadProgressPanel(final JDialog parent) {
    downloadCoordinator = new DownloadCoordinator();
  }


  public void cancel() {
    downloadCoordinator.cancelDownloads();
  }


  public void download(List<DownloadFileDescription> newDownloads) {
    List<DownloadFileDescription> brandNewDownloads = Lists.newArrayList();
    for (DownloadFileDescription download : newDownloads) {
      if (!downloadList.contains(download) && !download.isDummyUrl() && !download.getMapName().isEmpty()) {
        brandNewDownloads.add(download);
      }
    }
    newDownloads = brandNewDownloads;

    if (newDownloads.isEmpty()) {
      return;
    }

    int itemCount = newDownloads.size() + downloadList.size();
    this.removeAll();
    add(SwingComponents.horizontalJPanel(labelGrid, progressGrid));

    labelGrid.setLayout(new GridLayout(itemCount, 1));
    progressGrid.setLayout(new GridLayout(itemCount, 1));



    for (DownloadFileDescription download : newDownloads) {
      if (download.isDummyUrl() || download.getMapName().isEmpty()) {
        continue;
      }
      // space at the end of the label so the text does not end right at the progress bar
      labels.put(download, new JLabel(download.getMapName() + "  "));
      JProgressBar progressBar = new JProgressBar();
      progressBar.setStringPainted(true);
      progressBar.setToolTipText("Installing to: " + download.getInstallLocation().getAbsolutePath());

      progressBars.put(download, progressBar);
    }

    for (int i = newDownloads.size() - 1; i >= 0; i--) {
      // add new downoads to the head of the list, this will allow the user to see newly added items directly,
      // rather than having to scroll down to see new items.
      downloadList.add(0, newDownloads.get(i));
    }

    for (DownloadFileDescription download : downloadList) {
      labelGrid.add(labels.get(download));
      progressGrid.add(progressBars.get(download));
    }

    revalidate();
    repaint();

    for (DownloadFileDescription download : newDownloads) {
      if (download.isDummyUrl() || download.getMapName().isEmpty()) {
        continue;
      }
      final JProgressBar progressBar = progressBars.get(download);
      Consumer<Integer> progressListener = s -> SwingUtilities.invokeLater(() -> progressBar.setValue(s));
      Runnable completionListener = () -> SwingUtilities.invokeLater(() -> progressBar.setValue(progressBar.getMaximum()));

      (new Thread(() -> {
        int length = DownloadUtils.getDownloadLength(download.newURL());
        SwingUtilities.invokeLater(() -> {
          progressBar.setMinimum(0);
          progressBar.setMaximum(length);
        });
        downloadCoordinator.accept(download, progressListener, completionListener);
      })).start();
    }
  }
}
