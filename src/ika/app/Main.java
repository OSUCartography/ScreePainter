/*
 * Main.java
 *
 * Created on November 1, 2005, 10:19 AM
 *
 */

package ika.app;

import ika.gui.*;
import ika.utils.IconUtils;
import javax.swing.*;

/**
 * Main entry point.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class Main {
    
    /**
     * main routine for the application.
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        
        // on Mac OS X: take the menu bar out of the window and put it on top
        // of the main screen.
        if (ika.utils.Sys.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Scree Painter");
        }
        
        // set icon for JOptionPane dialogs
        java.util.Properties props 
                = ika.utils.PropertiesLoader.loadProperties("ika.app.Application");
        IconUtils.setOptionPaneIcons(props.getProperty("ApplicationIcon"));
        
        // Replace title of progress monitor dialog by empty string.
        UIManager.put("ProgressMonitor.progressText", "");
        
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
            
                // show std out in own window
                // new ika.utils.StdErrOutWindows(null, "Console Output", "Error Output");
                
                // create a temporary invisible BaseMainWindow, extract its
                // menu bar and pass it to the MacWindowsManager.
                if (ika.utils.Sys.isMacOSX()) {
                    MacWindowsManager.init(MainWindow.getMenuBarClone());
                }
                
                // create a new empty window
                MainWindow win = MainWindow.newDocumentWindow();

                // user canceled data selection dialog.
                if (win == null) {
                    System.exit(0);
                }
                /*
                // initialize output and error stream for display in a window
                String appName = ika.app.ApplicationInfo.getApplicationName();
                String outTitle = appName + " - Standard Output";
                String errTitle = appName + " - Error Messages";
                new ika.utils.StdErrOutWindows(null, outTitle, errTitle);
                 */
            }
        });
        
    }
}
