package games.strategy.triplea.ai.AdvancedAI;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.GameRunner2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JOptionPane;

public class AIStrategy
{
	private final String[] mapNames = { "World War II Classic", "World War II Revised", "World War II v3 1941",
									"World War II v3 1942", "WW2 NWO 2.0 Neutrals", "WW2 NWO 3.0 Neutrals",
									"WW2 Phillipines - v2", "New World Order 1939 Lebowski", "Stalingrad",
									"The Rising Sun", "Big World : 1942", "Big World : 1942 v3 Rules",
									"New World Order" }; // currently 13...not used yet
	private final String[] fileNames = { "World War II Classic", "World War II Revised", "World War II v3",
									"World War II v3", "WW2 NEW WORLD ORDER", "WW2 NEW WORLD ORDER",
									"WW2 Phillipines", "New World Order Lebowski Edition", "Stalingrad",
									"The_Rising_Sun", "big_world", "big_world", "new_world_order" };
	
	private final HashMap<Integer, AITurn> hashTurns; // contains the turn data
	private PlayerID player;
	// private final String stratName;
	private String gameName, fileName;
	private final String fileEnding;
	private final List<File> stratFileName;
	Properties stratProp;
	int mapType;
	String strategy;
	
	public AIStrategy(final String name)
	{
		// stratName = name; // This should be Russians, Americans, British, etc.
		mapType = -2; // map has not been checked
		hashTurns = new HashMap<Integer, AITurn>();
		stratFileName = new ArrayList<File>();
		fileEnding = ".moore_strat";
	}
	
	public void setPlayerID(final PlayerID id)
	{
		player = id;
	}
	
	private String getFileName()
	{
		if (getMapType() >= 0)
			return fileNames[getMapType()];
		return "";
	}
	
	private void setGameName(final String gN)
	{
		gameName = gN;
	}
	
	private String getGameName()
	{
		return gameName;
	}
	
	private int getMapType()
	{
		return mapType;
	}
	
	private String getFileEnding()
	{
		return fileEnding;
	}
	
	/**
	 * Checks to see if any information is available for this turn
	 * 
	 * @param currentTurn
	 * @return Territory List for Amphibious Assault
	 */
	public Collection<Territory> getAIAmphibStrategy(final int currentTurn)
	{
		if (hashTurns.containsKey(currentTurn))
		{
			return hashTurns.get(currentTurn).getLandingTerrs();
		}
		return null;
	}
	
	public HashMap<UnitType, Integer> getAIPurchaseStrategy(final int currentTurn)
	{
		if (hashTurns.containsKey(currentTurn))
		{
			return hashTurns.get(currentTurn).getPurchaseScheme();
		}
		return null;
	}
	
	public HashMap<Territory, Integer> getAITerritoryStrategy(final int currentTurn)
	{
		if (hashTurns.containsKey(currentTurn))
		{
			return hashTurns.get(currentTurn).getTargetTerr();
		}
		return null;
	}
	
	public HashMap<UnitType, HashMap<Territory, Integer>> getAIPlacementStrategy(final int currentTurn)
	{
		if (hashTurns.containsKey(currentTurn))
		{
			return hashTurns.get(currentTurn).getPlaceScheme();
		}
		return null;
	}
	
	/**
	 * Initialize the AI by
	 * 
	 * @param data
	 */
	public void initializeAIStrategy(final GameData data)
	{
		setGameName(data.getGameName());
		determineMapType(getGameName());
		setFilePathAndName(data);
		strategy = readStratFile();
		parseStrategy(data);
	}
	
	/**
	 * Prepare stratFileName for the possible locations of the strategy data
	 * 
	 * @param data
	 */
	private void setFilePathAndName(final GameData data)
	{
		// Taken from GameRunner code
		String thisGame = data.getGameName().trim();
		if (mapType >= 0) // we know exactly what the name is
			thisGame = getFileName();
		final String thisGame2 = thisGame;
		thisGame = thisGame.replace(' ', '_'); // Some of the maps use "_" in the names rather than spaces
		String dirName = thisGame;
		String dirName2 = dirName;
		fileName = thisGame + getFileEnding();
		final String fileName2 = thisGame2 + getFileEnding();
		dirName += File.separator + "AI" + File.separator + fileName;
		dirName2 += File.separator + "AI" + File.separator + fileName2;
		final File userMapsFolder = GameRunner2.getUserMapsFolder();
		final File[] userFiles = userMapsFolder.listFiles(new SpecialFileFilter(".zip")); // all of the zipfiles in user Folders
		stratFileName.add(new File(GameRunner2.getUserMapsFolder(), dirName));
		stratFileName.add(new File(new File(GameRunner2.getRootFolder(), "maps"), dirName));
		stratFileName.add(new File(GameRunner2.getUserMapsFolder(), dirName2));
		stratFileName.add(new File(new File(GameRunner2.getRootFolder(), "maps"), dirName2));
		final boolean addMore = moveFilesFromZip(userFiles);
		if (addMore)
		{
			final List<File> addFiles = findFileFromTempAI(new File(userMapsFolder, "tempAI"));
			if (addFiles != null)
				stratFileName.addAll(addFiles);
		}
	}
	
