/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.gui.genovis.tracks.categorizer;

import gedi.core.data.annotation.NameProvider;
import gedi.util.dynamic.DynamicObject;

import java.util.function.ToIntFunction;

public class NameCategorizer implements ToIntFunction<NameProvider> {

	
	private DynamicObject features;

	private String getType(NameProvider a) {
		return a.getName();
	}

	
	public void setFeatures(DynamicObject features) {
		this.features = features;
	}


	@Override
	public int applyAsInt(NameProvider a) {
		if (features==null) return -1;
		String t = getType(a);
		if (t==null) return -1;
		return features.getEntry(t).getEntry("category").asInt();
	}
	
	


}
