package com.santander.restservices;

public interface ApiRestModel
{

	public void setMimeType(String mimeType);
	
	public String getMimeType();
    
	public void pushTextMessage(String message);

    public String pullTextMessage();

    public void pushBinaryMessage(byte[] message);

    public byte[] pullBinaryMessage();

    public boolean checkModelDataLoaded();

    public void loadModelData(ApiRestModel model);

    public Class<? extends ApiRestModel> retriveModelClass();

}
