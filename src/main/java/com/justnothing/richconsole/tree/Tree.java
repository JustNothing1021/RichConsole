package com.justnothing.richconsole.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.justnothing.richconsole.text.Text;
import com.justnothing.richconsole.abc.RichRenderable;
import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.console.ConsoleOptions;
import com.justnothing.richconsole.segment.Segment;
import com.justnothing.richconsole.styled.Styled;
import com.justnothing.richconsole.style.Style;

/**
 * A renderable for a tree structure.
 * Ported from rich/tree.py.
 */
public class Tree implements RichRenderable {

    // Default style names
    private static final String DEFAULT_TREE_STYLE = "tree";
    private static final String DEFAULT_TREE_GUIDE_STYLE = "tree.line";

    // Tree drawing characters
    public static final String BRANCH = "├──";
    public static final String LAST = "└──";
    public static final String VERT = "│  ";
    public static final String SPACE = "   ";

    // ASCII alternatives
    private static final String ASCII_SPACE = "    ";
    private static final String ASCII_VERT = "|   ";
    private static final String ASCII_BRANCH = "+-- ";
    private static final String ASCII_LAST = "`-- ";

    private final TreeNode root;
    private Object style = DEFAULT_TREE_STYLE;
    private Object guideStyle = DEFAULT_TREE_GUIDE_STYLE;
    private boolean highlight = false;
    private boolean hideRoot = false;
    private boolean expanded = true;

    public static class Config {
        private Object style = "tree";
        private Object guideStyle = "tree.line";
        private boolean highlight = false;
        private boolean hideRoot = false;
        private boolean expanded = true;

        public void style(Object style) { this.style = style; }
        public void guideStyle(Object guideStyle) { this.guideStyle = guideStyle; }
        public void highlight(boolean highlight) { this.highlight = highlight; }
        public void hideRoot(boolean hideRoot) { this.hideRoot = hideRoot; }
        public void expanded(boolean expanded) { this.expanded = expanded; }
    }

    public static Tree of(Object label, Consumer<Config> configurer) {
        Config config = new Config();
        configurer.accept(config);
        return new Tree(label, config);
    }

    private Tree(Object label, Config config) {
        this.root = new TreeNode(label);
        this.style = config.style;
        this.guideStyle = config.guideStyle;
        this.highlight = config.highlight;
        this.hideRoot = config.hideRoot;
        this.expanded = config.expanded;
    }

    public Tree(Object label) {
        this.root = new TreeNode(label);
    }

    public TreeNode getRoot() {
        return root;
    }

    public Object getStyle() {
        return style;
    }

    public void setStyle(Object style) {
        this.style = style;
    }

    public Object getGuideStyle() {
        return guideStyle;
    }

    public void setGuideStyle(Object guideStyle) {
        this.guideStyle = guideStyle;
    }

    public boolean isHighlight() {
        return highlight;
    }

    public void setHighlight(boolean highlight) {
        this.highlight = highlight;
    }

    public boolean isHideRoot() {
        return hideRoot;
    }

    public void setHideRoot(boolean hideRoot) {
        this.hideRoot = hideRoot;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /**
     * Add a child to the root node and return it.
     */
    public TreeNode add(Object label) {
        return root.add(label);
    }

    /**
     * Add an existing TreeNode as a child of root.
     */
    public void add(TreeNode node) {
        root.add(node);
    }

    @Override
    public Iterable<?> richConsole(Console console, ConsoleOptions options) {
        List<Object> result = new ArrayList<>();
        boolean asciiOnly = options.isAsciiOnly();
        Style guideStyleResolved = console.getStyle(guideStyle, "");

        if (hideRoot) {
            // Render root's children directly at depth 0
            List<TreeNode> children = root.getChildren();
            for (int i = 0; i < children.size(); i++) {
                boolean isLast = (i == children.size() - 1);
                renderNode(console, options, children.get(i), "", isLast,
                        asciiOnly, guideStyleResolved, result);
            }
        } else {
            renderNode(console, options, root, "", true,
                    asciiOnly, guideStyleResolved, result);
        }
        return result;
    }

    private void renderNode(Console console, ConsoleOptions options,
                            TreeNode node, String prefix, boolean isLast,
                            boolean asciiOnly, Style guideStyleResolved,
                            List<Object> result) {
        // TODO: use options for width constraints
        String connector;
        if (prefix.isEmpty() && !hideRoot) {
            connector = "";
        } else {
            connector = isLast
                    ? (asciiOnly ? ASCII_LAST : LAST)
                    : (asciiOnly ? ASCII_BRANCH : BRANCH);
        }

        Style nodeStyle = console.getStyle(style, "");
        if (node.getStyle() != null) {
            nodeStyle = nodeStyle.add(console.getStyle(node.getStyle()));
        }

        // Render guide prefix + connector
        if (!prefix.isEmpty() || !connector.isEmpty()) {
            result.add(new Segment(prefix + connector, guideStyleResolved));
        }

        // Render label (without trailing newline - newline is added after)
        Object label = node.getLabel();
        if (label instanceof String) {
            // Create a Text with end="" to avoid trailing newline
            Text labelText =
                    Text.fromMarkup((String) label);
            labelText.setEnd("");
            result.add(new Styled(labelText, nodeStyle));
        } else {
            result.add(new Styled(label, nodeStyle));
        }
        result.add(Segment.line());

        // Render children recursively
        if (node.isExpanded()) {
            List<TreeNode> children = node.getChildren();
            for (int i = 0; i < children.size(); i++) {
                boolean childIsLast = (i == children.size() - 1);
                String childPrefix = prefix + (isLast
                        ? (asciiOnly ? ASCII_SPACE : SPACE)
                        : (asciiOnly ? ASCII_VERT : VERT));
                renderNode(console, options, children.get(i), childPrefix, childIsLast,
                        asciiOnly, guideStyleResolved, result);
            }
        }
    }

    /**
     * Inner class representing a node in the tree.
     */
    public static class TreeNode {

        private Object label;
        private final List<TreeNode> children;
        private Object style;
        private boolean expanded = true;

        public TreeNode(Object label) {
            this.label = label;
            this.children = new ArrayList<>();
        }

        public Object getLabel() {
            return label;
        }

        public void setLabel(Object label) {
            this.label = label;
        }

        public List<TreeNode> getChildren() {
            return children;
        }

        public Object getStyle() {
            return style;
        }

        public void setStyle(Object style) {
            this.style = style;
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpanded(boolean expanded) {
            this.expanded = expanded;
        }

        /**
         * Add a child node with the given label and return it.
         */
        public TreeNode add(Object label) {
            TreeNode child = new TreeNode(label);
            children.add(child);
            return child;
        }

        /**
         * Add an existing TreeNode as a child.
         */
        public void add(TreeNode node) {
            children.add(node);
        }
    }
}
