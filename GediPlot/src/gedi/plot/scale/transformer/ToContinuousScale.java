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

package gedi.plot.scale.transformer;

import gedi.plot.GPlotContext;
import gedi.plot.scale.GPlotTicks;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;

public interface ToContinuousScale {

	double transform(GPlotContext ctx, DataColumn<?> col, int row);
	double transformToUnit(GPlotContext ctx, DataColumn<?> col, int row);
	
	/**
	 * In the space that is supposed to be presented to the user (i.e. pre space)
	 * @param ctx
	 * @return
	 */
	GPlotTicks getTicks(GPlotContext ctx);
	
}
