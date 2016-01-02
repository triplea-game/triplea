package games.strategy.sound;


import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;



public class ClipPlayerTest {

  @Before
  public void setUp() throws Exception {}

  @Test
  public void test() {
    File assetsFolder = new File("assets");
    File soundsFolder = new File(assetsFolder, "sounds");
    File ww2SoundsFolder = new File(soundsFolder, "ww2");


    String[] files = ww2SoundsFolder.list();
    assertThat("Make sure we have some sound files and will be testing something",
        Lists.newArrayList(files), not(empty()));

    final String name = "ww2" + File.separator + files[0];

    final List<URL> availableSounds = new ArrayList<URL>();
    availableSounds.addAll(ClipPlayer.getInstance().createAndAddClips(ClipPlayer.ASSETS_SOUNDS_FOLDER + "/" + name));

    ClipPlayer.setBeSilent(false);
    ClipPlayer.getInstance().sounds.put(name, availableSounds);
    ClipPlayer.play(name);
    try {
      // sleep for a fraction of a second to let the audio play
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
