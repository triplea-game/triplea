package org.triplea.sound;

import games.strategy.engine.data.properties.BooleanProperty;

/** Checkbox wrapper for a sound option. */
class SoundOptionCheckBox extends BooleanProperty {
  private static final long serialVersionUID = 5774074488487286103L;
  private final String clipName;

  /**
   * Initializes a new instance of the SoundOptionsCheckBox class.
   *
   * @param clipName sound file name.
   * @param title title to display
   */
  SoundOptionCheckBox(final String clipName, final String title) {
    super(title, null, true);
    if (ClipPlayer.getInstance().isSoundClipMuted(clipName)) {
      setValue(false);
    }
    this.clipName = clipName;
  }

  String getClipName() {
    return clipName;
  }
}
