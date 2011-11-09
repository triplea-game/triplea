/**
 * 
 */
package games.strategy.util;

/**
 * Designed to remove/replace / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
 * 
 * @author Chris Duncan
 *
 */
public class IllegalCharacterRemover
{
	private static final char[] ILLEGAL_CHARACTERS = {'/', '\b', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', '\'', ':', '.', ',', '^', '[', ']', '=', '+', ';'};
	
	/**
	 * Designed to remove / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
	 * 
	 * @param text
	 * @return
	 */
	public static String removeIllegalCharacter(String text)
	{
		StringBuilder rVal = new StringBuilder();
		for (int i = 0; i < text.length(); ++i)
		{
			if (!isIllegalFileNameChar(text.charAt(i)))
				rVal.append(text.charAt(i));
		}
		return rVal.toString();
	}
	
	/**
	 * Designed to replace / \b \n \r \t \0 \f ` ? * \ < > | " ' : . , ^ [ ] = + ;
	 * 
	 * @param text
	 * @param replacement
	 * @return
	 */
	public static String replaceIllegalCharacter(String text, char replacement)
	{
		StringBuilder rVal = new StringBuilder();
		for (int i = 0; i < text.length(); ++i)
		{
			if (!isIllegalFileNameChar(text.charAt(i)))
				rVal.append(text.charAt(i));
			else
				rVal.append(replacement);
		}
		return rVal.toString();
	}
	
	private static boolean isIllegalFileNameChar(char c)
	{
		boolean isIllegal = false;
		for (int i = 0; i < ILLEGAL_CHARACTERS.length; i++)
		{
			if (c == ILLEGAL_CHARACTERS[i])
			{
				isIllegal = true;
				break;
			}
		}
		return isIllegal;
	}
}
