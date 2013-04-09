package org.aksw.lds.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.aksw.lds.service.config.ConfigManager;
import org.aksw.lds.service.config.ConfigRequest;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import android.util.Log;

public class JettyServer {
	public static final int SERVERPORT = 1234;
	public Handler handler;
	
	private static final String TAG = "BackgroundService_JETTY";
	
	private ConfigManager configManager;
	
	public JettyServer(ConfigManager cm)
	{
		configManager = cm;
		
		// log init
		Log.i("JettyServer", "Init");
		
		// init stuff
		init();
	}
	
	private void init()
	{
		initHandler();
		
		// create server
		Server server = new Server(SERVERPORT);
	    server.setHandler(handler);
	    try{
	    	server.start();
	    }catch(Exception e){
	    	Log.e("Jetty", e.toString());
	    }
	}
	
	private void initHandler()
	{
		handler = new AbstractHandler()
	    {	
	    	//@Override
			public void handle(String target, Request request, HttpServletRequest MainRequestObject,
					HttpServletResponse response) throws IOException, ServletException
			{
				try
				{
					//How to get Query String/
					Log.i("Query String", target);
					
					ConfigRequest dataReq = parseTarget(target);
					String data = configManager.getDataFor(dataReq);
					
					//How To Send Responce Back
					response.setContentType("text/turtle");
		            response.setStatus(HttpServletResponse.SC_OK);
		            response.getWriter().print(data);
		            ((Request)MainRequestObject).setHandled(true);				
				}
	        	catch (Exception ex)
	        	{
	        		Log.i("Error", ex.getMessage());
				}
			}			
	    };
	}
	
	private ConfigRequest parseTarget(String target)
	{
		ConfigRequest res = new ConfigRequest();
		
		Pattern pattern = Pattern.compile("/(.+?)/(.*)");
		Matcher matcher = pattern.matcher(target);
		// Check all occurrences
	    while (matcher.find()) {
	    	//Log.i(TAG, "Start index: " + matcher.start());
	    	//Log.i(TAG, " End index: " + matcher.end() + " ");
	    	int len = matcher.groupCount()+1;
	    	for(int i = 0; i < len; i++){
	    		Log.i(TAG, "MATCH ["+i+"]: "+matcher.group(i));
	    		if(i == 1){
	    			res.ProviderPrefix = matcher.group(i);
	    		}else if(i == 2){
	    			res.ResourceString = matcher.group(i);
	    		}
	    	}
	    }
	    
	    return res;
	}
}
