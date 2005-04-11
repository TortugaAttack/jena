/******************************************************************
 * File:        RuleMap.java
 * Created by:  Dave Reynolds
 * Created on:  04-Mar-2004
 * 
 * (c) Copyright 2004, 2005 Hewlett-Packard Development Company, LP, all rights reserved.
 * [See end of file]
 * $Id: RuleMap.java,v 1.8 2005-04-11 11:29:11 der Exp $
 *****************************************************************/
package jena;


import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.*;
import com.hp.hpl.jena.reasoner.rulesys.builtins.BaseBuiltin;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;

import jena.cmdline.*;
import java.util.*;
import java.io.*;

/**
 * General command line utility to process one RDF file into another
 * by application of a set of forward chaining rules.
 * <pre>
 * Usage:  RuleMap [-il inlang] [-ol outlang] [-d] rulefile infile
 * </pre>
 * The resulting RDF data is written to stdout in format <code>outlang</code
 * (default N3). If <code>-d</code> is given then only the deductions
 * generated by the rules are output. Otherwise all data including any input
 * data (other than any removed triples) is output.
 * <p>
 * Rules are permitted an additional action "deduce" which forces triples
 * to be added to the deductions graph even if they are already known (for use
 * in deductions only mode). 
 * </p>
 * 
 * @author <a href="mailto:der@hplb.hpl.hp.com">Dave Reynolds</a>
 * @version $Revision: 1.8 $ on $Date: 2005-04-11 11:29:11 $
 */
public class RuleMap {
    
    /**
     * Load a set of rule definitions including processing of
     * comment lines and any initial prefix definition lines.
     * Also notes the prefix definitions for adding to a later inf model.
     */
    public static List loadRules(String filename, Map prefixes) throws IOException {
        String fname = filename;
        if (fname.startsWith("file:///")) {
            fname = File.separator + fname.substring(8);
        } else if (fname.startsWith("file:/")) {
            fname = File.separator + fname.substring(6);
        } else if (fname.startsWith("file:")) {
            fname = fname.substring(5);
        }

        BufferedReader src = FileUtils.openResourceFile(fname);
        return loadRules(src, prefixes);
    }
    
    /**
     * Load a set of rule definitions including processing of
     * comment lines and any initial prefix definition lines.
     * Also notes the prefix definitions for adding to a later inf model.
     */
    public static List loadRules(BufferedReader src, Map prefixes) throws IOException {
        Rule.Parser parser = Rule.rulesParserFromReader(src);
        List rules = Rule.parseRules(parser);
        prefixes.putAll(parser.getPrefixMap());
        return rules;
    }
    
    /**
     * Internal implementation of the "deduce" primitve.
     * This takes the form <code> ... -> deduce(s, p, o)</code>
     */
    static class Deduce extends BaseBuiltin {

        /**
         * Return a name for this builtin, normally this will be the name of the 
         * functor that will be used to invoke it.
         */
        public String getName() {
            return "deduce";
        }    
   
        /**
         * Return the expected number of arguments for this functor or 0 if the number is flexible.
         */
        public int getArgLength() {
            return 3;
        }

        /**
         * This method is invoked when the builtin is called in a rule head.
         * Such a use is only valid in a forward rule.
         * @param args the array of argument values for the builtin, this is an array 
         * of Nodes.
         * @param length the length of the argument list, may be less than the length of the args array
         * for some rule engines
         * @param context an execution context giving access to other relevant data
         */
        public void headAction(Node[] args, int length, RuleContext context) {
            if (context.getGraph() instanceof FBRuleInfGraph) {
                Triple t = new Triple(args[0], args[1], args[2]);
                ((FBRuleInfGraph)context.getGraph()).addDeduction(t);
            } else {
                throw new BuiltinException(this, context, "Only usable in FBrule graphs");
            }
        }
    }
    
    /**
     * General command line utility to process one RDF file into another
     * by application of a set of forward chaining rules. 
     * <pre>
     * Usage:  RuleMap [-il inlang] [-ol outlang] -d infile rulefile
     * </pre>
     */
    public static void main(String[] args) {
        try {
            
            // Parse the command line
            CommandLine cl = new CommandLine();
            String usage = "Usage:  RuleMap [-il inlang] [-ol outlang] [-d] rulefile infile"; 
            cl.setUsage(usage);
            cl.add("il", true);
            cl.add("ol", true);
            cl.add("d", false);
            cl.process(args);
            if (cl.items().size() != 2) {
                System.err.println(usage);
                System.exit(1);
            }
            
            // Load the input data
            Arg il = cl.getArg("il");
            String inLang = (il == null) ? null : il.getValue();
            Model inModel = FileManager.get().loadModel((String)cl.items().get(1), inLang);
            
            // Determine the type of the output
            Arg ol = cl.getArg("ol");
            String outLang =  (ol == null) ? "N3" : ol.getValue();
            
            Arg d = cl.getArg("d");
            boolean deductionsOnly = (d != null);
            
            // Fetch the rule set and create the reasoner
            BuiltinRegistry.theRegistry.register(new Deduce());
            Map prefixes = new HashMap();
            List rules = loadRules((String)cl.items().get(0), prefixes);
            Reasoner reasoner = new GenericRuleReasoner(rules);
            
            // Process
            InfModel infModel = ModelFactory.createInfModel(reasoner, inModel);
            infModel.prepare();
            infModel.setNsPrefixes(prefixes);
            
            // Output
            PrintWriter writer = new PrintWriter(System.out);
            if (deductionsOnly) {
                Model deductions = infModel.getDeductionsModel();
                deductions.setNsPrefixes(prefixes);
                deductions.setNsPrefixes(inModel);
                deductions.write(writer, outLang);
            } else {
                infModel.write(writer, outLang);
            }
            writer.close();
            
        } catch (Throwable t) {
            System.err.println("An error occured: \n" + t);
            t.printStackTrace();
        }
    }

}

/*
    (c) Copyright 2004, 2005 Hewlett-Packard Development Company, LP
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met:

    1. Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer in the
       documentation and/or other materials provided with the distribution.

    3. The name of the author may not be used to endorse or promote products
       derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
    IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
    OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
    IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
    NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
    DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
    THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
    (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
    THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
