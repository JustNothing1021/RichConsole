package com.justnothing.richconsole.progress;

import com.justnothing.richconsole.text.Text;

/**
 * Renders a human readable transfer speed.
 * Ported from rich/progress.py TransferSpeedColumn.
 */
public class TransferSpeedColumn extends ProgressColumn {

    @Override
    public Object render(Progress.Task task) {
        Double speed = task.getFinishedSpeed() != null ? task.getFinishedSpeed() : task.getSpeed();
        if (speed == null) {
            return new Text("?", "progress.data.speed");
        }
        return new Text(ProgressFormats.formatSpeed(speed), "progress.data.speed");
    }
}
