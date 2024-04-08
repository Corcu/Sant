package com.santander.restservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExUtil {
    
	public RegExUtil() {}

	public static boolean matches(String data, String regex) 
	{
		Pattern pattern = null;
		Matcher matcher = null;
		
		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(data);
		
		return matcher.matches();
	}
	
	public static Map<Integer,String> getMatchingGroups(String data, String regex) 
	{
		Pattern pattern = null;
		Matcher matcher = null;
		Map<Integer,String> out = null;

		pattern = Pattern.compile(regex);
		matcher = pattern.matcher(data);
		
		out = new HashMap<Integer,String>();
		
	    if (matcher.find())
		{
		    int groupCount = matcher.groupCount();
			
			for (int i = 0; i <= groupCount; i++)
			{
				out.put(i,  matcher.group(i));
			}
		}
		
		return out;
	}
	
	public static String getMatchingGroup(String data, String regex, int group) 
	{
		Map<Integer,String> groups = null;
		String out = null;
		
		groups = getMatchingGroups(data, regex);
		
		if (groups != null)
		{
			out = groups.get(group);
		}
		
		return out;
	}
	
	public static List<String> findMatchingGroups(String data, String regex) 
	{
		Map<Integer,String> groups = null;
		ArrayList<String> out = null;
		
		groups = getMatchingGroups(data, regex);
		
		if (groups != null)
		{
			out = new ArrayList<String>();
			
			for (Integer index : groups.keySet())
			{
		        if (out.size() <= index)
		        {
		            for (int i = out.size(); i < index; i++)
		            {
		            	out.add(i, null);
		            }
		            out.add(index, groups.get(index));
		        }
		        else
		        {
		        	out.set(index, groups.get(index));
		        }
			}
		}
		
		return out;
	}
}
