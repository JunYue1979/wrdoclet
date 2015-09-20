package net.winroad.wrdoclet.data;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.winroad.wrdoclet.ConfigurationImpl;
import net.winroad.wrdoclet.builder.AbstractDocBuilder;
import net.winroad.wrdoclet.builder.DubboDocBuilder;
import net.winroad.wrdoclet.builder.RESTDocBuilder;
import net.winroad.wrdoclet.builder.SOAPDocBuilder;

import com.sun.tools.doclets.internal.toolkit.Configuration;

public class WRDoc {
	private Map<String, List<OpenAPI>> taggedOpenAPIs = new HashMap<String, List<OpenAPI>>();

	private List<AbstractDocBuilder> builders = new LinkedList<AbstractDocBuilder>();

	// The collection of tag name in this Doc.
	private Set<String> wrTags = new HashSet<String>();

	private Configuration configuration;

	private String docGeneratedDate;
	
	public Configuration getConfiguration() {
		return configuration;
	}

	public Set<String> getWRTags() {
		return this.wrTags;
	}

	public Map<String, List<OpenAPI>> getTaggedOpenAPIs() {
		return taggedOpenAPIs;
	}
	
	public String getDocGeneratedTime() {
		return this.docGeneratedDate;
	}

	public WRDoc(Configuration configuration) {
		this.configuration = configuration;
		Calendar c = Calendar.getInstance();
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"); 
		this.docGeneratedDate = df.format(c.getTime());
		this.builders.add(new RESTDocBuilder(this));
		this.builders.add(new SOAPDocBuilder(this));
		String dubboConfigPath = ((ConfigurationImpl) this.configuration).dubboconfigpath;
		if (dubboConfigPath != null && !dubboConfigPath.isEmpty()) {
			this.builders.add(new DubboDocBuilder(this));
		}
		this.build();
	}

	private void build() {
		for (AbstractDocBuilder builder : this.builders) {
			builder.buildWRDoc();
		}
	}
}
