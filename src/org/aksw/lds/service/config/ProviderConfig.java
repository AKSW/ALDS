package org.aksw.lds.service.config;

import java.util.List;
import java.util.Map;

public class ProviderConfig {
	/***
	 * URI used to query provider 
	 */
	public String ProviderURI;
	
	/***
	 * Rdf prefixes used to generate RDF
	 */
	public Map<String, String> RdfPrefixes;
	
	/***
	 * Columns descriptions
	 */
	public List<ColumnConfig> Columns;
	
	/***
	 * Provider prefix used in external URL generation 
	 */
	public String ProviderPrefix;
	
	/***
	 * URL Fields used in extenal URL generation
	 */
	public List<String> ProviderURIFields;
}