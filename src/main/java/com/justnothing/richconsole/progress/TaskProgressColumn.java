package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Show task progress as a percentage.
 * Ported from rich/progress.py TaskProgressColumn.
 */
public class TaskProgressColumn extends TextColumn {

    private final String textFormatNoPercentage;
    private final boolean showSpeed;

    public TaskProgressColumn() {
        this("[progress.percentage]{task.percentage:>3.0f}%", "  --%", "none", "left", true, false);
    }

    /**
     * @param textFormat               format for the percentage (with a percentage value)
     * @param textFormatNoPercentage   format when the total is unknown
     * @param style                    style of the output
     * @param justify                  text justification
     * @param markup                   enable markup
     * @param showSpeed                show speed (it/s) when the total is unknown
     */
    public TaskProgressColumn(String textFormat, String textFormatNoPercentage, Object style,
                              String justify, boolean markup, boolean showSpeed) {
        super(textFormat, style, justify, markup);
        this.textFormatNoPercentage = textFormatNoPercentage;
        this.showSpeed = showSpeed;
    }

    @Override
    public Object render(Progress.Task task) {
        if (task.getTotal() == null && showSpeed) {
            Double speed = task.getFinishedSpeed() != null ? task.getFinishedSpeed() : task.getSpeed();
            return renderSpeed(speed);
        }
        String textFormat = task.getTotal() == null ? textFormatNoPercentage : getTextFormat();
        return renderFormatted(TextColumn.formatTemplate(textFormat, task), task);
    }

    private static Text renderSpeed(Double speed) {
        if (speed == null) {
            return new Text("", "progress.percentage");
        }
        return new Text(ProgressFormats.formatIterationSpeed(speed), "progress.percentage");
    }
}
