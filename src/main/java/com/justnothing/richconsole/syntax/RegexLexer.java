package com.justnothing.richconsole.syntax;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A regex-based state machine lexer, inspired by Pygments' RegexLexer.
 * Subclasses define rules for each state, and the lexer processes input
 * by matching rules in order within the current state.
 */
public abstract class RegexLexer implements Lexer {

    // ---- Rule data classes ----

    protected static final String POP = "#pop";
    protected static final String ROOT = "root";

    /**
     * A single lexer rule: a regex pattern plus actions.
     */
    protected static class Rule {
        final Pattern pattern;
        final List<RuleAction> actions;

        Rule(Pattern pattern, List<RuleAction> actions) {
            this.pattern = pattern;
            this.actions = actions;
        }
    }

    /**
     * Base class for rule actions.
     */
    protected interface RuleAction {}

    /**
     * Emit a token with the given type.
     * The text comes from the matched group (group 0 for no group, or the specific group).
     */
    protected static class Emit implements RuleAction {
        final String type;
        final int group;

        Emit(String type, int group) {
            this.type = type;
            this.group = group;
        }

        Emit(String type) {
            this(type, 0);
        }
    }

    /**
     * Push a new state onto the state stack.
     */
    protected static class Push implements RuleAction {
        final String state;

        Push(String state) {
            this.state = state;
        }
    }

    /**
     * Pop the current state from the state stack.
     */
    protected static class Pop implements RuleAction {
        Pop() {}
    }

    // ---- Instance fields ----

    private Map<String, List<Rule>> rules;
    private boolean compiled = false;

    // ---- Abstract method ----

    /**
     * Subclasses define their rules by calling addRule() and addState() in this method.
     */
    protected abstract void defineRules();

    // ---- Rule definition helpers ----

    private final Map<String, List<Rule>> ruleBuilders = new LinkedHashMap<>();
    private String currentState = ROOT;

    protected void state(String name) {
        currentState = name;
        if (!ruleBuilders.containsKey(name)) {
            ruleBuilders.put(name, new ArrayList<>());
        }
    }

    protected void addRule(String regex, Object... actions) {
        List<RuleAction> actionList = new ArrayList<>();
        // Compile pattern first to count capturing groups
        Pattern pattern = Pattern.compile(regex);
        int groupCount = pattern.matcher("").groupCount();

        // If the regex has capturing groups, assign them in order to String actions.
        // Otherwise, all Emit actions use group 0 (entire match).
        int nextGroup = groupCount > 0 ? 1 : 0;

        for (Object action : actions) {
            if (action instanceof String) {
                String s = (String) action;
                if (POP.equals(s)) {
                    actionList.add(new Pop());
                } else if (s.startsWith("#push:")) {
                    actionList.add(new Push(s.substring(6)));
                } else {
                    // It's a token type
                    if (groupCount > 0 && nextGroup <= groupCount) {
                        actionList.add(new Emit(s, nextGroup));
                        nextGroup++;
                    } else {
                        actionList.add(new Emit(s, 0));
                    }
                }
            } else if (action instanceof RuleAction) {
                actionList.add((RuleAction) action);
            }
        }
        ruleBuilders.get(currentState).add(new Rule(pattern, actionList));
    }

    /**
     * Add a simple rule: one regex → one token type (no groups, no state change).
     */
    protected void rule(String regex, String tokenType) {
        ruleBuilders.get(currentState).add(new Rule(
                Pattern.compile(regex),
                java.util.Collections.singletonList(new Emit(tokenType))
        ));
    }

    /**
     * Add a rule with token type and next state.
     */
    protected void rule(String regex, String tokenType, String nextState) {
        List<RuleAction> actions = new ArrayList<>();
        actions.add(new Emit(tokenType));
        if (POP.equals(nextState)) {
            actions.add(new Pop());
        } else if (nextState.startsWith("#push:")) {
            actions.add(new Push(nextState.substring(6)));
        } else {
            actions.add(new Push(nextState));
        }
        ruleBuilders.get(currentState).add(new Rule(Pattern.compile(regex), actions));
    }

    // ---- Tokenization ----

    @Override
    public List<SyntaxToken> tokenize(String code) {
        if (!compiled) {
            defineRules();
            rules = new LinkedHashMap<>();
            for (Map.Entry<String, List<Rule>> entry : ruleBuilders.entrySet()) {
                rules.put(entry.getKey(), entry.getValue());
            }
            compiled = true;
        }

        List<SyntaxToken> tokens = new ArrayList<>();
        Deque<String> stateStack = new ArrayDeque<>();
        stateStack.push(ROOT);

        int pos = 0;
        int codeLen = code.length();

        while (pos < codeLen) {
            String currentStateName = stateStack.peek();
            List<Rule> stateRules = rules.get(currentStateName);
            if (stateRules == null) {
                // Unknown state, emit as text and pop
                tokens.add(new SyntaxToken(SyntaxTokenType.TEXT, code.substring(pos)));
                break;
            }

            boolean matched = false;
            for (Rule rule : stateRules) {
                Matcher m = rule.pattern.matcher(code);
                m.region(pos, codeLen);
                if (m.lookingAt()) {
                    // Apply actions
                    for (RuleAction action : rule.actions) {
                        if (action instanceof Emit emit) {
                            int group = emit.group;
                            // Fallback to group 0 if the regex doesn't have enough groups
                            if (group > m.groupCount()) {
                                group = 0;
                            }
                            String text = m.group(group);
                            if (text != null && !text.isEmpty()) {
                                tokens.add(new SyntaxToken(emit.type, text));
                            }
                        } else if (action instanceof Push push) {
                            stateStack.push(push.state);
                        } else if (action instanceof Pop) {
                            if (stateStack.size() > 1) {
                                stateStack.pop();
                            }
                        }
                    }
                    pos = m.end();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                // No rule matched, emit one character as text
                tokens.add(new SyntaxToken(SyntaxTokenType.TEXT, code.substring(pos, pos + 1)));
                pos++;
            }
        }

        return tokens;
    }
}
