package ika.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 * @param <T>
 */
public abstract class SwingWorkerWithProgressIndicator<T> extends SwingWorker<T, Integer>
        implements ProgressIndicator {

    /**
     * The GUI. Must be accessed by the Swing thread only.
     */
    protected ProgressPanel progressPanel;
    /**
     * The dialog to display the progressPanel. Must be accessed by the Swing
     * thread only.
     */
    protected JDialog dialog;
    /**
     * The owner frame of the dialog
     */
    protected Frame owner;
    /**
     * aborted is true after abort() is called. Access to aborted must be
     * synchronized.
     */
    private boolean aborted = false;
    /**
     * flag to remember whether the duration of the task is indeterminate.
     */
    private boolean indeterminate;
    /**
     * The number of tasks to execute. The default is 1.
     */
    private int totalTasksCount = 1;
    /**
     * The ID of the current task.
     */
    private int currentTask = 1;
    /**
     * If an operation takes less time than maxTimeWithoutDialog, no dialog is
     * shown. Unit: milliseconds.
     */
    private int maxTimeWithoutDialog = 2000;
    /**
     * Time in milliseconds when the operation started.
     */
    private long startTime;

    /**
     * Must be called in the Event Dispatching Thread.
     *
     * @param owner
     * @param dialogTitle
     * @param message
     * @param blockOwner
     */
    public SwingWorkerWithProgressIndicator(Frame owner,
            String dialogTitle,
            String message,
            boolean blockOwner) {

        assert (SwingUtilities.isEventDispatchThread());

        this.owner = owner;

        // prepare the dialog
        dialog = new JDialog(owner);
        dialog.setTitle(dialogTitle);
        dialog.setModal(blockOwner);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        Action cancelAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abort();
            }
        };

        progressPanel = new ProgressPanel();
        progressPanel.setMessage(message);
        progressPanel.setCancelAction(cancelAction);

        dialog.setContentPane(progressPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setName(ProgressPanel.DIALOG_NAME);
        dialog.setAlwaysOnTop(true);
    }

    /**
     * Initialize the dialog. Can be called from any thread. If called from the
     * event dispatching thread, the dialog is initialized immediately.
     */
    @Override
    public void start() {

        synchronized (this) {
            this.aborted = false;
            this.startTime = System.currentTimeMillis();
        }

        // start a timer task that will show the dialog a little later
        // in case the client does not call progress() for a longer period.
        new ShowDialogTask().start();

        // initialize the GUI in the event dispatching thread
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                progressPanel.start();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Stop the operation. The dialog will be hidden and the operation will stop
     * as soon as possible. This might not happen synchronously. Can be called
     * from any thread. If called from the event dispatching thread, the window
     * is shown immediately.
     */
    @Override
    public void abort() {
        synchronized (this) {
            // the client has to regularly check the aborted flag.
            this.aborted = true;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(false);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Inform the dialog that the operation has completed and it can be hidden.
     * If called from the event dispatch thread, the dialog is hidden
     * immediately.
     */
    @Override
    public void complete() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                dialog.setVisible(false);
                progressPanel.removeActionListeners();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    /**
     * Update the progress indicator.
     *
     * @param percentage A value between 0 and 100.
     * @return True if the operation should continue, false otherwise.
     */
    @Override
    public boolean progress(int percentage) {
        this.setProgress(percentage);
        this.publish(percentage);
        return !this.isAborted();
    }

    /**
     * Returns whether this operation should stop.
     *
     * @return
     */
    @Override
    public boolean isAborted() {
        synchronized (this) {
            return this.aborted;
        }
    }

    @Override
    public void disableCancel() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                progressPanel.disableCancel();
            }
        });

    }

    @Override
    public void enableCancel() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                progressPanel.enableCancel();
            }
        });
    }

    @Override
    public void setMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                progressPanel.setMessage(msg);
            }
        });
    }

    /**
     * Invoked when the task's publish() method is called. Update the value
     * displayed by the progress dialog. This is called from within the event
     * dispatching thread.
     */
    @Override
    protected void process(List<Integer> progressList) {
        assert (SwingUtilities.isEventDispatchThread());

        if (isAborted()) {
            return;
        }

        int progress = progressList.get(progressList.size() - 1).intValue();
        progress = progress / totalTasksCount + (currentTask - 1) * 100 / totalTasksCount;

        if (dialog.isVisible()) {
            this.progressPanel.updateProgressGUI(progress);
        } else {
            // make the dialog visible if necessary
            // Don't show the dialog for short operations.
            // Only show it when half of maxTimeWithoutDialog has passed
            // and the operation has not yet completed half of its task.
            final long currentTime = System.currentTimeMillis();
            if (currentTime - startTime > maxTimeWithoutDialog / 2 && progress < 50) {
                // show the dialog
                dialog.pack();
                dialog.setVisible(true);
            }
        }
    }

    /**
     * ShowDialogTask is a timer that makes sure the progress dialog is shown if
     * progress() is not called for a long time. In this case, the dialog would
     * never become visible.
     */
    private class ShowDialogTask implements ActionListener {

        private ShowDialogTask() {
        }

        /**
         * Start a timer that will show the dialog in maxTimeWithoutDialog
         * milliseconds if the dialog is not visible until then.
         */
        private void start() {
            javax.swing.Timer timer = new javax.swing.Timer(getMaxTimeWithoutDialog(), this);
            timer.setRepeats(false);
            timer.start();
        }

        /**
         * Show the dialog if it is not visible yet and if the operation has not
         * yet finished.
         */
        @Override
        public void actionPerformed(ActionEvent e) {

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    // only make the dialog visible if the operation is not
                    // yet finished
                    if (isAborted() || dialog.isVisible()) {
                        return;
                    }

                    // show the dialog
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
    }

    /**
     * Returns the milliseconds after which the progress dialog is shown if the
     * worker does not update the progress information.
     *
     * @return
     */
    public int getMaxTimeWithoutDialog() {
        return maxTimeWithoutDialog;
    }

    /**
     * A dialog is shown after some time if the worker does not update the
     * progress information.
     *
     * @param maxTimeWithoutDialog show dialog after this many milliseconds.
     */
    public void setMaxTimeWithoutDialog(int maxTimeWithoutDialog) {
        this.maxTimeWithoutDialog = maxTimeWithoutDialog;
    }

    /**
     * Sets the number of tasks. Each task has a progress between 0 and 100. If
     * the number of tasks is larger than 1, progress of task 1 will be rescaled
     * to 0..50.
     *
     * @param tasksCount The total number of tasks.
     */
    @Override
    public void setTotalTasksCount(int tasksCount) {
        synchronized (this) {
            this.totalTasksCount = tasksCount;
        }
    }

    /**
     * Returns the total numbers of tasks for this progress indicator.
     *
     * @return The total numbers of tasks.
     */
    @Override
    public int getTotalTasksCount() {
        synchronized (this) {
            return this.totalTasksCount;
        }
    }

    /**
     * Switch to the next task.
     */
    @Override
    public void nextTask() {
        synchronized (this) {
            if (currentTask + 1 <= totalTasksCount) {
                ++currentTask;
            }

            // set progress to 0 for the new task, otherwise the progress bar would
            // jump to the end of this new task and jump back when setProgress(0)
            // is called
            this.setProgress(0);
        }
    }

    public void nextTask(String message) {
        nextTask();
        setMessage(message);
    }

    /**
     * Returns the ID of the current task. The first task has ID 1 (and not 0).
     *
     * @return The ID of the current task.
     */
    @Override
    public int currentTask() {
        synchronized (this) {
            return currentTask;
        }
    }

    public boolean isIndeterminate() {
        synchronized (this) {
            return indeterminate;
        }
    }

    public void setIndeterminate(boolean indeterminate) {
        synchronized (this) {
            this.indeterminate = indeterminate;
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                progressPanel.setIndeterminate(indeterminate);
            }
        });
    }
}
