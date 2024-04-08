package com.isban.api.webservice;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import calypsox.webservices.annotations.SgtEndpoint;
import com.calypso.tk.bo.document.AdviceDocument;
import com.calypso.tk.core.Log;
import com.calypso.tk.service.DSConnection;


@Path("/hello")
public class HelloWorldRestResource extends SantanderRestWebServices {


	public HelloWorldRestResource(){
		super();
	}


	@GET
	@SgtEndpoint
	@Path("/helloworld")
	@Produces(MediaType.APPLICATION_JSON)
	public Response helloWorld() {
		String jsonMessage = "Hello World Calypso STC";
		Class[] paramTypes = null;
		if(isEngineServer(this.getClass(), new Throwable().getStackTrace()[0].getMethodName(), paramTypes)){
			return Response.status(Response.Status.ACCEPTED).type(MediaType.APPLICATION_JSON).entity(jsonMessage).build();
		}
		else{
			jsonMessage = buildJsonResponse("Request Discarded in this site");
			Log.info(this, jsonMessage);
			return Response.status(Response.Status.NOT_ACCEPTABLE).type(MediaType.APPLICATION_JSON).entity(jsonMessage).build();
		}
	}


	@GET
	@Path("/calypsoinfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCalypsoInfo() {
		String jsonMessage = "";
		Class[] paramTypes = null;
		if(isEngineServer(this.getClass(), new Throwable().getStackTrace()[0].getMethodName(), paramTypes)){
			jsonMessage = buildJsonResponse(DSConnection.getDefault().getAppName());
			return Response.status(Response.Status.ACCEPTED).type(MediaType.APPLICATION_JSON).entity(jsonMessage).build();
		}
		else{
			jsonMessage = buildJsonResponse("Request Discarded in this site");
			Log.info(this, jsonMessage);
			return Response.status(Response.Status.NOT_ACCEPTABLE).type(MediaType.APPLICATION_JSON).entity(jsonMessage).build();
		}
	}


	@POST
	@Path("/savejson")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public String saveJson(InputStream inputJson) {
		Class[] paramTypes = new Class[1];
		paramTypes[0] = InputStream.class;
		if(isEngineServer(this.getClass(), new Throwable().getStackTrace()[0].getMethodName(), paramTypes)) {
			StringBuffer output = new StringBuffer();
			StringBuffer documentText = new StringBuffer();
			try {
				AdviceDocument document = new AdviceDocument();
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputJson));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (documentText.length() > 0) {
						documentText.append('\n');
					}
					documentText.append(line);
				}
				document.setDocument(documentText);
				output.append("Received JSON:\n");
				output.append(documentText.toString());
			} catch (IOException e) {
				String errorMessage = "Could not read input JSON";
				Log.error(this, errorMessage, e);
				output.append(errorMessage);
				output.append('\n');
				output.append(e.toString());
			}
			return output.toString();
		}
		else{
			return Response.Status.NOT_ACCEPTABLE.getReasonPhrase();
		}
	}


}
