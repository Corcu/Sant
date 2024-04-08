package com.santander.restservices;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Properties;

public class ResourceLoader
{
    private static Logger Log = LogManager.getLogger(ResourceLoader.class);

    private static final int INPUT_STREAM_RETRIES = 10;

    private ResourceLoader()
    {
    }

    /**
     * Read the resource from system classpath
     * 
     * @return InputStream to resource
     */
    public static String getPathClassPath(Class<?> clazz, String resource)
    {

	ClassLoader loader = null;
	URL url = null;
	String path = null;

	try
	{
	    loader = clazz.getClassLoader();

	    if (loader != null)
	    {
		url = loader.getResource(resource);

		if (url != null)
		    path = url.getFile();
		else
		    Log.warn("Can not load resource from reference for: " + resource);
	    }
	    else
	    {
		Log.warn("Can not load resource from classpath for: " + resource);
	    }
	}
	catch (Exception e)
	{
	    Log.warn("Can not load resource from classpath for: " + resource, e);
	}

	return path;

    }

    /**
     * Read the resource from class reference
     * 
     * @return InputStream to resource
     */
    public static String getPathClassReference(Class<?> clazz, String resource)
    {

	URL url = null;
	String path = null;

	try
	{
	    url = clazz.getResource(resource);

	    if (url != null)
		path = url.getPath() + File.separator + url.getFile();
	    else
		Log.warn("Can not load resource from reference for: " + resource);

	}
	catch (Exception e)
	{
	    Log.warn("Can not load resource from reference for: " + resource, e);
	}

	return path;

    }

    /**
     * Read the properties from system classpath
     * 
     * @return InputStream to resource
     */
    public static Properties getPropertiesFromClassPath(Class<?> clazz, String resource)
    {

	Properties properties = null;
	InputStream is = null;

	is = loadFromClassPath(clazz, resource);

	try
	{
	    if (is != null)
	    {
		try
		{
		    properties = new Properties();
		    properties.load(is);
		}
		catch (final IOException e)
		{
		    properties = null;
		    Log.warn("Can not load properties from classpth for: " + resource, e);
		}
	    }
	    else
	    {
		Log.warn("Can not load properties from classpth for: " + resource);
	    }
	}
	finally
	{
	    try
	    {
		if (is != null)
		{
		    is.close();
		    is = null;
		}
	    }
	    catch (IOException e)
	    {
	    }
	}

	return properties;

    }

    /**
     * Read the properties from class reference
     * 
     * @return InputStream to resource
     */
    public static Properties getPropertiesFromClassReference(Class<?> clazz, String resource)
    {

	Properties properties = null;
	InputStream is = null;

	is = loadFromClassReference(clazz, resource);

	try
	{
	    if (is != null)
	    {
		try
		{
		    properties = new Properties();
		    properties.load(is);
		}
		catch (final IOException e)
		{
		    properties = null;
		    Log.warn("Can not load properties from resource for: " + resource, e);
		}
	    }
	    else
	    {
		Log.warn("Can not load properties from resource for: " + resource);
	    }
	}
	finally
	{
	    try
	    {
		if (is != null)
		{
		    is.close();
		    is = null;
		}
	    }
	    catch (IOException e)
	    {
	    }
	}

	return properties;

    }

    /**
     * Read the resource from system classpath
     * 
     * @return InputStream to resource
     */
    public static InputStream loadFromClassPath(Class<?> clazz, String resource)
    {

	ClassLoader loader = null;
	InputStream is = null;

	try
	{
	    loader = clazz.getClassLoader();

	    if (loader != null)
		is = loader.getResourceAsStream(resource);
	    else
		Log.warn("Can not load resource from classpath for: " + resource);
	}
	catch (Exception e)
	{
	    Log.warn("Can not load resource from classpath for: " + resource, e);
	}

	return is;

    }

    /**
     * Read the resource from class reference
     * 
     * @return InputStream to resource
     */
    public static InputStream loadFromClassReference(Class<?> clazz, String resource)
    {

	URL url = null;
	InputStream is = null;

	try
	{
	    url = clazz.getResource(resource);

	    if (url != null)
		is = url.openStream();
	    else
		Log.warn("Can not load resource from reference for: " + resource);

	}
	catch (Exception e)
	{
	    Log.warn("Can not load resource from reference for: " + resource, e);
	}

	return is;

    }

    /**
     * Read the resource from system classpath
     * 
     * @return InputStream to resource
     */
    public static byte[] loadDataFromClassPath(Class<?> clazz, String resource)
    {

	InputStream ios = null;
	ByteArrayOutputStream ous = null;
	byte[] out = null;

	try
	{
	    ous = new ByteArrayOutputStream();
	    ios = loadFromClassPath(clazz, resource);

	    readInputStreamByBuffer(ios, ous);

	    out = ous.toByteArray();
	}
	finally
	{
	    try
	    {
		if (ios != null)
		{
		    ios.close();
		    ios = null;
		}
	    }
	    catch (IOException e)
	    {
	    }

	    try
	    {
		if (ous != null)
		{
		    ous.close();
		    ous = null;
		}
	    }
	    catch (IOException e)
	    {
	    }
	}

	return out;

    }

    /**
     * Read the resource from class reference
     * 
     * @return InputStream to resource
     */
    public static byte[] loadDataFromClassReference(Class<?> clazz, String resource)
    {

	InputStream ios = null;
	ByteArrayOutputStream ous = null;
	byte[] out = null;

	try
	{
	    ous = new ByteArrayOutputStream();
	    ios = loadFromClassReference(clazz, resource);

	    readInputStreamByBuffer(ios, ous);

	    out = ous.toByteArray();
	}
	finally
	{
	    try
	    {
		if (ios != null)
		{
		    ios.close();
		    ios = null;
		}
	    }
	    catch (IOException e)
	    {
	    }

	    try
	    {
		if (ous != null)
		{
		    ous.close();
		    ous = null;
		}
	    }
	    catch (IOException e)
	    {
	    }
	}

	return out;

    }

    /**
     * Read data from InputStream using buffer and write data to OutputStream
     * 
     * @return void
     */
    public static void readInputStreamByBuffer(InputStream ios, OutputStream ous)
    {

	int retries = 0;
	int read = 0;
	byte[] buffer = new byte[4096];

	try
	{
	    retries = 0;
	    read = 0;
	    while (retries < INPUT_STREAM_RETRIES && (read = ios.read(buffer)) != -1)
	    {
		ous.write(buffer, 0, read);
		if (read <= 0)
		{
		    retries++;
		}
	    }
	}
	catch (IOException e)
	{
	    Log.error("Can not read InputStream", e);
	}

    }
}
