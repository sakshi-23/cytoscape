// cytoscape.java  

/** Copyright (c) 2002 Institute for Systems Biology and the Whitehead Institute
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** any later version.
 ** 
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and the
 ** Institute for Systems Biology and the Whitehead Institute 
 ** have no obligations to provide maintenance, support,
 ** updates, enhancements or modifications.  In no event shall the
 ** Institute for Systems Biology and the Whitehead Institute 
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if the
 ** Institute for Systems Biology and the Whitehead Institute 
 ** have been advised of the possibility of such damage.  See
 ** the GNU Lesser General Public License for more details.
 ** 
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **/

// $Revision$
// $Date$ 
// $Author$
//-------------------------------------------------------------------------------------
package cytoscape;
//-------------------------------------------------------------------------------------
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import java.util.*;
import java.util.logging.*;

import cytoscape.data.*;
import cytoscape.data.servers.*;
import cytoscape.view.CyWindow;

import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.Options;
import com.jgoodies.plaf.plastic.Plastic3DLookAndFeel;



//------------------------------------------------------------------------------
public class cytoscape implements WindowListener {
  protected Vector windows = new Vector ();
  protected CytoscapeVersion version = new CytoscapeVersion();
  protected Logger logger;
  protected SplashScreen splashScreen;
//------------------------------------------------------------------------------
public cytoscape (String [] args) throws Exception {
    splashScreen = new SplashScreen();
    //parse args and config files into config object
    CytoscapeConfig config = new CytoscapeConfig(args);
    splashScreen.advance(10);
    
    //handle special cases of arguments
    if (config.helpRequested()) {
        System.out.println(version);
        System.out.println(config.getUsage());
        exit(0);
    }    
    else if (config.inputsError()) {
        System.out.println(version);
        System.out.println("------------- Inputs Error");
        System.out.println(config.getUsage ());
        System.out.println(config);
        exit(1);
    }
    else if (config.displayVersion()) {
        System.out.println (version);
        exit(0);
    }

    //set up the logger
    setupLogger(config);
    logger.info(config.toString());
    if (splashScreen != null) {splashScreen.advance(20);}
    
    //try to create a bioDataServer
    String bioDataDirectory = config.getBioDataDirectory();
    BioDataServer bioDataServer = null;
    if (bioDataDirectory != null) {
        try {
            bioDataServer = new BioDataServer(bioDataDirectory);
        } catch (Exception e) {
            logger.severe("Unable to load bioDataServer from '" + bioDataDirectory + "'");
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
    }

    //create the global CytoscapeObj object
    CytoscapeObj cytoscapeObj = new CytoscapeObj(this, config, logger, bioDataServer);
    //get some standard fields for doing name resolution
    boolean canonicalize = Semantics.getCanonicalize(cytoscapeObj);
    String defaultSpecies = Semantics.getDefaultSpecies(null, cytoscapeObj);
    if (splashScreen != null) {splashScreen.advance(25);}
    
    //window title
    String title = null;
    
    //load a network if requested
    CyNetwork network = null;
    String geometryFilename = config.getGeometryFilename();
    String interactionsFilename = config.getInteractionsFilename();
    if (geometryFilename != null && interactionsFilename != null) {
        StringBuffer sb = new StringBuffer("Config specifies both interactions file '");
        sb.append(interactionsFilename + "' and GML file '" + geometryFilename + "'");
        sb.append("; using GML file");
        logger.severe(sb.toString());
    }
    if (geometryFilename != null) {
	logger.info("reading " + geometryFilename + "...");
	network = CyNetworkFactory.createNetworkFromGMLFile(geometryFilename, config.isYFiles());
	logger.info("  done");
	title = geometryFilename;
    } 
    else if (interactionsFilename != null) {
        logger.info("reading " + interactionsFilename + "...");
        network =
            CyNetworkFactory.createNetworkFromInteractionsFile( interactionsFilename,
                                                                canonicalize,
                                                                bioDataServer,
                                                                defaultSpecies,
                                                                config.isYFiles() );
	logger.info("  done");
        title = interactionsFilename;
    }
    if (network == null) {//none specified, or failed to read
        logger.info("no graph read, creating empty network");
        network = CyNetworkFactory.createEmptyNetwork(config.isYFiles());
        splashScreen.noGraph = true;
        title = "(Untitled)";
    }
    //add the semantics we usually expect
    Semantics.applyNamingServices(network, cytoscapeObj);
    
    //load any specified data attribute files
    logger.info("reading attribute files");
    CyNetworkFactory.loadAttributes(network,
                                    config.getNodeAttributeFilenames(),
                                    config.getEdgeAttributeFilenames(),
                                    canonicalize, bioDataServer, defaultSpecies);
    logger.info(" done");
    
    //load expression data if specified
    String expDataFilename = config.getExpressionFilename();
    if (expDataFilename != null) {
        logger.info("reading " + expDataFilename + "...");
        try {
            ExpressionData expData = new ExpressionData(expDataFilename);
            network.setExpressionData(expData);
            if (config.getWhetherToCopyExpToAttribs()) {
                expData.copyToAttribs(network.getNodeAttributes());
            }
        } catch (Exception e) {
            logger.severe("Exception reading expression data file '" + expDataFilename + "'");
            logger.severe(e.getMessage());
            e.printStackTrace();
        }
        logger.info("  done");
    }    
    if (splashScreen!=null) {splashScreen.advance(90);}
    
    //create the window
    CyWindow cyWindow = new CyWindow(cytoscapeObj, network, title);
    cyWindow.showWindow();
    
    if (splashScreen!=null) {splashScreen.advance(100);}
} // ctor
//------------------------------------------------------------------------------
/**
 * configure logging:  cytoscape.props specifies what level of logging
 * messages are written to the console; by default, only SEVERE messages
 * are written.  in time, more control of logging (i.e., optional logging
 * to a file, disabling console logging, per-window or per-plugin logging) 
 * can be provided
 */
protected void setupLogger (CytoscapeConfig config) {
    logger = Logger.getLogger("global"); 
    Properties properties = config.getProperties();
    String level = properties.getProperty("logging", "SEVERE");
    
    if (level.equalsIgnoreCase("severe")) {
        logger.setLevel(Level.SEVERE);
    } else if (level.equalsIgnoreCase("warning")) {
        logger.setLevel(Level.WARNING);
    } else if (level.equalsIgnoreCase("info")) {
        logger.setLevel(Level.INFO);
    } else if (level.equalsIgnoreCase("config")) {
        logger.setLevel(Level.CONFIG);
    } else if (level.equalsIgnoreCase("all")) {
        logger.setLevel(Level.ALL);
    } else if (level.equalsIgnoreCase("none")) {
        logger.setLevel(Level.OFF);
    } else if (level.equalsIgnoreCase("off")) {
        logger.setLevel(Level.OFF);
    }
}
//------------------------------------------------------------------------------
public void windowActivated   (WindowEvent e) {
    if(splashScreen != null) {
        splashScreen.advance(200);
        splashScreen.dispose();
        splashScreen = null;
    }
}
//------------------------------------------------------------------------------
/**
 * on linux (at least) a killed window generates a 'windowClosed' event; trap that here
 */
public void windowClosing     (WindowEvent e) {windowClosed (e);}
public void windowDeactivated (WindowEvent e) {}
public void windowDeiconified (WindowEvent e) {}
public void windowIconified   (WindowEvent e) {}
//------------------------------------------------------------------------------
public void windowOpened      (WindowEvent e) {  
    if(splashScreen != null) {
        splashScreen.advance(200);
        splashScreen.dispose();
        splashScreen = null;
    }
    windows.add (e.getWindow ());
}
//------------------------------------------------------------------------------
public void windowClosed     (WindowEvent e) { 
    Window window = e.getWindow();
    if (windows.contains(window)) {windows.remove (window);}
    
    if (windows.size () == 0) {
        logger.info("all windows closed, exiting...");
        exit(0);
    }
}	
//------------------------------------------------------------------------------
public void exit(int exitCode) {
    for (int i=0; i < windows.size (); i++) {
        Window w = (Window) windows.elementAt(i);
        w.dispose();
    }
    System.exit(exitCode);
}
//------------------------------------------------------------------------------
public static void main(String args []) throws Exception {
 
  UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
  Options.setGlobalFontSizeHints(FontSizeHints.MIXED);
  Options.setDefaultIconSize(new Dimension(18, 18));
 
  try {
    if ( LookUtils.isWindowsXP() ) {
      // use XP L&F
      UIManager.setLookAndFeel( Options.getCrossPlatformLookAndFeelClassName() );
    } else if ( System.getProperty("os.name").startsWith( "Mac" ) ) {
      // do nothing, I like the OS X L&F
    } else {
      // this is for for *nix
      // I happen to like this color combo, there are others
      Plastic3DLookAndFeel laf = new Plastic3DLookAndFeel();
      laf.setMyCurrentTheme( new com.jgoodies.plaf.plastic.theme.SkyBluerTahoma() );
      UIManager.setLookAndFeel( laf );
    }
  } catch (Exception e) {
    System.err.println("Can't set look & feel:" + e);
  }
  


    cytoscape app = new cytoscape(args);
} // main
//------------------------------------------------------------------------------
} // cytoscape


