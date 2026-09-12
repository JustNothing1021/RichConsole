package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.spinner.Spinner;
import com.justnothing.richconsole.text.Text;

/**
 * A column with a 'spinner' animation.
 * Ported from rich/progress.py SpinnerColumn.
 */
public class SpinnerColumn extends ProgressColumn {

    private Spinner spinner;
    private final Object finishedText;

    public SpinnerColumn() {
        this("dots", "progress.spinner", 1.0, " ");
    }

    public SpinnerColumn(String spinnerName, Object style, double speed, Object finishedText) {
        this.spinner = new Spinner(spinnerName, null, style, speed);
        this.finishedText = finishedText instanceof String str
                ? Text.fromMarkup(str)
                : finishedText;
    }

    /**
     * Swap in a new spinner animation.
     *
     * @param spinnerName  spinner name, see {@link Spinner#getSpinnerNames()}
     * @param spinnerStyle style for the spinner
     * @param speed        speed factor
     */
    public void setSpinner(String spinnerName, Object spinnerStyle, double speed) {
        this.spinner = new Spinner(spinnerName, null, spinnerStyle, speed);
    }

    @Override
    public Object render(Progress.Task task) {
        if (task.isFinished()) {
            return finishedText;
        }
        return spinner.render(task.getTime());
    }
}
