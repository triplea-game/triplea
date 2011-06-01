/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/*
 * 
 *
 * 
 */

package games.strategy.triplea.attatchments;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.IAttachment;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.Constants;

/**
*
* @author  Squid
* 
*/

public class UnitSupportAttachment extends DefaultAttachment{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3015679930172496082L;

	
	private Set<UnitType> m_unitType = null;
    private boolean m_offence = false;
    private boolean m_defence = false;
    private boolean m_roll = false;
    private boolean m_strength = false;
    private int m_bonus = 0;
    private int m_numberSupported = 0;
    private boolean m_allied = false;
    private boolean m_enemy = false;
    private String m_bonusType = null;
    private List<PlayerID> m_players = new ArrayList<PlayerID>();
    private boolean m_impArtTech = false;

    public UnitSupportAttachment()
    {
    } 
    
    public static Set<UnitSupportAttachment> get(UnitType u) {
    	Set<UnitSupportAttachment> supports = new HashSet<UnitSupportAttachment>();
        Map<String, IAttachment> map = u.getAttachments();
        Iterator<String> objsIter = map.keySet().iterator();
        while(objsIter.hasNext() )
        {
            IAttachment attachment = map.get(objsIter.next());
            String name = attachment.getName();
            if (name.startsWith(Constants.SUPPORT_ATTACHMENT_PREFIX))
            {
            	supports.add((UnitSupportAttachment)attachment);
            }
        }
        return supports;
    	
    }
    
    public static Set<UnitSupportAttachment> get(GameData data) 
    {
    	Set<UnitSupportAttachment> supports = new HashSet<UnitSupportAttachment>();
    	Iterator<UnitType> i = data.getUnitTypeList().iterator();
    	while(i.hasNext()){
    		supports.addAll(get(i.next()));
    	}
    	return supports;
    }
    public void setUnitType(String names) throws GameParseException
    {
    	m_unitType = new HashSet<UnitType>();
    	String[] s = names.split(":");
    	for( int i=0; i < s.length; i++){
            UnitType type = getData().getUnitTypeList().getUnitType(s[i]);
            if(type == null)
                throw new GameParseException("Supports: Could not find unitType. name:" + s[i]);
            m_unitType.add(type);
    	}
    }
    
    public void setFaction(String faction) throws GameParseException
    {
    	String[] s = faction.split(":");
    	for( int i=0; i < s.length; i++){
    		if(s[i].equalsIgnoreCase("allied") )
    			m_allied = true;
    		else if(s[i].equalsIgnoreCase("enemy")) 
    			m_enemy = true;
    		else
    			throw new GameParseException("Supports: " + faction + " faction must be allied, or enemy");
    	}
    }
 
    public void setSide(String side) throws GameParseException{
    	
    	String[] s = side.split(":");
    	for( int i=0; i < s.length; i++){
    		if(s[i].equalsIgnoreCase("defence") ) 
    			m_defence = true;
    		else if(s[i].equalsIgnoreCase("offence")) 
    			m_offence = true;
    		else
    			throw new GameParseException("Supports: " + side + " side must be defence or offence");
    	}
    }
    
    public void setDice(String dice) throws GameParseException{
    	
    	String[] s = dice.split(":");
    	for( int i=0; i < s.length; i++){
    		if(s[i].equalsIgnoreCase("roll") ) 
    			m_roll = true;
    		else if(s[i].equalsIgnoreCase("strength")) 
    			m_strength = true;
    		else
    			throw new GameParseException("Supports: " + dice + " dice must be roll or strength");
    	}
    }

    public void setBonus(String bonus) {
    	m_bonus = getInt(bonus);
    }

    public void setNumber(String number) {
    	m_numberSupported = getInt(number);
    }
    
    public void setBonusType(String type) {
    	m_bonusType = type;
    }
    
    public void setPlayers(String names) throws GameParseException
    {
    	String[] s = names.split(":");
    	for( int i=0; i < s.length; i++){
    		PlayerID player = getData().getPlayerList().getPlayerID(s[i]);
            if(player == null)
                throw new GameParseException("Supports: Could not find player. name:" + s[i]);
            else
            	m_players.add(player);
    	}       
    }
    
    public void setPlayers(ArrayList<PlayerID> players)
    {
    	m_players = players;
    }
    
    public void setImpArtTech(String tech) {
    	m_impArtTech = getBool(tech);
    }
    
    public Set<UnitType> getUnitTypes() {
    	return m_unitType;
    }
    
    public List<PlayerID> getPlayers() {
    	return m_players;
    }
    
    public int getNumber() {
    	return m_numberSupported;
    }
    
    public int getBonus() {
    	return m_bonus;
    }
    
    public boolean getDefence() {
    	return m_defence;
    }
    
    public boolean getOffence() {
    	return m_offence;
    }
    
    public String getBonusType() {
    	return m_bonusType;
    }
    
    public boolean getImpArtTech() {
    	return m_impArtTech; 
    }
    
    /*
     * following are all to support old artillery flags.
     * boolean first is a cheat, adds a bogus support to a unit
     * in the case that supportable units are declared before any artillery
     */
    public static void addRule(UnitType type, GameData data, boolean first) throws GameParseException{
  
    	UnitSupportAttachment rule = new UnitSupportAttachment();
    	rule.setData(data);
    	rule.setBonus("1");
    	rule.setBonusType(Constants.OLD_ART_RULE_NAME);
    	rule.setDice("strength");
    	rule.setFaction("allied");
    	rule.setImpArtTech("true");
    	if( first)
    		rule.setNumber("0");
    	else
    		rule.setNumber("1");
    	rule.setSide("offence");
    	if(first)
    		rule.addUnitTypes(Collections.singleton(type));	
    	else
    		rule.addUnitTypes(getTargets(data));	
    	rule.setPlayers(new ArrayList<PlayerID>(data.getPlayerList().getPlayers()));
    	type.addAttachment(Constants.SUPPORT_RULE_NAME_OLD+type.getName(), rule);
    	rule.setAttatchedTo(type);
    	rule.setName(Constants.SUPPORT_RULE_NAME_OLD+type.getName()); 
    }
    
    
    private static Set<UnitType> getTargets(GameData data) {
    	Iterator<UnitSupportAttachment> iter = get(data).iterator();
    	while( iter.hasNext()){
    		UnitSupportAttachment rule = iter.next();
    		if( rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME))
    			return rule.getUnitTypes(); 
    	}
    	return null;
    }
    
    private void addUnitTypes(Set<UnitType> types) 
    {
    	if( types == null )
    		return;
    	if( m_unitType == null)
    		m_unitType = new HashSet<UnitType>();
        m_unitType.addAll(types);
    }
    
    public static void setOldSupportCount (UnitType type, GameData data, String count){
    	Iterator<UnitSupportAttachment> iter = get(data).iterator();
    	while( iter.hasNext()){
    		UnitSupportAttachment rule = iter.next();
    		if( rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME)
    				&& rule.getAttatchedTo() == type)
    			rule.setNumber(count);
    		
    	}
    }

    public static void addTarget(UnitType type,GameData data)throws GameParseException{
    	Iterator<UnitSupportAttachment> iter = get(data).iterator();
    	boolean first = true;
    	while( iter.hasNext()){
    		UnitSupportAttachment rule = iter.next();
    		if( rule.getBonusType().equals(Constants.OLD_ART_RULE_NAME) ){
    			rule.addUnitTypes(Collections.singleton(type));
    			first = false;
    		}
    	}
    	if(first)
    		addRule(type, data, first);
    		
    }

}
