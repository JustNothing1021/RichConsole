package com.justnothing.richconsole.noneprompt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.segment.Segment;

/**
 * Interactive text input prompt.
 * Ported from noneprompt/InputPrompt.
 *
 * <p>Visual style guide (matching Python noneprompt):</p>
 * <pre>
 * [?] Enter your name? input_text
 * </pre>
 */
public class InputPrompt extends BasePrompt<String> {

    private final String question;
    private final String defaultText;
    private final boolean password;
    private final String questionMark;
    private final Predicate<String> validator;
    private final String errorMessage;

    // Mutable state
    private boolean answered;
    private String result;
    private StringBuilder inputText;
    private boolean invalid;

    public InputPrompt(String question, Console console) {
        this(question, console, null, false, null, null, null);
    }

    public InputPrompt(String question, Console console, String defaultText, boolean password,
                       String questionMark, Predicate<String> validator, String errorMessage) {
        this(question, console, defaultText, password, questionMark, validator, errorMessage, null);
    }

    public InputPrompt(String question, Console console, String defaultText, boolean password,
                       String questionMark, Predicate<String> validator, String errorMessage, PromptStyle promptStyle) {
        super(console, promptStyle);
        this.question = question;
        this.defaultText = defaultText;
        this.password = password;
        this.questionMark = questionMark != null ? questionMark : "[?]";
        this.validator = validator;
        this.errorMessage = errorMessage != null ? errorMessage : "Invalid input";
    }

    @Override
    protected void reset() {
        this.answered = false;
        this.result = null;
        this.inputText = new StringBuilder();
        this.invalid = false;
        if (defaultText != null) {
            inputText.append(defaultText);
        }
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
            promptLine.add(new Segment(password ? "********" : result, answerStyle()));
        } else {
            String display = password ? "*".repeat(inputText.length()) : inputText.toString();
            promptLine.add(new Segment(display));
        }
        lines.add(promptLine);

        // Error line
        if (invalid) {
            List<Segment> errorLine = new ArrayList<>();
            errorLine.add(new Segment(errorMessage, errorStyle()));
            lines.add(errorLine);
        }

        return lines;
    }

    @Override
    protected boolean handleKey(String op) {
        invalid = false;

        if (op.equals(OP_ENTER)) {
            String input = inputText.toString();
            if (validator != null && !validator.test(input)) {
                invalid = true;
                return false;
            }
            result = input;
            answered = true;
            return true;
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
    protected String getResult() {
        return result;
    }

    // =========================================================================
    // Convenience API
    // =========================================================================

    /**
     * Ask the user for text input.
     *
     * @param question the question to ask
     * @param console  the console to use
     * @return the user's input
     * @throws CancelledException if the user cancels
     */
    public static String ask(String question, Console console) throws CancelledException {
        return new InputPrompt(question, console).prompt();
    }

    /**
     * Ask the user for text input with a default value.
     */
    public static String ask(String question, Console console, String defaultText)
            throws CancelledException {
        return new InputPrompt(question, console, defaultText, false, null, null, null).prompt();
    }

    /**
     * Ask the user for password input (masked with *).
     */
    public static String askPassword(String question, Console console) throws CancelledException {
        return new InputPrompt(question, console, null, true, null, null, null).prompt();
    }

    /**
     * Ask the user for text input with validation.
     */
    public static String ask(String question, Console console, Predicate<String> validator,
                             String errorMessage) throws CancelledException {
        return new InputPrompt(question, console, null, false, null, validator, errorMessage).prompt();
    }
}
