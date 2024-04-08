package com.santander.restservices;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Store the connection properties for the api rest service
 * 
 */
public class ApiRestProxy
{
    private String host = null;
    private int port = 8080;
    private String user = null;
    private String password = null;

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

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public boolean isValid()
    {
        boolean control = false;

        control = ((this.host != null) && (this.port > 0) && (this.user != null) && (this.password != null));

        return control;
    }

    @Override
    public String toString()
    {
        ReflectionToStringBuilder tsb = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return tsb.toString();
    }
}
