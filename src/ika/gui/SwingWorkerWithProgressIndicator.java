package ika.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author Bernhard Jenny, Institute of Cartography, ETH Zurich.
 */
public abstract class SwingWorkerWithProgressIndicator<T, V> extends SwingWorker<T, V>
        implements ProgressIndicator, PropertyChangeListener {

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
     * The number of tasks to excecute. The default is 1.
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

        this.addPropertyChangeListener(this);
        initDialog(owner, dialogTitle, message, blockOwner);
    }

    private void initDialog(Frame owner,
            String dialogTitle,
            String message,
            boolean blockOwner) {

        if (!SwingUtilities.isEventDispatchThread()) {
            System.err.println("SwingProgressIndicator must be created in Swing thread.");
        }
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

    public void start() {
        synchronized (this) {
            this.aborted = false;
            this.startTime = System.currentTimeMillis();

            // start a timer task that will show the dialog a little later
            // in case the client never calls progress().
            new ShowDialogTask(this).start();
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressPanel.start();
            }
        });
    }

    public void abort() {
        synchronized (this) {
            // the client has to regularly check the aborted flag.
            this.aborted = true;
        }

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                dialog.setVisible(false);
            }
        });
    }

    public void complete() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                dialog.setVisible(false);
                progressPanel.removeActionListeners();
            }
        });
    }

    public boolean progress(int percentage) {
        this.setProgress(percentage);
        return !this.isAborted();
    }

    public boolean isAborted() {
        synchronized (this) {
            return this.aborted;
        }
    }

    public void disableCancel() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressPanel.disableCancel();
            }
        });

    }

    public void enableCancel() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressPanel.enableCancel();
            }
        });
    }

    public void setMessage(final String msg) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressPanel.setMessage(msg);
            }
        });
    }

    /**
     * Invoked when the task's progress property changes. Update the value
     * displayed by the progress dialog and inform the task if the user presses
     * the cancel button. This is called in the event dispatching thread.
     */
    public void propertyChange(PropertyChangeEvent evt) {

        if ("progress".equals(evt.getPropertyName())) {
            int progress = ((Integer) evt.getNewValue()).intValue();
            progress = progress / totalTasksCount + (currentTask - 1) * 100 / totalTasksCount;

            // make the dialog visible if necessary
            synchronized (this) {
                if (aborted == false) {
                    // make the dialog visible if necessary
                    if (dialog.isVisible()) {
                        this.progressPanel.updateProgressGUI(progress);
                    } else {
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
            }
        }

        if ("state".equals(evt.getPropertyName())) {

        }
    }

    /**
     * ShowDialogTask is a timer that makes sure the progress dialog is shown if
     * progress() is not called for a long time. In this case, the dialog would
     * never become visible.
     */
    private class ShowDialogTask implements ActionListener {

        /**
         * A SwingProgressIndicator to access synchronized status variables.
         */
        final private SwingWorker swingWorker;

        public ShowDialogTask(SwingWorker swingWorker) {
            this.swingWorker = swingWorker;
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
        public void actionPerformed(ActionEvent e) {

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {

                    // only make the dialog visible if the operation is not
                    // yet finished
                    if (swingWorker.isDone() || dialog.isVisible()) {
                        return;
                    }

                    // don't know how long the operation will take.
                    progressPanel.setIndeterminate(true);

                    // show the dialog
                    dialog.pack();
                    dialog.setVisible(true);
                }
            });
        }
    }

    public int getMaxTimeWithoutDialog() {
        return maxTimeWithoutDialog;
    }

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
    public int getTotalTasksCount() {
        synchronized (this) {
            return this.totalTasksCount;
        }
    }

    /**
     * Switch to the next task.
     */
    public void nextTask() {
        synchronized (this) {
            ++this.currentTask;
            if (this.currentTask > this.totalTasksCount) {
                this.totalTasksCount = this.currentTask;
            }

            // set progress to 0 for the new task, otherwise the progress bar would
            // jump to the end of this new task and jump back when setProgress(0)
            // is called
            this.setProgress(0);
        }
    }

    /**
     * Returns the ID of the current task. The first task has ID 1 (and not 0).
     *
     * @return The ID of the current task.
     */
    public int currentTask() {
        synchronized (this) {
            return this.currentTask;
        }
    }

    public boolean isIndeterminate() {
        synchronized (this) {
            return this.indeterminate;
        }
    }

    public void setIndeterminate(boolean indet) {
        synchronized (this) {
            this.indeterminate = indet;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                progressPanel.setIndeterminate(indeterminate);
            }
        });
    }
}
