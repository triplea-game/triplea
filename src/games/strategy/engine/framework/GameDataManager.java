package games.strategy.engine.framework;

import java.io.*;
import java.util.*;

import games.strategy.engine.data.*;
import games.strategy.engine.delegate.*;
import games.strategy.util.*;

/**
 * <p>Title: TripleA</p>
 * <p>Description: Respsible for loading saved games, new games from xml, and saving games</p>
 * @author Sean Bridges
 */

public class GameDataManager
{

  private final static String DELEGATE_START = "<DelegateStart>";
  private final static String DELEGATE_END = "<DelegateEnd>";
  private final static String DELEGATE_DATA_NEXT = "<DelegateData>";
  private final static String DELEGATE_LIST_END = "<EndDelegateList>";

  public GameDataManager()
  {
  }


  public GameData loadGame(File savedGameFile) throws IOException
  {
    InputStream input = null;
    try
    {
      input = new BufferedInputStream(new FileInputStream(savedGameFile));
      return loadGame(input);
    }
    finally
      {
      try
      {
        input.close();
      } catch(Exception e) {}
      }

  }

  public GameData loadGame(InputStream input) throws IOException
  {
    return loadGame(new GameDataManagerObjectInputStream(input));
  }

  public GameData loadGame(ObjectInputStream input) throws IOException
  {
    try
    {
      //TODO we should check the game version as well
      Version engineVersion = (Version) input.readObject();
      if(!engineVersion.equals(games.strategy.engine.EngineVersion.VERSION))
        throw new IOException("Incompatable engine versions");

      Unit.clearUnits();
      GameData data = (GameData) input.readObject();


      loadDelegates(input, data);
        data.postSerialize();

      return data;

  } catch(ClassNotFoundException cnfe)
  {
    throw new IOException(cnfe.getMessage());
  }

  }

  private void loadDelegates(ObjectInputStream input, GameData data) throws ClassNotFoundException, IOException
  {
  for(Object endMarker = input.readObject(); !endMarker.equals(DELEGATE_LIST_END); endMarker = input.readObject() )
  {
      String name = (String) input.readObject();
    String displayName = (String) input.readObject();
    String className = (String) input.readObject();

    Delegate instance;
    try
    {
      instance = (Delegate) Class.forName(className).newInstance();
      instance.initialize(name, displayName);
      data.getDelegateList().addDelegate(instance);
    } catch(Exception e)
    {
      e.printStackTrace();
      throw new IOException(e.getMessage());
    }

    String next = (String) input.readObject();
    if(next.equals(DELEGATE_DATA_NEXT))
    {
      ((SaveableDelegate) instance).loadState((Serializable) input.readObject());
    }
  }
  }

  public void saveGame(File destination, GameData data) throws IOException
  {
    OutputStream fileStream = null;
    try
    {
      fileStream = new FileOutputStream(destination);
      saveGame(fileStream, data);
    }
    finally
    {
      try
      {
        fileStream.close();
      } catch(Exception e) {}
    }
  }

  public void saveGame(OutputStream sink, GameData data) throws IOException
  {
    //write internally first in case of error
    ByteArrayOutputStream bytes = new ByteArrayOutputStream(25000);
    ObjectOutputStream outStream = new ObjectOutputStream(bytes);

    outStream.writeObject(games.strategy.engine.EngineVersion.VERSION);
    outStream.writeObject(data);
    writeDelegates(data, outStream);
    writeGameSteps(data, outStream);

    //now write to file
    sink.write(bytes.toByteArray());
  }

  private void writeDelegates(GameData data, ObjectOutputStream out) throws IOException
  {

  Iterator iter = data.getDelegateList().iterator();
  while(iter.hasNext())
  {
    out.writeObject(DELEGATE_START);

    Delegate delegate = (Delegate) iter.next();

    //write out the delegate info
    out.writeObject(delegate.getName());
    out.writeObject(delegate.getDisplayName());
    out.writeObject(delegate.getClass().getName());

    if(delegate instanceof SaveableDelegate)
    {
      SaveableDelegate saveable = (SaveableDelegate) delegate;
      String[] message = new String[1];
      if(!saveable.canSave(message))
      {
        throw new IOException(message[0]);
      }
      else
      {
        out.writeObject(DELEGATE_DATA_NEXT);
        out.writeObject(saveable.saveState());
      }
    }
    else
    {
      out.writeObject(DELEGATE_END);
    }
  }
  //mark end of delegate section
  out.writeObject(DELEGATE_LIST_END);
  }

  private void writeGameSteps(GameData data, ObjectOutputStream out) throws  IOException
  {
  //TODO write the game steps, write the current step (PC)
  }


  public GameData loadNewGame(File gameXMLFile)
  {
    return null;
  }



}



class GameDataManagerObjectInputStream extends ObjectInputStream
{
  GameDataManagerObjectInputStream(InputStream in) throws IOException
  {
    super(in);

    enableResolveObject(true);
  }

  protected Object resolveObject(Object obj) //throws IOException
  {
    //we want to register the units in the hash map.
    if(obj instanceof Unit)
    {
      Unit.put( (Unit) obj);
    }
    return obj;
  }
}
