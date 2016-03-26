package ika.app;

import ika.gui.ProgressIndicator;

/**
 * A ProgressIndicator implementation which sends messages to the standard
 * output.
 *
 * @author Bernhard Jenny, School of Mathematical and Geospatial Sciences, RMIT
 * University, Melbourne
 */
public class CmdLineProgress implements ProgressIndicator {

    private boolean aborted = false;

    @Override
    public void start() {
    }

    @Override
    public void abort() {
        this.aborted = true;
    }

    @Override
    public void complete() {
        System.out.println("\r100%");
    }

    @Override
    public boolean progress(int percentage) {
        System.out.print(percentage + "%\r");
        return true;
    }

    @Override
    public boolean isAborted() {
        return this.aborted;
    }

    @Override
    public void disableCancel() {
    }

    @Override
    public void enableCancel() {
    }

    @Override
    public void setMessage(String msg) {
        System.out.println(msg);
    }

    @Override
    public void setTotalTasksCount(int tasksCount) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getTotalTasksCount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void nextTask() {
    }

    @Override
    public int currentTask() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