	/**
	 * Could move a test in here to determine which AI file is the real file
	 * But let the strat reader worry about that.
	 * 
	 * @param path
	 * @return
	 */
	private List<File> findFileFromTempAI(final File path)
	{
		if (path == null)
			return null;
		final File[] allAIFiles = path.listFiles(new SpecialFileFilter(getFileEnding()));
		final int numFiles = allAIFiles.length;
		// JOptionPane.showMessageDialog(null, "Path: "+path+"\n Number: "+numFiles+"\n Files: "+allAIFiles);
		if (numFiles == 0)
			return null;
		final List<File> otherAIFiles = new ArrayList<File>();
		for (int i = 0; i < numFiles; i++)
		{
			otherAIFiles.add(allAIFiles[i]);
		}
		return otherAIFiles;
	}
	
	/**
	 * Moves all of the AI files from the userMaps folder into a subfolder named "tempAI"
	 * 
	 * @param userFiles
	 * @return
	 */
	private boolean moveFilesFromZip(final File[] userFiles)
	{
		final byte[] buffer = new byte[1024];
		boolean someFilesMoved = false;
		final int fSize = userFiles.length;
		for (int i = 0; i < fSize; i++)
		{
			final File openFile = userFiles[i]; // these should only be .zip files
			final String openPath = openFile.getParent();
			final File createDir = new File(openPath, "tempAI");
			if (!createDir.isDirectory())
			{
				if (createDir.exists()) // is somehow tempAI a file?
					JOptionPane.showMessageDialog(null, "The maps directory contains an invalid file named tempAI. Please delete it.");
				try
				{
					createDir.mkdirs();
				} catch (final Exception e)
				{
					e.printStackTrace();
				}
			}
			try
			{
				final ZipFile zipFile = new ZipFile(openFile);
				final Enumeration<? extends ZipEntry> entries = zipFile.entries();
				while (entries.hasMoreElements())
				{
					final ZipEntry anEntry = entries.nextElement();
					final String checkThisEntry = anEntry.getName().trim();
					if (checkThisEntry.toLowerCase().startsWith("ai") && checkThisEntry.endsWith(getFileEnding()))
					{
						// JOptionPane.showMessageDialog(null, "Found AI Entry: "+checkThisEntry);
						final String fName = checkThisEntry.substring(3, checkThisEntry.length());
						final File createFile = new File(createDir, fName);
						if (createFile.exists())
							createFile.delete(); // only the last file will be considered
						final FileOutputStream oStream = new FileOutputStream(createFile);
						final InputStream ziStream = zipFile.getInputStream(anEntry);
						int len = ziStream.read(buffer);
						while (len > 0)
						{
							oStream.write(buffer, 0, len);
							len = ziStream.read(buffer);
						}
						oStream.close();
						ziStream.close();
						someFilesMoved = true;
					}
				}
				zipFile.close();
			} catch (final Exception e)
			{
				e.printStackTrace();
			}
		}
		return someFilesMoved;
	}
	
	/**
	 * Map Types
	 * Currently not used.
	 * Idea is to code in some strategy options for various files.
	 * With strategy file implemented, this may never actually be used.
	 * 
	 * If mapType = -1, no special strategy will be used.
	 */
	private void determineMapType(final String mapName)
	{
		for (int i = 0; i < 13; i++)
		{
			if (mapName.equals(mapNames[i]))
			{
				// JOptionPane.showMessageDialog(null, "Map Name Found: "+mapNames[i]+"\n"+fileNames[i]);
				mapType = i;
			}
		}
		if (mapType == -2)
			mapType = -1; // map is unknown
	}
	
	/**
	 * Takes a territory name and finds the territory object
	 * 
	 * @param allTerritories
	 * @param terr
	 * @return
	 */
	private Territory getTerritory(final Collection<Territory> allTerritories, final String terr)
	{
		for (final Territory thisTerr : allTerritories)
		{
			if (thisTerr.getName().equals(terr))
				return thisTerr;
		}
		return null;
	}
	
	private UnitType getUnitType(final Collection<UnitType> allUnits, final String unit)
	{
		for (final UnitType thisUnit : allUnits)
		{
			if (thisUnit.getName().equals(unit))
				return thisUnit;
		}
		return null;
	}
	
