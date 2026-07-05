package com.justnothing.richconsole.noneprompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.segment.Segment;

/**
 * Interactive yes/no confirmation prompt.
 * Ported from noneprompt/ConfirmPrompt.
 *
 * <p>Visual style guide (matching Python noneprompt):</p>
 * <pre>
 * [?] Continue? (Y/n)
 * </pre>
 */
public class ConfirmPrompt extends BasePrompt<Boolean> {

    private static final Set<String> TRUE_STRINGS = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList("y", "yes", "true", "t", "1")));
    private static final Set<String> FALSE_STRINGS = java.util.Collections.unmodifiableSet(
            new java.util.HashSet<>(java.util.Arrays.asList("n", "no", "false", "f", "0")));

    private final String question;
    private final Boolean defaultChoice;
    private final String questionMark;

    // Mutable state
    private boolean answered;
    private Boolean result;
    private StringBuilder inputText;
    private boolean invalid;

    public ConfirmPrompt(String question, Console console) {
        this(question, console, null, null);
    }

    public ConfirmPrompt(String question, Console console, Boolean defaultChoice, String questionMark) {
        this(question, console, defaultChoice, questionMark, null);
    }

    public ConfirmPrompt(String question, Console console, Boolean defaultChoice, String questionMark, PromptStyle promptStyle) {
        super(console, promptStyle);
        this.question = question;
        this.defaultChoice = defaultChoice;
        this.questionMark = questionMark != null ? questionMark : "[?]";
    }

    @Override
    protected void reset() {
        this.answered = false;
        this.result = null;
        this.inputText = new StringBuilder();
        this.invalid = false;
    }

    @Override
    protected List<List<Segment>> render() {
        List<List<Segment>> lines = new ArrayList<>();
        List<Segment> promptLine = new ArrayList<>();

        if (!questionMark.isEmpty()) {
            promptLine.add(new Segment(questionMark, questionMarkStyle()));
            promptLine.add(new Segment(" "));
        }
        promptLine.add(new Segment(question.trim(), questionStyle()));
        promptLine.add(new Segment(" "));

        if (answered) {
            promptLine.add(new Segment(result ? "Yes" : "No", answerStyle()));
        } else {
            // Annotation (Y/n), (y/N), (y/n)
            String annotation;
            if (defaultChoice == null) {
                annotation = "(y/n)";
            } else if (defaultChoice) {
                annotation = "(Y/n)";
            } else {
                annotation = "(y/N)";
            }
            promptLine.add(new Segment(annotation, annotationStyle()));
            promptLine.add(new Segment(" "));
            promptLine.add(new Segment(inputText.toString()));
        }
        lines.add(promptLine);

        // Error line
        if (invalid) {
            List<Segment> errorLine = new ArrayList<>();
            errorLine.add(new Segment("Please enter Y or N", errorStyle()));
            lines.add(errorLine);
        }

        return lines;
    }

    @Override
    protected boolean handleKey(String op) {
        invalid = false;

        if (op.equals(OP_ENTER)) {
            String input = inputText.toString().trim().toLowerCase();
            if (input.isEmpty()) {
                if (defaultChoice != null) {
                    result = defaultChoice;
                    answered = true;
                    return true;
                }
                invalid = true;
                return false;
            }
            if (TRUE_STRINGS.contains(input)) {
                result = true;
                answered = true;
                return true;
            }
            if (FALSE_STRINGS.contains(input)) {
                result = false;
                answered = true;
                return true;
            }
            invalid = true;
            return false;
        }

        if (op.equals(OP_BACKSPACE) || op.equals(OP_DELETE)) {
            if (inputText.length() > 0) {
                inputText.deleteCharAt(inputText.length() - 1);
            }
            return false;
        }

        // Printable character (single-char operation name = the character itself)
        if (op.length() == 1) {
            inputText.append(op);
            return false;
        }

        return false;
    }

    @Override
    protected Boolean getResult() {
        return result;
    }

    // =========================================================================
    // Convenience API
    // =========================================================================

    /**
     * Ask a yes/no question.
     *
     * @param question    the question to ask
     * @param console     the console to use
     * @param defaultVal  the default value (true=yes, false=no, null=none)
     * @return true if confirmed, false if denied
     * @throws CancelledException if the user cancels
     */
    public static boolean ask(String question, Console console, Boolean defaultVal)
            throws CancelledException {
        return new ConfirmPrompt(question, console, defaultVal, null).prompt();
    }

    /**
     * Ask a yes/no question without a default.
     */
    public static boolean ask(String question, Console console) throws CancelledException {
        return new ConfirmPrompt(question, console, null, null).prompt();
    }
}
