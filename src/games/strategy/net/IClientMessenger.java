package games.strategy.net;

import games.strategy.engine.framework.ui.SaveGameFileChooser;

import java.io.File;

public interface IClientMessenger extends IMessenger
{
	public void changeServerGameTo(final String gameName);
	
	public void changeToLatestAutosave(final SaveGameFileChooser.AUTOSAVE_TYPE typeOfAutosave);
	
	public void changeToGameSave(final byte[] bytes, final String fileName);
	
	public void changeToGameSave(final File saveGame, final String fileName);
}
