/*
 * MacWindowsManager.java
 *
 * Created on October 25, 2005, 12:09 PM
 *
 */

package ika.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuBar;

/**
 * Mac OS X specific code. Integrates a Java application to the standard Mac
 * look and feel.
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public class MacWindowsManager {
    
    /** This JMenuBar is displayed when no window is open.
     */
    private static JMenuBar macFramelessMenuBar = null;
    
    private MacWindowsManager(){
    }
    
    /**
     * Setup the menu bar for Mac OS X.
     */
    public static void init(JMenuBar menuBar) {
        if (!ika.utils.Sys.isMacOSX() || menuBar == null)
            return;
        
        MacWindowsManager.macFramelessMenuBar = menuBar;
    }
    
    /**
     * Update the menu bar that is displayed when no document window is open.
     */
    public static void updateFramelessMenuBar() {
        // Search for the Window menu in the menu bar that is displayed
        // when no window is open or when all windows are minimized.
        // Update its Window menu to list all windows. This is important when 
        // all windows are minimized. The other menus don't need to be updated,
        // they are disabled by default.
        if (macFramelessMenuBar == null)
            return;
        final int menuCount = macFramelessMenuBar.getMenuCount();
        for (int i = menuCount - 1; i >= 0; i--) {
            JMenu menu = macFramelessMenuBar.getMenu(i);
            if ("WindowsMenu".equals(menu.getName())) {
                MainWindow.updateWindowMenu(menu, null);
                break;
            }
        }
    }
    
}
