package driftmodelintegration.core;

/*
 *  streams library
 *
 *  Copyright (C) 2011-2012 by Christian Bockermann, Hendrik Blom
 * 
 *  streams is a library, API and runtime environment for processing high
 *  volume data streams. It is composed of three submodules "stream-api",
 *  "stream-core" and "stream-runtime".
 *
 *  The streams library (and its submodules) is free software: you can 
 *  redistribute it and/or modify it under the terms of the 
 *  GNU Affero General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any 
 *  later version.
 *
 *  The stream.ai library (and its submodules) is distributed in the hope
 *  that it will be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * This interface defines a single data item.
 * </p>
 * 
 * @author Christian Bockermann &lt;chris@jwall.org&gt;
 * 
 */
public interface Data extends Map<String, Serializable>, Serializable {

	/**
	 * attributes starting with this prefix are considered special and will not
	 * be regarded for training classifiers
	 */
	public final static String SPECIAL_PREFIX = "_";

	/**
	 * attributes starting with this prefix are considered as hidden and must
	 * not be processed (neither removed nor modified) by data mappers.
	 * 
	 * @deprecated
	 */
	public final static String HIDDEN_PREFIX = "._";

	/**
	 * Attributes starting with an '@' character are regarded as annotations,
	 * i.e. referring to data transformation parameters (normalization,...)
	 */
	public final static String ANNOTATION_PREFIX = "@";

	/**
	 * Attributes that refer to predicted values, i.e. the class of an instance
	 * predicted by some learning/prediction-model are prefixed with this
	 * string.
	 */
	public final static String PREDICTION_PREFIX = ANNOTATION_PREFIX
			+ "prediction";
}