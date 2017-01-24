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

package gedi.util.orm.special;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.TreeMap;

public class TreeMapSpecialSerializer implements SpecialBinarySerializer<TreeMap<?,?>> {

	@Override
	public void serialize(OrmSerializer parent, BinaryWriter out, TreeMap<?,?> object) throws IOException {
		out.putCInt(object.size());
		for (Object k : object.keySet()) {
			parent.serialize(out, k);
			parent.serialize(out, object.get(k));
		}
	}

	@Override
	public TreeMap<?,?> deserialize(OrmSerializer parent, BinaryReader in) throws IOException {
		int c = in.getCInt();
		TreeMap re = new TreeMap();
		for (int i=0; i<c; i++)
			re.put(parent.deserialize(in),parent.deserialize(in));
		return re;
	}

}
