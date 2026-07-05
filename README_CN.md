# RichConsole

> [Rich](https://github.com/Textualize/rich) 的 Java 移植版 —— Python 最优秀的终端格式化库，并集成了 [noneprompt](https://github.com/nonebot/noneprompt) 的交互式提示功能。

[English](./README.md)

**这个文档是 AI 生成的，懒得写 Markdown 了嘿嘿**

RichConsole 为 Java 终端应用带来精美的富文本格式化。渲染表格、进度条、语法高亮、Markdown、树形视图等等——全部支持完整的 ANSI 样式。

## 功能特性

- **富文本 & 标记语言** — 使用 `[bold red]...[/]` 语法的行内标记
- **表格** — 带边框和网格表格，支持列样式、对齐和多行单元格
- **语法高亮** — 支持 Java、Python、JSON，提供 Monokai/Vim 主题和自定义词法分析器
- **Markdown** — 支持 GFM 表格、带语法高亮的代码块、标题、列表、引用
- **进度条** — 带预估剩余时间、已用时间、速度和加载动画的动画进度条
- **树形视图** — 目录树风格的视图，支持可配置的连接线
- **面板** — 带标题和副标题的边框容器
- **布局** — 支持键盘调整大小的分屏终端布局
- **多列** — 等宽多列渲染
- **美化打印** — 带缩进指引线的数据结构可视化
- **JSON 渲染** — 带语法高亮的 JSON，可配置缩进
- **异常回溯** — 带语法高亮源码上下文的富异常渲染
- **实时显示** — 用于进度、状态等的自动刷新显示
- **交互式提示** — 列表选择、多选框、确认和文本输入（来自 noneprompt）
- **分隔线** — 带可选标题的水平分隔线
- **对齐 / 内边距 / 约束** — 可组合的布局原语

## 快速开始

### Gradle

```groovy
dependencies {
    implementation 'io.github.richconsole:richconsole:0.1.0'
}
```

### Hello World

```java
import com.justnothing.richconsole.console.Console;

Console console = Console.of(cfg -> cfg.forceTerminal(true));
console.print("[bold green]你好，世界！[/]");
```

### 表格

```java
import com.justnothing.richconsole.table.Table;
import com.justnothing.richconsole.box.Box;

Table table = Table.of(cfg -> cfg
    .title("星球大战电影")
    .box(Box.ROUNDED)
    .expand(true));
table.addColumn("片名", "left", 40);
table.addColumn("年份", "center", 8);
table.addColumn("评分", "center", 8);
table.addRow("新希望", "1977", "8.6");
table.addRow("帝国反击战", "1980", "8.7");
console.print(table);
```

### 语法高亮

```java
import com.justnothing.richconsole.syntax.Syntax;

String code = """
    public class Hello {
        public static void main(String[] args) {
            System.out.println("你好，世界！");
        }
    }
    """;

console.print(Syntax.of(code, cfg -> cfg
    .lexerName("java")
    .lineNumbers(true)
    .wordWrap(true)));
```

### 进度条

```java
try (Progress progress = console.progress(cfg -> cfg
        .refreshPerSecond(10.0)
        .expand(true))) {
    var task = progress.addTask("处理中...", 100);
    while (!task.isFinished()) {
        Thread.sleep(100);
        task.advance(1);
    }
}
```

### 交互式提示

```java


// 单选
Choice<String> choice = ListPrompt.ask(
        "选择你的语言：", console,
        List.of(
                new Choice<>("Java", "java"),
                new Choice<>("Python", "python"),
                new Choice<>("Rust", "rust")
        ));

        // 多选
        List<Choice<String>> selected = CheckboxPrompt.ask(
                "选择功能：", console,
                List.of(
                        new Choice<>("语法高亮", "syntax"),
                        new Choice<>("进度条", "progress"),
                        new Choice<>("表格", "tables")
                ));

        // 是/否
        Boolean confirmed = ConfirmPrompt.ask("是否继续？", console);

        // 文本输入
        String name = InputPrompt.ask("你的名字：", console);
        String password = InputPrompt.askPassword("密码：", console);
```

### Markdown

```java
import com.justnothing.richconsole.markdown.Markdown;

String md = """
    # 你好，Markdown！
    
    这是 **粗体** 和 *斜体* 文本。
    
    | 名称  | 类型 |
    |-------|------|
    | Panel | Block |
    | Rule  | Block |
    """;

console.print(new Markdown(md));
```

### 使用 Config 自定义样式

所有主要组件均支持 `Config` + `Consumer<Config>` 模式，实现清晰的关键字式构造：

```java
// 控制台
Console console = Console.of(cfg -> cfg
    .forceTerminal(true)
    .width(120));

// 表格
Table table = Table.of(cfg -> cfg
    .title("结果")
    .box(Box.HEAVY)
    .expand(true)
    .showLines(true));

// 面板
Panel panel = Panel.of(content, cfg -> cfg
    .title("我的面板")
    .titleAlign("center")
    .borderStyle("red")
    .expand(true));

// 语法高亮
Syntax syntax = Syntax.of(code, cfg -> cfg
    .lexerName("java")
    .lineNumbers(true)
    .wordWrap(true)
    .theme("monokai"));

// 进度条（通过 console 便捷方法）
Progress progress = console.progress(cfg -> cfg
    .refreshPerSecond(10.0)
    .expand(true)
    .transient(false));

// 文本
Text text = Text.of("你好", cfg -> cfg
    .style("bold cyan")
    .justify("center")
    .end(""));

// 树
Tree tree = Tree.of("根节点", cfg -> cfg
    .style("green")
    .guideStyle("dim")
    .hideRoot(true));
```

## 架构

```
io.github.richconsole
├── console/       — Console 核心、渲染引擎、选项
├── table/         — 表格（带边框/网格模式）
├── panel/         — 带标题和边框的面板
├── text/          — 支持标记的富文本
├── style/         — 样式定义与 ANSI 渲染
├── segment/       — 基于 Segment 的渲染管线
├── syntax/        — 语法高亮（Java/Python/JSON）
├── markdown/      — Markdown 渲染（GFM）
├── progress/      — 进度追踪
├── progressbar/   — ProgressBar 组件
├── spinner/       — 加载动画
├── status/        — 状态动画
├── live/          — 实时自动刷新显示
├── tree/          — 树形视图
├── rule/          — 水平分隔线
├── align/         — 对齐包装器
├── padding/       — 内边距包装器
├── constrain/     — 尺寸约束
├── columns/       — 多列布局
├── layout/        — 分屏布局
├── pretty/        — 美化打印
├── json/          — JSON 渲染
├── traceback/     — 异常回溯渲染
├── noneprompt/    — 交互式提示（列表/多选/确认/输入）
├── color/         — 颜色系统检测与渲染
├── box/           — 表格/面板的边框字符
├── markup/        — 标记解析器
├── theme/         — 主题支持
├── cells/         — 单元格宽度测量
├── control/       — ANSI 控制序列
└── abc/           — 基础接口（RichRenderable）
```

## 依赖

- **JLine 3** — 终端检测和原始模式（用于交互式提示）
- **commonmark-java** — 支持 GFM 扩展的 Markdown 解析
- **Jackson** — JSON 解析与树模型
- **Brigadier** — NoneColdWind 给我加的依赖，以后可能会用？

## 环境要求

- Java 17+（语言级别），Java 8+（API 级别）— 使用 Java 17 语言特性（文本块、switch 表达式等），但仅调用 Java 8 API，以确保最大兼容性

## 致谢

本项目移植自两个优秀的 Python 库：

- **[Rich](https://github.com/Textualize/rich)** 作者 [Will McGugan](https://github.com/willmcgugan) —— 启发了整个项目的原始 Python 库。如果你觉得 RichConsole 有用，请考虑[在 GitHub 上赞助 Will](https://github.com/sponsors/willmcgugan)。
- **[noneprompt](https://github.com/nonebot/noneprompt)** 由 NoneBot 团队开发 —— 我们移植的 Python 交互式提示库，用于 `noneprompt` 模块。

## 许可证

MIT
