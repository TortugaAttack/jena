/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package projects.prefixes;

import org.apache.jena.graph.Node;

public class PrefixLib {

    /**
     * Remove ":" from a prefix if necessary to make it canonical.
     * @param prefix
     * @return prefix, without colon.
     */
    public static String canonicalPrefix(String prefix) {
        if ( prefix.endsWith(":") )
            return prefix.substring(0, prefix.length() - 1);
        return prefix;
    }

    static Node canonicalGraphName(Node graphName) {
        if ( graphName == null )
            return DatasetPrefixesMem.dftGraph;
        if ( DatasetPrefixesMem.dftGraph2.equals(graphName) )
            return DatasetPrefixesMem.dftGraph;
        return graphName;
    }
    
}
