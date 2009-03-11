package games.strategy.util;

public class ClientPasswodScrambler
{

    
    /**
     * Technically this is not correct.  We should encrypt the password on the server using a random
     * salt generated on the server.  But to avoid sending the password over the network as plain text,
     * encrypt it here.
     * 
     *  All we are really doing is not sending the password over the network in plaintext, but if this
     *  string is captured, then it could be used to log in, just as well as the original password.
     */
    public static String scramble(String password)
    {
        return MD5Crypt.crypt(password, "testring", "by" );
    }
    
    public static void main(String[] args)
    {
        System.out.println((scramble("testing")));
    }
    
}
