package org.aksw.lds.service.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class ConfigManager {
	public static final String URL_SEPARATOR = "__";
	
	private static final String TAG = "BackgroundService_ConfigManager";
	
	ArrayList<ProviderConfig> configs;
	
	Context appContext;
	
	public ConfigManager(Context ctx)
	{
		appContext = ctx;
		
		configs = new ArrayList<ProviderConfig>();
		
		// read config
		JSONObject obj = readConfig("phones.json");
		ProviderConfig conf = prepareConfig(obj);
		configs.add(conf);
		
		// test fetching data
 		//fetchAllData(conf);
		//fetchData(conf, "_id = 5 AND display_name = \"Some Other Guy\"");
	}
	
	public String getDataFor(ConfigRequest req)
	{
		// init res
		String res = "";
		// prepare provider var
		ProviderConfig cfg = null;
		
		// find provider
		for(int i = 0; i < configs.size(); i++){
			//Log.i(TAG, "'"+configs.get(i).ProviderPrefix + "' == '" + req.ProviderPrefix + "' => " + configs.get(i).ProviderPrefix.equalsIgnoreCase(req.ProviderPrefix) );
			if( configs.get(i).ProviderPrefix.equalsIgnoreCase(req.ProviderPrefix) ){
				cfg = configs.get(i); 
				//Log.i(TAG, "Provider found - "+req.ProviderPrefix+" : "+i);
				break;
			}
		}
		
		// die if no config found
		if(cfg == null){
			res = "No valid config found!";
			return res;
		}
		
		// if there's no resource, fetch all stuff 
		if(req.ResourceString.length() <= URL_SEPARATOR.length()){
			res = fetchAllData(cfg);
		}else{
			// break resource to parts
			String[] resource = req.ResourceString.split(URL_SEPARATOR);
			//Log.i(TAG, resource.toString());
			
			// die if resource parts doesn't match configured pattern
			if(resource.length != cfg.ProviderURIFields.size()){
				res = "Resource ID is incorrect!";
				return res;
			}
			
			// form conditions
			String conditions = "";
			for(int i = 0; i < resource.length; i++){
				conditions += cfg.ProviderURIFields.get(i)+" = \""+URLDecoder.decode(resource[i])+"\"";
				if( i < (resource.length-1) ) conditions += " AND ";
			}
			//Log.i(TAG, "Conditional string: "+conditions);
			
			res = fetchData(cfg, conditions);
			
			// cleanup
			conditions = null;
			resource = null;
		}
		
		// cleanup
		cfg = null;
		
		// force gc
		System.gc();
		
		return res;
	}
	
	private ProviderConfig prepareConfig(JSONObject config){
		ProviderConfig conf = null;
		
		try {
			conf = new ProviderConfig();
			// get provider URI
			conf.ProviderURI = config.getString("provider_uri");
			
			// fill prefixes
			conf.RdfPrefixes = new HashMap<String, String>();
			JSONObject prefs = config.getJSONObject("rdf_perifxes");
			Iterator<String> iter = prefs.keys();
			while(iter.hasNext()){
		        String key = iter.next();
		        String value = prefs.getString(key);
		        conf.RdfPrefixes.put(key, value);
		    }
		    
		    // fill columns
		    conf.Columns = new ArrayList<ColumnConfig>();
		    JSONArray cols = config.getJSONArray("columns");
		    for(int i = 0; i < cols.length(); i++){
		    	ColumnConfig colConf = new ColumnConfig();
		    	colConf.ID = cols.getJSONObject(i).getString("id");
		    	colConf.Name = cols.getJSONObject(i).getString("name");
		    	colConf.Predicate = cols.getJSONObject(i).getString("predicate");
		    	conf.Columns.add(colConf);
		    }
		    
		    // url generation stuff
		    JSONObject urlgen = config.getJSONObject("uri_generation");
		    // get prefix
		    conf.ProviderPrefix = urlgen.getString("prefix");
		    // fill url generation fields
		    conf.ProviderURIFields = new ArrayList<String>();
		    JSONArray fields = urlgen.getJSONArray("values");
		    for(int i = 0; i < fields.length(); i++){
		    	conf.ProviderURIFields.add( fields.getString(i) );
		    }
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return conf;
	}
	
	private JSONObject readConfig(String configName) {
		try {
		    BufferedReader reader = new BufferedReader(new InputStreamReader(appContext.getAssets().open(configName), "UTF-8")); 

		    // do reading, usually loop until end of file reading
		    String file = "";
		    String mLine = reader.readLine();
		    while (mLine != null) {
		       //process line
		       file += mLine;
		       mLine = reader.readLine(); 
		    }
		    reader.close();
		    
		    JSONObject jObject = new JSONObject(file);
            
            return jObject;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private JSONObject readConfigFromSD(String configName){
		try {
            File dir = Environment.getExternalStorageDirectory();
            File yourFile = new File(dir, "path/to/the/file/inside/the/sdcard.ext");
            FileInputStream stream = new FileInputStream(yourFile);
            String jString = null;
            try {
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                /* Instead of using default, pass in a decoder. */
                jString = Charset.defaultCharset().decode(bb).toString();
            } finally {
            	  stream.close();
            }

            JSONObject jObject = new JSONObject(jString);
            
            return jObject;
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
		return null;
	}
	
	private String fetchAllData(ProviderConfig config) {	
		Uri uri = Uri.parse(config.ProviderURI);
		String[] projection = new String[config.Columns.size()];
		for(int i = 0; i < config.Columns.size(); i++){
			//Log.i("COLUMN_"+i, config.Columns.get(i).ID);
			projection[i] = config.Columns.get(i).ID;
		}

		Cursor people = appContext.getContentResolver().query(uri, projection, null, null, null);
		
		// make jena model
		Model m = ModelFactory.createDefaultModel();
		
		// set prefixes
		Iterator<Entry<String, String>> it = config.RdfPrefixes.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			m.setNsPrefix(pairs.getKey(), pairs.getValue());
		}
		
		// make properties
		Map<String, Property> properties = new HashMap<String, Property>();
		for(int i = 0; i < config.Columns.size(); i++){
			ColumnConfig c = config.Columns.get(i);
			if(c.Predicate != "null"){
				String url = preparePredicate(c.Predicate, config);
				properties.put(c.ID, m.createProperty(url));
			} 
		}	
		
		Map<String, String> data;
		Resource r;

		// extract data
		//Log.i("START", "Extracting");
		people.moveToFirst();
		do {
			data = new HashMap<String, String>();
			
			for(int i = 0; i < config.Columns.size(); i++){
				// get column id & index
				String columnId = config.Columns.get(i).ID;
				int columnIndex = people.getColumnIndex(columnId);
				// get value
				String value = people.getString(columnIndex);
				
				// log
				//Log.i("DATA", columnId + "[" + columnIndex + "]: " + value);
				// store
				data.put(columnId, value);
				
				// cleanup
				columnId = null;
				value = null;
			}
			
			// prepare resource
			String itemUrl = prepareItemUrl(data, config);
			//Log.i("ITEM_URL", itemUrl);
			// create resource
			r = m.createResource(itemUrl);
			//Log.i("RESOURCE_CREATE", "OK");
			
			// add property & value
			Iterator<Entry<String, String>> dit = data.entrySet().iterator();
			while(dit.hasNext()){
				Map.Entry<String,String> pairs = (Map.Entry<String,String>)dit.next();
				Property p = properties.get(pairs.getKey());
				
				if(p != null){
					r.addProperty(p, pairs.getValue());
					//Log.i("RESOURCE_ADD_PROP", p.toString() + " " + pairs.getValue());
				}
				
				// cleanup
				p = null;
			}
			
			// cleanup
			dit = null;
			itemUrl = null;
		} while (people.moveToNext());
		
		// output
		String syntax = "TURTLE";// "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "N3" and "TURTLE".
		StringWriter out = new StringWriter();
		m.write(out, syntax);
		String result = out.toString();
		//Log.i("OUTPUT_RDF", result);
		
		// cleanup
		uri = null;
		projection = null;
		people = null;
		m = null;
		properties = null;
		it = null;
		data = null;
		r = null;
		syntax = null;
		out = null;
		
		return result;
	}
	
	private String fetchData(ProviderConfig config, String conditions) {	
		Uri uri = Uri.parse(config.ProviderURI);
		String[] projection = new String[config.Columns.size()];
		for(int i = 0; i < config.Columns.size(); i++){
			//Log.i("COLUMN_"+i, config.Columns.get(i).ID);
			projection[i] = config.Columns.get(i).ID;
		}

		Cursor people = appContext.getContentResolver().query(uri, projection, conditions, null, null);
		
		// make jena model
		Model m = ModelFactory.createDefaultModel();
		
		// set prefixes
		Iterator<Entry<String, String>> it = config.RdfPrefixes.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			m.setNsPrefix(pairs.getKey(), pairs.getValue());
		}
		
		// make properties
		Map<String, Property> properties = new HashMap<String, Property>();
		for(int i = 0; i < config.Columns.size(); i++){
			ColumnConfig c = config.Columns.get(i);
			if(c.Predicate != "null"){
				String url = preparePredicate(c.Predicate, config);
				properties.put(c.ID, m.createProperty(url));
			} 
		}	
		
		Map<String, String> data;
		Resource r;

		// extract data
		//Log.i("START", "Extracting");
		people.moveToFirst();
		do {
			data = new HashMap<String, String>();
			
			for(int i = 0; i < config.Columns.size(); i++){
				// get column id & index
				String columnId = config.Columns.get(i).ID;
				int columnIndex = people.getColumnIndex(columnId);
				// get value
				String value = people.getString(columnIndex);
				
				// log
				//Log.i("DATA", columnId + "[" + columnIndex + "]: " + value);
				// store
				data.put(columnId, value);
				
				// cleanup
				value = null;
				columnId = null;
			}
			
			// prepare resource
			String itemUrl = prepareItemUrl(data, config);
			//Log.i("ITEM_URL", itemUrl);
			// create resource
			r = m.createResource(itemUrl);
			//Log.i("RESOURCE_CREATE", "OK");
			
			// add property & value
			Iterator<Entry<String, String>> dit = data.entrySet().iterator();
			while(dit.hasNext()){
				Map.Entry<String,String> pairs = (Map.Entry<String,String>)dit.next();
				Property p = properties.get(pairs.getKey());
				
				if(p != null){
					r.addProperty(p, pairs.getValue());
					//Log.i("RESOURCE_ADD_PROP", p.toString() + " " + pairs.getValue());
				}
				
				// cleanup
				p = null;
			}
			
			// cleanup
			dit = null;
			itemUrl = null;
			
		} while (people.moveToNext());
		
		// output
		String syntax = "TURTLE";// "RDF/XML", "RDF/XML-ABBREV", "N-TRIPLE", "N3" and "TURTLE".
		StringWriter out = new StringWriter();
		m.write(out, syntax);
		String result = out.toString();
		//Log.i("OUTPUT_RDF", result);
		
		// cleanup
		uri = null;
		projection = null;
		people = null;
		m = null;
		properties = null;
		it = null;
		data = null;
		r = null;
		syntax = null;
		out = null;
		
		return result;
	}
	
	private String prepareItemUrl(Map<String, String> data, ProviderConfig config)
	{
		try{
			String Url = "http://localhost/"+URLEncoder.encode(config.ProviderPrefix, "UTF-8")+"/";
			
			for(int i = 0; i < config.ProviderURIFields.size(); i++){
				String field = config.ProviderURIFields.get(i);
				String val = data.get(field);
				if(val != null) Url += URLEncoder.encode(val, "UTF-8");
				if(i < (config.ProviderURIFields.size()-1) ) Url += URL_SEPARATOR;
			}
			
			return Url;
		}catch(Exception e){
			return null;
		}
	}
	
	private String preparePredicate(String predicate, ProviderConfig config)
	{
		String newPredicate = predicate;
		
		Iterator<Entry<String, String>> it = config.RdfPrefixes.entrySet().iterator();
		while(it.hasNext()){
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			String key = pairs.getKey()+":";
			String val = pairs.getValue();
			newPredicate = newPredicate.replace(key, val);
		}
		
		return newPredicate;
	}
}
