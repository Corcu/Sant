package com.santander.restservices;

import com.calypso.tk.core.Log;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Store the connection properties for the api rest service
 * 
 */
public class ApiRestEndPoint
{
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final int HTTP_DEFAULT_PORT = 80;
    public static final int HTTPS_DEFAULT_PORT = 443;
    
    private String protocol = null;
    private String host = null;
    private int port = 0;
    private String file = null;

    public ApiRestEndPoint()
    {
        super();
        this.protocol = HTTP_PROTOCOL;
        this.port = HTTP_DEFAULT_PORT;
        this.file = "/";
    }

    public ApiRestEndPoint(String protocol, String host, int port, String file)
    {
        super();
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.file = (file != null) ? file : "/";
    }

    public ApiRestEndPoint(String protocol, String host, String file)
    {
        super();
        this.protocol = protocol;
        this.host = host;
        this.port = ((this.protocol != null) && (this.protocol.equals(HTTPS_PROTOCOL))) ? HTTPS_DEFAULT_PORT : HTTP_DEFAULT_PORT;
        this.file = (file != null) ? file : "/";
    }

    public ApiRestEndPoint(String host, String file)
    {
        super();
        this.protocol = HTTP_PROTOCOL;
        this.host = host;
        this.port = HTTP_DEFAULT_PORT;
        this.file = (file != null) ? file : "/";
    }

    public ApiRestEndPoint(String host)
    {
        super();
        this.protocol = HTTP_PROTOCOL;
        this.host = host;
        this.port = HTTP_DEFAULT_PORT;
        this.file = "/";
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        this.protocol = protocol;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getFile()
    {
        return file;
    }

    public void setFile(String file)
    {
        this.file = file;
    }

    public String getEndPoint()
    {
        URL url = null;
        String out = null;
        String debug = null;
        
        try
        {
            url = new URL(this.protocol, this.host, this.port, this.file);
            out = url.toExternalForm();
        }
        catch (MalformedURLException e)
        {
            debug = (new StringBuilder())
                    .append("End point not valid: \n")
                    .append(this.toString()).append("\n")
                    .append("in method ApiRestEndPoint.getEndPoint").toString();

            Log.error(this, debug);
        }
        
        
        return out;
    }
    
    public boolean isValid()
    {
        boolean control = false;

        control = ((this.protocol != null) && (this.host != null) && (this.port > 0));

        return control;
    }

    @Override
    public String toString()
    {
        ReflectionToStringBuilder tsb = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return tsb.toString();
    }
}
