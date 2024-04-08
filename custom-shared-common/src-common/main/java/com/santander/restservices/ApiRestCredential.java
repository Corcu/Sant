package com.santander.restservices;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Store the connection properties for the api rest service
 * 
 */
public class ApiRestCredential
{
    public static final String OPEN_CREDENTIAL_TYPE = "Open";
    public static final String BASIC_CREDENTIAL_TYPE = "Basic";
    public static final String BEARER_CREDENTIAL_TYPE = "Bearer";

    private String type;
    private String user;
    private String password;
    private String token;

    public ApiRestCredential()
    {
        super();
        this.type = OPEN_CREDENTIAL_TYPE;
    }

    public ApiRestCredential(String type)
    {
        super();
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
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

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public boolean isValid()
    {
        boolean control = false;

        if (this.type != null)
        {
            if (this.type.equals(OPEN_CREDENTIAL_TYPE))
            {
                control =  true;
            }
            else
            if (this.type.equals(BASIC_CREDENTIAL_TYPE))
            {
                control =  ((this.user != null) && (this.password != null));
            }
            else
            if (this.type.equals(BEARER_CREDENTIAL_TYPE))
            {
                control =  (this.token != null);
            }
        }

        return control;
    }

    @Override
    public String toString()
    {
        ReflectionToStringBuilder tsb = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return tsb.toString();
    }
}
