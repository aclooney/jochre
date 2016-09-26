///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2012 Assaf Urieli
//
//This file is part of Jochre.
//
//Jochre is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Jochre is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Jochre.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.jochre;

import java.util.Locale;

import com.joliciel.jochre.lang.DefaultLinguistics;
import com.joliciel.jochre.lang.Linguistics;
import com.joliciel.talismane.utils.ObjectCache;
import com.joliciel.talismane.utils.SimpleObjectCache;
import com.typesafe.config.Config;

/**
 * A class storing session-wide reference data.
 * 
 * @author Assaf Urieli
 *
 */
public class JochreSession {
	private final Locale locale;
	private Linguistics linguistics;
	private double junkConfidenceThreshold = 0.75;
	private final Config config;
	private final ObjectCache objectCache;

	public JochreSession(Config config) {
		this.config = config;
		this.locale = Locale.forLanguageTag(config.getString("jochre.locale"));
		this.objectCache = new SimpleObjectCache();
	}

	public Locale getLocale() {
		return locale;
	}

	public Linguistics getLinguistics() {
		if (this.linguistics == null) {
			this.linguistics = DefaultLinguistics.getInstance(this.locale);
		}
		return linguistics;
	}

	public void setLinguistics(Linguistics linguistics) {
		this.linguistics = linguistics;
	}

	/**
	 * The average confidence below which a paragraph is considered to be junk,
	 * when considering all of its letters.
	 */
	public double getJunkConfidenceThreshold() {
		return junkConfidenceThreshold;
	}

	public void setJunkConfidenceThreshold(double junkConfidenceThreshold) {
		this.junkConfidenceThreshold = junkConfidenceThreshold;
	}

	public Config getConfig() {
		return config;
	}

	public ObjectCache getObjectCache() {
		return objectCache;
	}
}
