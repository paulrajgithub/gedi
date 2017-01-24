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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ReadMismatchTypeFeature extends AbstractFeature<String> {

	

	private String between = "->";

	public ReadMismatchTypeFeature() {
		minInputs = maxInputs = 0;
	}
	
	
	
	public boolean dependsOnData() {
		return true;
	}
	
	public void setBetween(String between) {
		this.between = between;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			
			if (d.isMismatch(0, i)) {
				values.add(d.getMismatchGenomic(0, i)+between+d.getMismatchRead(0, i));
			}
			
		}
		
	}



	@Override
	public GenomicRegionFeature<String> copy() {
		ReadMismatchTypeFeature re = new ReadMismatchTypeFeature();
		re.copyProperties(this);
		re.between = between;
		return re;
	}

	

}
