package com.justnothing.richconsole.noneprompt;

import java.util.ArrayList;
import java.util.List;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.segment.Segment;

/**
 * Interactive single-select list prompt.
 * Ported from noneprompt/ListPrompt.
 *
 * <p>Displays a list of choices with a pointer (❯) that can be navigated
 * with arrow keys. Supports filtering by typing, mouse scrolling,
 * and auto-scrolling for long lists.</p>
 *
 * <p>Visual style guide (matching Python noneprompt):</p>
 * <pre>
 * [?] Choose a choice? (Use ↑ and ↓ to choose, Enter to submit)
 *  ❯  choice selected
 *     choice unselected
 * </pre>
 *
 * @param <T> the type of data associated with each choice
 */
public class ListPrompt<T> extends BasePrompt<Choice<T>> {

    private final String question;
    private final List<Choice<T>> choices;
    private final boolean allowFilter;
    private final String questionMark;
    private final String pointer;
    private final String annotation;
    private final int maxHeight;

    // Mutable state
    private int index;
    private int displayIndex;
    private boolean answered;
    private Choice<T> answer;
    private StringBuilder filterText;

    public ListPrompt(String question, List<Choice<T>> choices, Console console) {
        this(question, choices, console, true, null, null, null, null, null);
    }

    public ListPrompt(String question, List<Choice<T>> choices, Console console,
                      boolean allowFilter, Integer defaultSelect,
                      String questionMark, String pointer, String annotation,
                      Integer maxHeight) {
        this(question, choices, console, allowFilter, defaultSelect, questionMark, pointer, annotation, maxHeight, null);
    }

    public ListPrompt(String question, List<Choice<T>> choices, Console console,
                      boolean allowFilter, Integer defaultSelect,
                      String questionMark, String pointer, String annotation,
                      Integer maxHeight, PromptStyle promptStyle) {
        super(console, promptStyle);
        this.question = question;
        this.choices = choices;
        this.allowFilter = allowFilter;
        this.questionMark = questionMark != null ? questionMark : "[?]";
        this.pointer = pointer != null ? pointer : "❯";
        this.annotation = annotation != null ? annotation :
                "(Use ↑ and ↓ to choose, Enter to submit)";
        this.maxHeight = maxHeight != null && maxHeight > 0 ? maxHeight : getTerminalHeight();
        this.index = defaultSelect != null ? defaultSelect % choices.size() : 0;
        this.displayIndex = 0;
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

    // =========================================================================
    // BasePrompt implementation
    // =========================================================================

    @Override
    protected void reset() {
        this.index = 0;
        this.displayIndex = 0;
        this.answered = false;
        this.answer = null;
        this.filterText = new StringBuilder();
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
        if (answered && answer != null) {
            promptLine.add(new Segment(answer.name().trim(), answerStyle()));
        } else {
            promptLine.add(new Segment(annotation, annotationStyle()));
            if (allowFilter && filterText.length() > 0) {
                promptLine.add(new Segment(" "));
                promptLine.add(new Segment(filterText.toString()));
            }
        }
        lines.add(promptLine);

        // Choices (only show if not answered)
        if (!answered) {
            List<Choice<T>> filtered = getFilteredChoices();
            int maxShow = maxHeight - 1;
            int showStart = displayIndex;
            int showEnd = Math.min(showStart + maxShow, filtered.size());

            for (int i = showStart; i < showEnd; i++) {
                List<Segment> choiceLine = new ArrayList<>();
                Choice<T> choice = filtered.get(i);

                if (i == index) {
                    choiceLine.add(new Segment(pointer, pointerStyle()));
                    choiceLine.add(new Segment(" "));
                    choiceLine.add(new Segment(choice.name().trim(), selectedStyle()));
                } else {
                    choiceLine.add(new Segment(" ".repeat(pointer.length())));
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
        List<Choice<T>> filtered = getFilteredChoices();

        if (op.equals(OP_UP)) {
            if (!filtered.isEmpty()) {
                index = (index - 1 + filtered.size()) % filtered.size();
                adjustDisplayIndex(filtered.size());
            }
            return false;
        }

        if (op.equals(OP_DOWN)) {
            if (!filtered.isEmpty()) {
                index = (index + 1) % filtered.size();
                adjustDisplayIndex(filtered.size());
            }
            return false;
        }

        if (op.equals(OP_ENTER)) {
            if (filtered.isEmpty()) {
                return false;
            }
            answer = filtered.get(index);
            answered = true;
            return true;
        }

        // Typing for filter
        if (allowFilter && !answered) {
            if (op.equals(OP_BACKSPACE) || op.equals(OP_DELETE)) {
                if (filterText.length() > 0) {
                    filterText.deleteCharAt(filterText.length() - 1);
                    resetSelection(filtered.size());
                }
                return false;
            }

            // Printable character (single-char operation name = the character itself)
            if (op.length() == 1) {
                filterText.append(op);
                resetSelection(getFilteredChoices().size());
                return false;
            }
        }

        return false;
    }

    @Override
    protected Choice<T> getResult() {
        return answer;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private List<Choice<T>> getFilteredChoices() {
        if (!allowFilter || filterText.length() == 0) {
            return choices;
        }
        String text = filterText.toString().toLowerCase();
        List<Choice<T>> result = new ArrayList<>();
        for (Choice<T> choice : choices) {
            if (choice.name().toLowerCase().contains(text)) {
                result.add(choice);
            }
        }
        return result;
    }

    private void resetSelection(int filteredSize) {
        if (index >= filteredSize) {
            index = 0;
        }
        displayIndex = 0;
    }

    private void adjustDisplayIndex(int filteredSize) {
        int maxShow = maxHeight - 1;
        if (filteredSize <= maxShow) {
            displayIndex = 0;
            return;
        }

        // Scroll up
        if (index < displayIndex) {
            displayIndex = index;
        }
        // Scroll down
        int endIndex = displayIndex + maxShow - 1;
        if (index > endIndex) {
            displayIndex = index - maxShow + 1;
        }
        // Clamp
        int maxDisplayStart = filteredSize - maxShow;
        if (displayIndex > maxDisplayStart) {
            displayIndex = Math.max(0, maxDisplayStart);
        }
    }

    // =========================================================================
    // Convenience API
    // =========================================================================

    /**
     * Ask the user to select one choice from a list.
     *
     * @param question the question to ask
     * @param choices  the available choices
     * @param console  the console to use
     * @return the selected choice
     * @throws CancelledException if the user cancels
     */
    public static <T> Choice<T> ask(String question, List<Choice<T>> choices, Console console)
            throws CancelledException {
        return new ListPrompt<>(question, choices, console).prompt();
    }

    /**
     * Ask the user to select one choice from a list of strings.
     */
    public static Choice<String> askFromString(String question, List<String> choices, Console console)
            throws CancelledException {
        List<Choice<String>> choiceList = new ArrayList<>();
        for (String s : choices) {
            choiceList.add(new Choice<>(s, s));
        }
        return new ListPrompt<>(question, choiceList, console).prompt();
    }
}
