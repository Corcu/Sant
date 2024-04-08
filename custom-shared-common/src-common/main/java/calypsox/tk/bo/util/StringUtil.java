package calypsox.tk.bo.util;

import java.util.ArrayList;
import java.util.List;

public class StringUtil
{
    public static String truncate(String str, int len)
    {
        int index = 0;
        String out = "";
        
        try
        {
            if (str != null)
            {
                index = (str.length() < len) ?  str.length() : len;
                
                out = str.substring(0, index);
            }
        }
        catch (IndexOutOfBoundsException e) {}
        
        return out;
    }
    
    public static String truncate(String str, int ini, int len)
    {
        int index1 = 0;
        int index2 = 0;
        String out = "";
        
        try
        {
            if (str != null)
            {
                index1 = (ini < str.length()) ? ini : str.length();
                index1 = (index1 < 0) ?  str.length() : index1;
                index2 = (str.length() - index1 < len) ? str.length() - index1 : len;
                index2 = (index2 < 0) ?  0 : index2;
                
                out = str.substring(index1, index1 + index2);
            }
        }
        catch (IndexOutOfBoundsException e) {}
        
        return out;
    }
    
    public static List<String> truncateRecursive(String str, int len, int max)
    {
        int index = 0;
        String token = "";
        List<String> out = null;
        
        out = new ArrayList<String>();
        
        if (str != null)
        {
            do
            {
                token = truncate(str, index * len, len);
                
                if ((token != null) && (!token.equals("")))
                {
                    out.add(token);
                }
                
                index++;
            }
            while ((token != null) && (!token.equals("")) && (index < max));
        }
        
        return out;
    }
}
