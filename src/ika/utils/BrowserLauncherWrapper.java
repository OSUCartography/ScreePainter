package ika.utils;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Launches a web browser with a URL and takes care of exception handling.
 */
public class BrowserLauncherWrapper {

    private static final String CLASS = "edu.stanford.ejalbert.BrowserLauncher";
    private static final String METHOD = "openURLinBrowser";

    /*
     public static void main(String[] args) {
     BrowserLauncherWrapper.openURL("http://www.apple.com");
     }
     */
    /**
     * Creates a new instance of BrowserLauncherWrapper
     */
    private BrowserLauncherWrapper() {
    }

    /**
     * Opens a url in a web browser in a separate thread. Displays an error
     * dialog if the page cannot be opened.
     *
     * @param url
     */
    public static void openURL(String url) {
        String errorMsg = "Could not load the web page or not find a web browser. \n"
                        + "Please make sure your computer is connected to the Internet.";
        try {
            java.awt.Desktop.getDesktop().browse(new URI(url));
        } catch (Exception ex) {
            try {
                // use reflection to execute this code:
                // new BrowserLauncher().openURLinBrowser(url);
                // With reflection the BrowserLauncher2 jar does not have to
                // be linked against.
                Class cls = Class.forName(CLASS);
                Object browserLauncher = cls.getConstructor().newInstance();
                Method method = cls.getMethod(METHOD, new Class[]{String.class});
                method.invoke(browserLauncher, new Object[]{url});
            } catch (Exception e) {
                ErrorDialog.showErrorDialog(errorMsg, e);
            }
        }
    }
}
