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

package executables;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTree;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.NameAttributeMapAnnotation;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.io.text.fasta.FastaEntry;
import gedi.util.io.text.fasta.FastaFile;
import gedi.util.io.text.genbank.GenbankFile;

import java.io.File;
import java.io.IOException;

public class GenbankIndex {

	
	public static void main(String[] args) throws IOException {
		
		if (args.length!=1 || !new File(args[0]).exists()) {
			System.err.println("ConvertGenbank <gb-file>");
			System.exit(1);
		}
		
		GenbankFile file = new GenbankFile(args[0]);
		
		String pref = args[0];
		pref = pref.substring(0, pref.lastIndexOf('.'));
		
		FastaFile ff = new FastaFile(pref+".fasta");
		ff.startWriting();
		ff.writeEntry(new FastaEntry(file.getAccession(),file.getSource().toUpperCase()));
		ff.finishWriting();
		ff.obtainDefaultIndex().create(ff);
		
		CenteredDiskIntervalTreeStorage<NameAttributeMapAnnotation> full = new CenteredDiskIntervalTreeStorage<NameAttributeMapAnnotation>(pref+".full.cit", NameAttributeMapAnnotation.class);
		ReferenceSequence ref = Chromosome.obtain(file.getAccession(),Strand.Plus);
		
		full.fill(file.featureIterator().map(f->
			new ImmutableReferenceGenomicRegion<NameAttributeMapAnnotation>(
					ref.toStrand(f.getPosition().getStrand()),
					f.getPosition().toGenomicRegion(),
					new NameAttributeMapAnnotation(f.getFeatureName(), f.toSimpleMap()))
		));
		
		
	}
	
}
