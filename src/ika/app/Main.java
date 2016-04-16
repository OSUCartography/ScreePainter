package ika.app;

import ika.gui.MacWindowsManager;
import ika.gui.MainWindow;
import ika.utils.IconUtils;
import ika.utils.PropertiesLoader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main entry point for Scree Painter
 *
 * @author Bernhard Jenny
 */
public class Main {

    /**
     * Start a second process that maximizes heap space. This process will be
     * blocked until the second process finishes.
     *
     * @param args command line arguments for second process.
     */
    private static void startProcess(String[] args, String className) {
        try {
            String xDockAppName = ApplicationInfo.getApplicationName();
            ProcessLauncher processLauncher = new ProcessLauncher();
            String xDockIconPath = processLauncher.findXDockIconPath("icon.icns");
            processLauncher.startJVM(className, xDockAppName, xDockIconPath, args);
            System.exit(0);
        } catch (Throwable ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }

    /**
     * Start the graphical user interface.
     */
    public static void startGUI() {
        // set icon for JOptionPane dialogs
        Properties props = PropertiesLoader.loadProperties("ika.app.Application");
        IconUtils.setOptionPaneIcons(props.getProperty("ApplicationIcon"));

        // on Mac OS X: take the menu bar out of the window and put it on top
        // of the main screen.
        if (ika.utils.Sys.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Scree Painter");
        }

        // Replace title of progress monitor dialog by empty string.
        UIManager.put("ProgressMonitor.progressText", "");

        SwingUtilities.invokeLater(new Runnable() {
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
            }
        });
    }

    /**
     * main routine for the application.
     *
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        // if arguments are received, start batch process
        if (args.length > 0) {
            startProcess(args, ScreePainterBatch.class.getName());
        } else {
            // no arguments received, so run in GUI mode
            startGUI();
        }
    }
}
