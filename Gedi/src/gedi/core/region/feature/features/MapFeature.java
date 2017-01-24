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

package gedi.core.region.feature.features;

import gedi.core.data.annotation.Transcript;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Object.class)
public class MapFeature extends AbstractFeature<Object> {

	public MapFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 1;
	}

	@Override
	protected void accept_internal(Set<Object> values) {
		values.addAll(getInput(0));
	}

	@Override
	public GenomicRegionFeature<Object> copy() {
		MapFeature re = new MapFeature();
		re.copyProperties(this);
		return re;
	}
	
}
