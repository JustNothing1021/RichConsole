package com.justnothing.richconsole.demo;

import com.justnothing.richconsole.console.Console;
import com.justnothing.richconsole.tree.Tree;
import com.justnothing.richconsole.tree.Tree.TreeNode;

/**
 * Tree Demo — 展示树形结构渲染。
 */
public class TreeDemo {

    public static void main(String[] args) {
        Console console = Console.of(cfg -> {});

        // =====================================================================
        // 1. Basic tree
        // =====================================================================
        console.rule("Basic Tree");
        Tree root = new Tree("RichConsole");
        root.add("Console");
        root.add("Panel");
        root.add("Table");

        TreeNode progress = root.add("Progress");
        progress.add("ProgressBar");
        progress.add("Spinner");

        TreeNode text = root.add("Text");
        text.add("Span");
        text.add("Markup");

        console.println(root);
        console.println();

        // =====================================================================
        // 2. File system tree
        // =====================================================================
        console.rule("File System Tree");
        Tree fs = new Tree("src/");
        TreeNode main = fs.add("main/");
        TreeNode java = main.add("java/");
        java.add("Console.java");
        java.add("Panel.java");
        java.add("Table.java");

        TreeNode resources = main.add("resources/");
        resources.add("styles.css");

        TreeNode test = fs.add("test/");
        TreeNode testJava = test.add("java/");
        testJava.add("ConsoleTest.java");
        testJava.add("PanelTest.java");

        console.println(fs);
        console.println();

        // =====================================================================
        // 3. Styled tree with markup
        // =====================================================================
        console.rule("Styled Tree");
        Tree styled = new Tree("[bold magenta]Project[/]");
        styled.add("[cyan]README.md[/]");
        styled.add("[green].gitignore[/]");

        TreeNode src = styled.add("[yellow]src/[/]");
        src.add("[blue]main/[/]");
        src.add("[red]test/[/]");

        styled.add("[dim]build.gradle[/]");
        styled.add("[dim]settings.gradle[/]");

        console.println(styled);
        console.println();
    }
}
