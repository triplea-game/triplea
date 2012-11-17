/**
 * Created on 12.03.2012
 */
package games.strategy.sound;

import games.strategy.engine.data.properties.BooleanProperty;

/**
 * Checkbox wrapper for a sound option.
 * 
 * @author Frigoref
 * 
 */
class SoundOptionCheckBox extends BooleanProperty
{
	private static final long serialVersionUID = 5774074488487286103L;
	
	final String m_clipName;
	
	/**
	 * 
	 * @param clipName
	 *            sound file name
	 * @param title
	 *            title to display
	 */
	public SoundOptionCheckBox(final String clipName, final String title)
	{
		super(title, null, true);
		if (ClipPlayer.getInstance().isMuted(clipName))
			setValue(false);
		m_clipName = clipName;
	}
	
	public String getClip()
	{
		return m_clipName;
	}
	
	public String getClipName()
	{
		return m_clipName;
	}
}