	// Does nothing
	public void makeStrategyFile() throws FileNotFoundException
	{
		try
		{
			final FileOutputStream outputStream = new FileOutputStream(fileName);
			// final HashMap<Territory, Integer> strat = new HashMap<Territory, Integer>();
			// strat.put(, arg1)
			outputStream.close();
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private String[] splitLine(final String line)
	{
		final String[] splitData = line.split("=");
		return splitData;
	}
	
	private boolean isComment(final String line)
	{
		return (line.trim().startsWith("//"));
	}
	
	/**
	 * Reads the actual file and loads it into a String with ";" separating each line
	 * 
	 * @return
	 */
	private String readStratFile()
	{
		String s = "";
		try
		{
			boolean fileFound = false;
			final Iterator<File> fileIter = stratFileName.iterator();
			while (!fileFound && fileIter.hasNext())
			{
				final File tryThisFile = fileIter.next();
				if (!tryThisFile.exists())
					continue;
				// JOptionPane.showMessageDialog(null, "Found AI File");
				final FileReader fileStream = new FileReader(tryThisFile);
				final BufferedReader inputStream = new BufferedReader(fileStream);
				String line = inputStream.readLine();
				while (line != null && isComment(line))
					line = inputStream.readLine(); // clear leading comments
				if (line == null)
					continue; // A file full of comments?
				// JOptionPane.showMessageDialog(null, "First Line: "+line+"\n Game Name: "+getGameName());
				if (!line.split("=")[1].toLowerCase().trim().equals(getGameName().toLowerCase().trim()))
					continue;
				boolean beginWriting = false;
				while (line != null)
				{
					if (line.toLowerCase().trim().equals("player=" + player.getName().toLowerCase()))
						beginWriting = true;
					if (!isComment(line) && beginWriting)
						s = s + line + ";";
					if (line.toLowerCase().trim().equals("endplayer=" + player.getName().toLowerCase()))
						beginWriting = false;
					line = inputStream.readLine();
				}
				inputStream.close();
				fileFound = true;
			}
		} catch (final FileNotFoundException e)
		{
			return null;
		} catch (final Exception e)
		{
			e.printStackTrace();
		}
		// JOptionPane.showMessageDialog(null, "Strategy: "+s);
		return s;
	}
	
	/**
	 * Parse the strategy data.
	 * 
	 * @param currentTurn
	 * @param allTerritories
	 *            Return Nothing. Everything is saved in the AITurns.
	 * 
	 *            Format of the file
	 *            Every line has an = on it
	 *            Every line has 2 pieces of information around the =
	 *            AmphibTargets ---> specific spot for potential amphibious landing
	 *            Territory ---> Bonuses for evaluating a country
	 *            + values will make the country more valuable
	 *            - values will make the country less valuable
	 *            Purchase ---> Specifically how many of a unitType to purchase
	 *            -99 means max possible
	 *            Placement ---> Each UnitType where to place
	 *            -99 means max possible
	 *            Player=Russians
	 *            Turn=1
	 *            Territory=1
	 *            Karelia=50 **Bonus for Karelia when evaluating the map
	 *            Purchase=1 **Only purchase 1 unittype
	 *            infantry=8
	 *            Placement=1 **Only place 1 unittype
	 *            infantry=2 **Place in 2 different locations
	 *            Russia=4
	 *            Karelia=4
	 *            EndTurn=1
	 *            EndPlayer=Russians
	 * 
	 *            Start a line with "//" to put a comment in the file
	 */
	private void parseStrategy(final GameData data)
	{
		if (strategy == null) // There is no strategy data
			return;
		final String[] allStrat = strategy.trim().split(";");
		// JOptionPane.showMessageDialog(null, "The Strategy File: "+"\n"+strategy.toString()+"\n"+allStrat);
		final int size = allStrat.length;
		if (size <= 1) // nothing here or incorrectly formatted
			return;
		int position = 0;
		final Collection<Territory> allTerritories = data.getMap().getTerritories();
		boolean foundPlayer = false;
		final String thisPlayer = player.getName();
		final Collection<UnitType> allUnits = data.getUnitTypeList().getAllUnitTypes();
		while (position < size)
		{
			String line = allStrat[position++];
			if (line == null) // just in case
				continue;
			String command[] = splitLine(line);
			if (!foundPlayer)
			{
				foundPlayer = command[1].contains(thisPlayer);
				if (foundPlayer)
				{
					// JOptionPane.showMessageDialog(null, "Player: "+player.getName()+" has been found!");
					line = allStrat[position++];
				}
			}
			if (!foundPlayer || line == null)
				continue;
			command = splitLine(line);
			int turn = 0;
			while (!command[0].toLowerCase().trim().contains("endplayer"))
			{
				if (command[0].toLowerCase().trim().contains("turn"))
					turn = Integer.parseInt(command[1]);
				final AITurn thisTurn = new AITurn(turn);
				line = allStrat[position++];
				command = splitLine(line);
				while (!command[0].toLowerCase().trim().contains("endturn"))
				{
					final Collection<Territory> landingTerrs = new ArrayList<Territory>();
					final HashMap<Territory, Integer> targetTerrs = new HashMap<Territory, Integer>();
					final HashMap<UnitType, Integer> purchaseStrat = new HashMap<UnitType, Integer>();
					final HashMap<UnitType, HashMap<Territory, Integer>> placeStrat = new HashMap<UnitType, HashMap<Territory, Integer>>();
					if (command[0].toLowerCase().trim().contains("amphibtargets"))
					{
						final int numTerr = Integer.parseInt(command[1].trim());
						for (int i = 0; i < numTerr; i++)
						{
							line = allStrat[position++];
							command = line.split("=");
							if (command[0].toLowerCase().trim().equals("target"))
							{
								final Territory landingTerr = getTerritory(allTerritories, command[1].trim());
								if (landingTerr != null)
									landingTerrs.add(landingTerr);
							}
						}
						thisTurn.setLandingTerrs(landingTerrs);
					}
					else if (command[0].toLowerCase().trim().contains("territory"))
					{
						final int numTerr = Integer.parseInt(command[1].trim());
						for (int i = 0; i < numTerr; i++)
						{
							line = allStrat[position++];
							command = splitLine(line);
							final Territory nTerr = getTerritory(allTerritories, command[0].trim());
							if (nTerr != null)
							{
								final Integer thisNum = Integer.parseInt(command[1].trim());
								targetTerrs.put(nTerr, thisNum);
							}
						}
						thisTurn.setTargetTerr(targetTerrs);
					}
					else if (command[0].toLowerCase().trim().contains("purchase"))
					{
						{
							final int numPurchase = Integer.parseInt(command[1]);
							for (int i = 0; i < numPurchase; i++)
							{
								line = allStrat[position++];
								command = splitLine(line);
								final UnitType nUnit = getUnitType(allUnits, command[0].trim());
								if (nUnit != null)
								{
									Integer thisNum = 0;
									if (command[1].toLowerCase().trim().equals("max"))
										thisNum = -99;
									else
										thisNum = Integer.parseInt(command[1].trim());
									purchaseStrat.put(nUnit, thisNum);
								}
							}
						}
						thisTurn.setPurchaseScheme(purchaseStrat);
					}
					else if (command[0].toLowerCase().trim().contains("placement"))
					{
						
						final int numPlace = Integer.parseInt(command[1].trim());
						for (int i = 0; i < numPlace; i++)
						{
							line = allStrat[position++];
							command = splitLine(line);
							final UnitType nUnit = getUnitType(allUnits, command[0].trim());
							final int iNumForThisUnit = Integer.parseInt(command[1].trim());
							final HashMap<Territory, Integer> thisUnitMap = new HashMap<Territory, Integer>();
							for (int j = 0; j < iNumForThisUnit; j++)
							{
								line = allStrat[position++];
								command = splitLine(line);
								final Territory placeTerr = getTerritory(allTerritories, command[0].trim());
								if (nUnit != null && placeTerr != null)
								{
									Integer iPlaceNum;
									if (command[1].toLowerCase().trim().equals("max"))
										iPlaceNum = -99;
									else
										iPlaceNum = Integer.parseInt(command[1].trim());
									thisUnitMap.put(placeTerr, iPlaceNum);
								}
							}
							placeStrat.put(nUnit, thisUnitMap);
						}
						thisTurn.setPlaceScheme(placeStrat);
					}
					line = allStrat[position++];
					command = splitLine(line);
				}
				hashTurns.put(turn, thisTurn);
				line = allStrat[position++];
				command = splitLine(line);
			}
		}
	}
	
	// Not implemented yet...ability to save information which is accessible by the other AI players
	public void SaveStrategyResourceFile() throws IOException
	{
		final FileOutputStream outputStream = new FileOutputStream(fileName);
		try
		{
			stratProp.storeToXML(outputStream, "Properties for " + player.getName());
		} catch (final IOException e)
		{
			// Don't worry about saving if you can't.
			// We'll just lose communication between allies.
		}
	}
	
	
	// Find only certain file endings (like .zip or .strat)
	public class SpecialFileFilter implements FileFilter
	{
		String thisOnly;
		
		public SpecialFileFilter(final String checkThis)
		{
			thisOnly = checkThis;
		}
		
		public boolean accept(final File file)
		{
			if (file.getName().trim().toLowerCase().endsWith(thisOnly))
				return true;
			return false;
		}
	}
}
