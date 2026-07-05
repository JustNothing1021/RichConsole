package com.justnothing.richconsole.noneprompt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.style.Style;

/**
 * Interactive multi-select checkbox prompt.
 * Ported from noneprompt/CheckboxPrompt.
 *
 * <p>Displays a list of choices with checkboxes (●/○) and a pointer (❯).
 * Use arrow keys to navigate, Space to toggle selection, Enter to submit.</p>
 *
 * <p>Visual style guide (matching Python noneprompt):</p>
 * <pre>
 * [?] Choose items? (Use ↑ and ↓ to move, Space to select, Enter to submit)
 *  ❯  ●  choice selected
 *     ○  choice unselected
 * </pre>
 *
 * @param <T> the type of data associated with each choice
 */
public class CheckboxPrompt<T> extends BasePrompt<List<Choice<T>>> {

    private final String question;
    private final List<Choice<T>> choices;
    private final Set<Integer> defaultSelect;
    private final String questionMark;
    private final String pointer;
    private final String selectedSign;
    private final String unselectedSign;
    private final String annotation;
    private final int maxHeight;

    // Mutable state
    private int index;
    private int displayIndex;
    private Set<Integer> selected;
    private boolean answered;

    public CheckboxPrompt(String question, List<Choice<T>> choices, Console console) {
        this(question, choices, console, null, null, null, null, null, null, null);
    }

    public CheckboxPrompt(String question, List<Choice<T>> choices, Console console,
                          List<Integer> defaultSelect, String questionMark, String pointer,
                          String selectedSign, String unselectedSign, String annotation,
                          Integer maxHeight) {
        this(question, choices, console, defaultSelect, questionMark, pointer, selectedSign, unselectedSign, annotation, maxHeight, null);
    }

    public CheckboxPrompt(String question, List<Choice<T>> choices, Console console,
                          List<Integer> defaultSelect, String questionMark, String pointer,
                          String selectedSign, String unselectedSign, String annotation,
                          Integer maxHeight, PromptStyle promptStyle) {
        super(console, promptStyle);
        this.question = question;
        this.choices = choices;
        this.defaultSelect = defaultSelect != null
                ? new HashSet<>(defaultSelect.stream().map(i -> i % choices.size()).collect(java.util.stream.Collectors.toList()))
                : new HashSet<>();
        this.questionMark = questionMark != null ? questionMark : "[?]";
        this.pointer = pointer != null ? pointer : "❯";
        this.selectedSign = selectedSign != null ? selectedSign : "●";
        this.unselectedSign = unselectedSign != null ? unselectedSign : "○";
        this.annotation = annotation != null ? annotation :
                "(Use ↑ and ↓ to move, Space to select, Enter to submit)";
        this.maxHeight = maxHeight != null && maxHeight > 0 ? maxHeight : getTerminalHeight();
    }

    private int getTerminalHeight() {
        if (terminal != null) {
            org.jline.terminal.Size size = terminal.getSize();
            if (size != null && size.getRows() > 0) {
                return size.getRows();
            }
        }
        return 25;
    }

    @Override
    protected void reset() {
        this.index = 0;
        this.displayIndex = 0;
        this.selected = new HashSet<>(defaultSelect);
        this.answered = false;
    }

    @Override
    protected List<List<Segment>> render() {
        List<List<Segment>> lines = new ArrayList<>();

        // Line 1: [?] Question (annotation or answer)
        List<Segment> promptLine = new ArrayList<>();
        if (!questionMark.isEmpty()) {
            promptLine.add(new Segment(questionMark, questionMarkStyle()));
            promptLine.add(new Segment(" "));
        }
        promptLine.add(new Segment(question.trim(), questionStyle()));
        promptLine.add(new Segment(" "));
        if (answered) {
            List<Choice<T>> result = getResult();
            String answerText = String.join(", ",
                    result.stream().map(c -> c.name().trim()).collect(java.util.stream.Collectors.toList()));
            promptLine.add(new Segment(answerText, answerStyle()));
        } else {
            promptLine.add(new Segment(annotation, annotationStyle()));
        }
        lines.add(promptLine);

        // Choices (only show if not answered)
        if (!answered) {
            int maxShow = maxHeight - 1;
            int showStart = displayIndex;
            int showEnd = Math.min(showStart + maxShow, choices.size());

            for (int i = showStart; i < showEnd; i++) {
                List<Segment> choiceLine = new ArrayList<>();
                Choice<T> choice = choices.get(i);

                // Pointer
                if (i == index) {
                    choiceLine.add(new Segment(pointer, pointerStyle()));
                } else {
                    choiceLine.add(new Segment(" ".repeat(pointer.length())));
                }
                choiceLine.add(new Segment(" "));

                // Checkbox sign
                if (selected.contains(i)) {
                    choiceLine.add(new Segment(selectedSign, signStyle()));
                    choiceLine.add(new Segment(" "));
                    choiceLine.add(new Segment(choice.name().trim(), selectedStyle()));
                } else {
                    choiceLine.add(new Segment(unselectedSign));
                    choiceLine.add(new Segment(" "));
                    choiceLine.add(new Segment(choice.name().trim()));
                }
                lines.add(choiceLine);
            }
        }

        return lines;
    }

    @Override
    protected boolean handleKey(String op) {
        if (op.equals(OP_UP)) {
            index = (index - 1 + choices.size()) % choices.size();
            adjustDisplayIndex();
            return false;
        }

        if (op.equals(OP_DOWN)) {
            index = (index + 1) % choices.size();
            adjustDisplayIndex();
            return false;
        }

        if (op.equals(OP_SPACE)) {
            if (selected.contains(index)) {
                selected.remove(index);
            } else {
                selected.add(index);
            }
            return false;
        }

        if (op.equals(OP_ENTER)) {
            answered = true;
            return true;
        }

        return false;
    }

    @Override
    protected List<Choice<T>> getResult() {
        List<Choice<T>> result = new ArrayList<>();
        for (int i : selected.stream().sorted().collect(java.util.stream.Collectors.toList())) {
            if (i < choices.size()) {
                result.add(choices.get(i));
            }
        }
        return result;
    }

    private void adjustDisplayIndex() {
        int maxShow = maxHeight - 1;
        if (choices.size() <= maxShow) {
            displayIndex = 0;
            return;
        }
        if (index < displayIndex) {
            displayIndex = index;
        }
        int endIndex = displayIndex + maxShow - 1;
        if (index > endIndex) {
            displayIndex = index - maxShow + 1;
        }
        int maxDisplayStart = choices.size() - maxShow;
        if (displayIndex > maxDisplayStart) {
            displayIndex = Math.max(0, maxDisplayStart);
        }
    }

    /**
     * Style for the checkbox sign (●).
     */
    protected Style signStyle() {
        return promptStyle.checkboxSign;
    }

    // =========================================================================
    // Convenience API
    // =========================================================================

    /**
     * Ask the user to select multiple choices.
     */
    public static <T> List<Choice<T>> ask(String question, List<Choice<T>> choices, Console console)
            throws CancelledException {
        return new CheckboxPrompt<>(question, choices, console).prompt();
    }

    /**
     * Ask the user to select multiple choices from string list.
     */
    public static List<Choice<String>> askFromStrings(String question, List<String> choices, Console console)
            throws CancelledException {
        List<Choice<String>> choiceList = new ArrayList<>();
        for (String s : choices) {
            choiceList.add(new Choice<>(s, s));
        }
        return new CheckboxPrompt<>(question, choiceList, console).prompt();
    }
}
