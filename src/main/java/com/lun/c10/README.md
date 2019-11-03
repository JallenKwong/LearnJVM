# 早期（编译器）优化 #

[1.概述](#概述)

[2.Javac编译器](#javac编译器)

[2.1.Javac的源码与调试](#javac的源码与调试)

[2.2.解析与填充符号表](#解析与填充符号表)

[2.2.1.词法、语法分析](#词法语法分析)

[2.2.2.填充符号表](#填充符号表)

[2.3.注解处理器](#注解处理器)

[2.4.语义分析与字节码生成](#语义分析与字节码生成)

[2.4.1.标注检查](#标注检查)

[2.4.2.数据及控制流分析](#数据及控制流分析)

[2.4.3.解语法糖](#解语法糖)

[2.4.4.字节码生成](#字节码生成)

[3.Java语法糖的味道](#java语法糖的味道)

[3.1.泛型与类型擦除](#泛型与类型擦除)

[3.2.自动装箱、拆线与遍历循环](#自动装箱拆线与遍历循环)

[3.3.条件编译](#条件编译)

[4.实战：插入式注解处理器](#实战插入式注解处理器)

[4.1.实战目标](#实战目标)

[4.2.代码实现](#代码实现)

[4.3.运行与测试](#运行与测试)

[4.4.其它应用案例](#其它应用案例)

## 概述 ##

Java 语言的 “编译期” 其实是一段 “不确定” 的操作过程，因为

- 它可能是指一个前端编译器（其实叫 “编译器的前端” 更准确一些）把 \*.java 文件转变成 \*.class 文件的过程；
- 也可能是指虚拟机的后端运行期编译器（JIT 编译器，Just In Time Compiler）把字节码转变成机器码的过程；
- 还可能是指使用静态提前编译器（AOT 编译器，Ahead Of Time Compiler）直接把 \*.java 文件编译成本地机器代码的过程。

下面列举了这 3 类编译过程中一些比较有代表性的编译器。

- 前端编译器：Sun 的 Java、Eclipse JDT 中的增量式编译器（ECJ）。
- JIT 编译器：HotSpot VM 的 C1、C2 编译器。
- AOT 编译器：GNU Compiler for the Java （GCJ）、Excelsior JET。

这 3 类过程中最符合大家对 Java 程序编译认知的应该是第一类，在后续文字里，提到的 “编译期” 和 “编译器” 都仅限于第一类编译过程。

限制了编译范围后，我们对于 “优化” 二字的定义就需要宽松一些，因为 Javac 这类编译器对代码的运行效率**几乎没有任何优化措施**（在 JDK 1.3 之后，Javac 的 -O 优化参数就不再有意义）。虚拟机设计团队把**对性能的优化集中到了后端的即时编译器中**，这样可以让那些不是由 Javac 产生的 Class 文件（如 JRuby、Groovy 等语言的 Class 文件）也同样能享受到编译器优化所带来的好处。

但是 Javac 做了许多针对 Java 语言编码过程的优化措施来改善程序员的编码风格和提高编码效率。相当多新生的 Java 语法特性，都是靠编译器的 “**语法糖**” 来实现，而不是依赖虚拟机的底层改进来支持，可以说，Java 中即时编译器在运行期的优化过程对于程序运行来说更重要，而前端编译器在编译期的优化过程对于程序编码来说关系更加密切。

## Javac编译器 ##

**分析源码是了解一项技术的实现内幕的最有效的手段**，Javac 编译器不像 HotSpot 虚拟机那样使用 C++ 语言（包含少量 C 语言）实现，**它本身就是一个由 Java 语言编写的程序**，这为纯 Java 的程序员了解它的编译过程带来了很大的便利。

### Javac的源码与调试 ###

Javac 的源码存放在 JDK_SRC_HOME/langtools/src/share/classes/com/sun/tools/javac 中，除了 JDK 自身的 API 外，就只引用了JDK_SRC_HOME/langtools/src/share/classes/com/sun/* 里面的代码，调试环境建立起来简单方便，因为基本上不需要处理依赖关系。

以 Eclipse IDE 环境为例，先建立一个名为 “Compiler_javac” 的 Java 工程，然后把 JDK_SRC_HOME/langtools/src/share/classes/com/sun/* 目录下的源文件全部复制到工程的源码目录中，如下图所示。

![](image/javac.png)

导入代码期间，可能会提示 “AccessA Restriction”，被 Eclipse 拒绝编译。

这是由于 Eclipse 的 JRE System Library 中默认包含了一系列的代码访问规则（Access Rules），如果代码中引用了这些访问规则所禁止引用的类，就会提示这个错误。可以通过添加一条允许访问 JAR 包中所有类的访问规则来解决这个问题，如下图所示。

![](image/access.png)

导入了 Javac 的源码后，就可以运行 com.sun.tools.javac.Main 的 main() 方法来执行编译了，与命令行中使用 Javac 的命令没有什么区别。

虚拟机规范严格定义了 Class 文件的格式，但是《Java 虚拟机规范（第 2 版）》中，虽然有专门的一章 “Compiling for the Java Virtual Machine”，但都是以举例的形式描述，并没有对如何把 Java 源码文件转变为 Class 文件的编译过程进行十分严格的定义，这导致 Class 文件编译在某种程度上是与具体 JDK 实现相关的，在一些极端情况，可能出现一段代码 Javac 编译器可以编译，但是 ECJ 编译器就不可以编译的问题。从 Sun Javac 的代码来看，编译过程大致可以分为 3 个过程，分别是：

- 解析与填充符号表过程。
- 插入式注解处理器的注解处理过程。
- 语法分析与字节码生成过程。

这 3 个步骤之间的关系与交互顺序如下图所示。

![](image/javac2.png)

Javac 编译动作的入口是 com.sun.tools.javac.main.JavaCompiler 类，上述 3 个过程的代码逻辑集中在这个类的 compile() 和 compile2() 方法中，其中主体代码如下图所示，整个编译最关键的处理就由图中标注的 8 个方法来完成。

![](image/javac3.png)

### 解析与填充符号表 ###

解析步骤由上图中的 parseFiles() 方法完成，解析步骤包括了经典程序编译原理中的词法分析和语法分析两个过程。

#### 词法、语法分析 ####

词法分析是将源代码的字符流转变为标记（Token）集合，单个字符是程序编写过程的最小元素，而标记则是编译过程的最小元素，关键字、变量名、字面量、运算符都可以成为标记，如 “int a=b+2” 这句代码包含了 6 个标记，分别是 int、a、=、b、+、2，虽然关键字 int 由 3 个字符构成，但是它只是一个 Token，不可再拆分。在 Javac 的源码中，词法分析过程由com.sun.tools.javac.parser.Scanner 类来实现。

语法分析是根据 Token 序列构造抽象语法树的过程，抽象语法树（Abstract Syntax Tree，AST）是一种用来描述程序代码语法结构的树形表示方式，语法树的每一个节点都代表着程序代码中的一个语法结构（Construct），例如包、类型、修饰符、运算符、接口、返回值甚至代码注释等都可以是一个语法结构。

上图是根据 Eclipse AST View 插件（分析出来的某段代码的抽象语法树视图，读者可以通过这张图对抽象语法树有一个直观的认识。在 Javac 的源码中，语法分析过程由 com.sun.tools.javac.parser.Parser 类实现，这个阶段产出的抽象语法树由com.sun.tools.javac.tree.JCTree 类表示，经过这个步骤之后，编译器就基本不会再对源码文件进行操作了，后续的操作都建立在抽象语法树之上。

![](image/ast.png)

#### 填充符号表 ####

完成了语法分析和词法分析之后，下一步就是填充符号表的过程，也就是enterTrees() 方法所做的事情。

符号表（Symbol Table）是由一组符号地址和符号信息构成的表格，读者可以把它想象成哈希表中 K-V 值对的形式（实际上符号表不一定是哈希表实现，可以是有序符号表、树状符号表、栈结构符号表等）。

符号表中所登记的信息在编译的不同阶段都要用到。在语义分析中，符号表所登记的内容将用于语义检查（如检查一个名字的使用和原先的说明是否一致）和产生中间代码。在目标代码生成阶段，当对符号名进行地址分配时，符号表是地址分配的依据。

在 Javac 源代码中，填充符号表的过程由 com.sun.tools.javac.comp.Enter 类实现，此过程的出口是一个待处理列表（To Do List），包含了每一个编译单元的抽象语法树的顶级节点，以及 package-info.java（如果存在的话）的顶级节点。

### 注解处理器 ###

在 JDK 1.5 之后，Java 语言提供了对注解（Annotation）的支持，这些注解与普通 Java 代码一样，是在运行期间发挥作用的。

在 JDK 1.6 中实现了 JSR-269 规范（JSR-269：Pluggable Annotations Processing API（插入式注解处理 API）），提供了一组插入式注解处理器的标准 API 在编译期间对注解进行处理，**可以把它看做是一组编译器的插件**，**在这些插件里面，可以读取、修改、添加抽象语法树中的任意元素**。如果这些插件在处理注解期间对语法树进行了修改，编译器将回到解析及填充符号表的过程重新处理，知道所有插入式注解处理器都没有再对语法树进行修改为止，每一次循环称为一个 Round。

有了编译器注解处理的标准 API 后，我们的代码才有可能干涉编译器的行为，由于语法树中的任意元素，甚至包括代码注释都可以在插件之中访问到，所以通过插入式注解处理器实现的插件在功能上有很大的发挥空间。**只要有足够的创意，程序员可以使用插入式注解处理器来实现许多原本只能在编码中完成的事情**。

在 Javac 源码中，插入式注解处理器的初始化过程是在 initProcessAnnotations() 方法中完成的，而它的执行过程则是在 processAnnotations() 方法中完成的，这个方法判断是否还有新的注解处理器需要执行，如果有的话，通过 com.sun.tools.javac.processing.JavacProcessingEnvironment 类的 doProcessing() 方法生成一个新的 JavaCompiler 对象对编译的后续步骤进行处理。

### 语义分析与字节码生成 ###

语法分析之后，编译器获得了程序代码的抽象语法树表示，语法树能表示一个结构正确的源程序的抽象，但无法保证源程序是符合逻辑的。而语义分析的主要任务是对结构上正确的源程序进行上下文有关性质的审查，如进行类型审查。举个例子，假设有如下的 3 个变量定义语句：

	int a = 1;
	boolean b = false;
	char c = 2;

后续可能出现的赋值运算：

	int d = a + c;
	int d = b + c;//在Java不能编译
	char d = a + c;//在Java不能编译

后续代码中如果出现了如上 3 种赋值运算的话，那它们都能构成结构正确的语法树，但是只有第 1 种的写法在语义上是没有问题的，能够通过编译，其余两种在 Java 语言中是不合逻辑的，无法编译（是否合乎语义逻辑必须限定在语言与具体的上下文环境之中才有意义。如在 C 语言中，a、b、c 的上下文定义不变，第 2、3 种写法都是可以正确编译）。

#### 标注检查 ####

Javac 的编译过程中，语义分析过程分为标注检查以及数据及控制流分析两个步骤，分别由 attribute() 和 flow() 方法完成。

**标注检查步骤检查的内容包括诸如变量使用前是否已被声明、变量与赋值之间的数据类型是否能够匹配等**。在标注检查步骤中，还有一个重要的动作称为常量折叠，如果我们在代码中写了如下定义：

	int a = 1 + 2;

那么在语法树上仍然能看到字面量 “1”、“2” 以及操作符 “+”，但是在经过常量折叠之后，它们将会被折叠为字面量 “3”，如下图所示，这个插入式表达式（Infix Expression）的值已经在语法树上标注出来了（ConstantExpressionValue：3）。由于编译期间进行了常量折叠，所以在代码里面定义 “a=1+2” 比起直接定义 “a=3”，并不会增加程序运行期哪怕仅仅一个 CPU 指令的运算量。

![](image/ast2.png)

标注检查步骤在 Javac 源码中的实现类是 com.sun.tools.javac.comp.Attr 类和 com.sun.tools.javac.comp.Check 类。

#### 数据及控制流分析 ####

**数据及控制流分析是对程序上下文逻辑更进一步的验证，它可以检测出诸如程序局部变量是在使用前是否有赋值、方法的每条路径是否都有返回值、是否所有的受查异常都被正确处理了等问题**。编译时期的数据及控制流分析与类加载时数据及控制流分析的目的基本上是一致的，但校验范围有所区别，有一些校验只有在编译期或运行期才能进行。下面举一个关于 final 修饰符的数据及控制流分析的例子，见代码如下。

	// 方法一带有 final 修饰
	public void foo(final int arg) {
		final int var = 0;
		// do something
	}
		
	// 方法而没有 final 修饰
	public void foo(int arg) {
		int var = 0;
		// do something
	}

在这两个 foo() 方法中，第一种方法的参数和局部变量定义使用了 final 修饰符，而第二种方法则没有，在代码编写时程序肯定会受到 final 修饰符的影响，不能再改吧 arg 和 var 变量的值，**但是这两段代码编译出来的 Class 文件是没有任何一点区别的**，

通过[Class类文件的结构](../c06)讲解，局部变量与字段（实例变量、类变量）是有区别的，它在常量池中没有 CONSTANT_Fieldref_info 的符号引用，自然就没有访问标志（Access_Flags）的信息，甚至可能连名称都不会保留下来（取决于编译时的选项），自然在 Class 文件中不可能知道一个局部变量是不是声明为 final 了。因此，将局部变量声明为 final，对运行期是没有影响的，变量的不变性仅仅由编译器在编译期间保障。在 Javac 的源码中，数据及控制流分析的入口是flow() 方法，具体操作由 com.sun.tools.javac.comp.Flow 类来完成。

#### 解语法糖 ####

**语法糖**（Syntactic Sugar），也称糖衣语法，是由英国计算机科学家彼得·约翰·兰达（Perter J.Landin）发明的一个术语，**指在计算机语言中添加的某种语法，这种语法对语言的功能并没有影响，但是更方便程序员使用**。通常来说，使用语法糖能够增加程序的可读性，从而减少程序代码出错的机会。

Java 在现代编程语言之中属于 “低糖语言”（相对于 C# 及许多其他 JVM 语言来说），尤其是 JDK 1.5 之前的版本，“低糖” 语法也是 Java 语言被怀疑已经 “落后” 的一个表面理由。Java 中最常用的语法糖主要是前面提到过的泛型（泛型并不一定都是语法糖实现，如 C# 的泛型就是直接由 CLR 支持的）、变长参数、自动装箱 / 拆箱等，虚拟机运行时不支持这些语法，它们在编译阶段还原回简单的基础语法结构，这个过程称为解语法糖。

在 Javac 的源码中，解语法糖的过程由 desugar() 方法触发，在 com.sun.tools.javac.comp.TransTypes 类和com.sun.tools.javac.comp.Lower 类中完成。

#### 字节码生成 ####

字节码生成是 Javac 编译过程的最后一个阶段，在 Javac 源码里面由com.sun.tools.javac.jvm.Gen 类来完成。字节码生成阶段不仅仅是把前面各个步骤所生成的信息（语法树、符号表）转化成字节码写到磁盘中，编译器还进行了少量的代码添加和转换工作。

例如，前面多次提到的实例构造器 <init&gt;() 方法和类构造器 <clinit&gt;() 方法**就是在这个阶段添加到语法树之中**的（注意，这里的实例构造器并不是指默认的构造函数，如果用户代码中没有提供任何构造函数，那编译器将会添加一个没有参数的、访问性（public、protected 或 private）与当前类一直的默认构造函数，这个工作在填充符号表阶段就已经完成），这两个构造器的产生过程实际上是一个**代码收敛**的过程，编译器会把

1. 语句块（对于实力构造器而言是 “{}” 块，对于类构造器而言是 “static{}” 块）、
2. 变量初始化（实力变量和类变量）、
3. 调用父类的实例构造器（仅仅是实例构造器，<clinit&gt;() 方法中无须调用父类的 <clinit&gt;() 方法，虚拟机会自动保证父类构造器的执行，但在 <clinit&gt;() 方法中经常会生成调用 java.lang.Object 的 <init&gt;() 方法的代码）等操作收敛到 <init&gt;() 和 <clinit&gt;() 方法之中，并且保证一定是按先执行父类的实例构造器，

然后初始化变量，最后执行语句块的顺序进行，上面所述的动作由 Gen.normalizeDef() 方法来实现。除了生成构造器以外，还有其他的一些代码替换工作用于优化程序的实现逻辑，如把字符串的加操作替换为 StringBuffer 或 StringBuilder（取决于目标代码的版本是否大于或等于 JDK 1.5）的 append() 操作等。

完成了对语法树的遍历和调整之后，就会把填充了所有所需信息的符号表交给 com.sun.tools.javac.jvm.ClassWriter 类，由这个类的 writeClass() 方法输出字节码，生成最终的 Class 文件，到此为止整个编译过程宣告结束。

## Java语法糖的味道 ##

几乎各种语言或多或少都提供过一些语法糖来方便程序员的代码开发，这些语法糖虽然不会提供实质性的功能改进，但是它们或能提高效率，或能提升语法的严谨性，或能减少编码出错的机会。不过也有一种观点认为语法糖并不一定都是有益的，大量添加和使用 “含糖” 的语法，容易让程序员产生依赖，无法看清语法糖的糖衣背后，程序代码的真实面目。

总而言之，**语法糖可以看做是编译器实现的一些 “小把戏”**，这些 “小把戏” 可能会使得效率 “大提升”，但我们也应该去了解这些 “小把戏” 背后的真实世界，那样才能利用好它们，而不是被它们所迷惑。

### 泛型与类型擦除 ###

泛型是 JDK 1.5 的一项新增特性，它的本质是参数化类型（Parametersized Type）的应用，也就是说操作的数据类型被指定为一个参数。这种参数类型可以用在类、接口和方法的创建中，分别称为泛型类、泛型接口和泛型方法。

泛型思想早在 C++ 语言的模板（Template）中就开始生根发芽，在 Java 语言处于还没有出现泛型的版本时，只能通过 Object 是所有类型的父类和类型强制转换两个特点的配合来实现类型泛化。例如，在哈希表的存取中，JDK 1.5 之前使用 HashMap 的 get() 方法，返回值就是一个 Object 对象，由于 Java 语言里面所有的类型都继承于 java.lang.Object，所以 Object 转型成任何对象都是有可能的。但是也因为有无限的可能性，就只有程序员和运行期的虚拟机才知道这个 Object 到底是什么类型的对象。在编译期间，编译器无法检查这个 Object 的强制转型是否成功，如果仅仅依赖程序员去保障这项操作的正确性，许多 ClassCastException 的风险就会转嫁到程序运行期之中。

泛型技术在 C# 和 Java之中的使用方式看似相同，但实现上却有着根本性的分歧，C# 里面泛型无论是在程序源码中、编译后的 IL 中（Intermediate Language，中间语言，这时候泛型是一个占位符），或是运行期的 CLR 中，都是切实存在的，List<int&gt; 与 List<String&gt; 就是两个不同的类型，它们在系统运行期生成，有自己的虚方法表和类型数据，这种实现称为类型膨胀，基于这种方法实现的泛型称为真实泛型。

Java 语言中的泛型则不一样，它只在程序源码中存在，在编译后的字节码文件中，就已经替换为原来的原生类型（Raw Type，也称为裸类型）了，并且在相应的地方插入了强制类型代码，因此，对于运行期的 Java 语言来说，ArrayList<int&gt; 与 ArrayList<String&gt; 就是同一个类，所以泛型技术实际上是 Java 语言的一颗语法糖，**Java 语言中的泛型实现方法称为类型擦除，基于这种方法实现的泛型称为伪泛型**。

举一个例子，看一下它编译后的结果是怎样的。

[GenericTry](GenericTry.java)

把这段 Java 代码编译成 Class 文件，然后再用字节码反编译工具进行反编译后，将会发现泛型都不见了（用jd-gui 查看发现声明的时候泛型还在，其他地方就变成了强制类型转换），程序又变回了 Java 泛型出现之前的写法，泛型类型都变回了原生类型，下面代码如下

![](image/generic.png)

当初 JDK 设计团队为什么选择类型擦除的方式来实现 Java 语言的泛型支持呢？是因为实现简单、兼容性考虑还是别的原因？我们已不得而知，但确实有不少人对 Java 语言提供的伪泛型颇有微词，当时甚至连《Thinking in Java》一书的作者 Bruce Eckel 也发表了一篇《**这不是泛型**！》来批评 JDK 1.5 中的泛型实现。

在当时众多的批评之中，有一些是比较表面的，还有一些从性能上说泛型会由于强制转型操作和运行期缺少针对类型的优化等从而导致比 C# 的泛型慢一些，则是完全偏离了方向，姑且不论 Java 泛型是不是真的会比 C# 泛型慢，选择从性能的角度上评价用于提升语义准确性的泛型思想就不太恰当。但笔者也并非在为 Java 的泛型辩护，它在某些场景下确实存在不足，**认为通过擦除法来实现泛型丧事了一些泛型思想应有的优雅**，

举个例子

	public class GenericTypes {
	
		public static void method(List<String> list) {
			System.out.println("invoke method(List<String> list)");
		}
		
		public static void method(List<Integer> list) {
			System.out.println("invoke method(List<Integer> list)");
		}
	}

这段代码是不能被编译的，因为参数 List<Integer&gt; 和 List<String&gt; 编译之后都被擦除了，变成了一样的原生类型 List<E&gt;，擦除动作导致这两种方法的特征签名变得一模一样。初步看来，无法重载的原因已经找到了，但真的就是如此吗？只能说，**泛型擦除成相同的原生类型只是无法重载的其中一部分原因**

举第二个例子

	public class GenericTypes {
	
		public static String method(List<String> list) {
			System.out.println("invoke method(List<String> list)");
			return "";
		}
		
		public static int method(List<Integer> list) {
			System.out.println("invoke method(List<Integer> list)");
			return 1;
		}
		
		public static void main(String[] args) {
			method(new ArrayList<String>());
			method(new ArrayList<Integer>());
		}
	
	}

执行结果：

	invoke method(List<String> list)
	invoke method(List<Integer> list)

>PS. JDK 1.8 不能编译上面代码

上面两段代码的差别是两个 method 方法添加了不同的返回值，由于这两个返回值的加入，方法重载居然成功了，即这段代码可以被编译和执行（注：测试的时候请使用 Sun JDK 1.6(1.7 和 1.8 也无法进行编译) 进行编译，其他编译器，如 Eclipse JDT 的 ECJ 编译器，仍然可能会拒绝这段代码）了。**这是对 Java 语言中返回值不参与重载选择的基本认知的挑战吗**？

上面代码的重载当然不是根据返回值来确定的，之所以这次能编译和执行成功，是因为两个 method() 方法**加入了不同的返回值**后才能共存在一个 Class 文件之中。前面介绍 Class 文件方法表（method_info）的数据结构时曾经提到过，方法重载要求方法具备不同的特征签名，返回值并不包含在方法的特征签名之中，所以返回值不参与重载选择，**但是**在 Class 文件格式之中，只要描述符不是完全一致的两个方法就可以共存。也就是说，两个方法如果有相同的名称和特征签名，但返回值不同，那它们也是可以合法地共存于一个 Class 文件中的。

---

由于 Java 泛型的引入，各种场景（虚拟机解析、反射等）下的方法调用都有可能对原有的基础产生影响和新的需求，如在泛型类中如何获取传入的参数化类型等。因此，JCP 组织对虚拟机规范作出了相应的修改，引入了诸如 Signature、LocalVariableTable 等新的属性用于解决伴随而来的参数类型的识别问题，Signature是其中最重要的一项属性，它的作用就是存储一个方法在字节码层面的特征签名，这个属性中保存的参数类型并不是原生类型，而是包括了参数化类型的信息。修改后的虚拟机规范要求所有能识别 49.0 以上版本的 Class 文件的虚拟机都要能正确地识别 Signature 参数。

从上面的例子可以看到擦除法对实际编码带来的影响，由于 List<String> 和 List<Integer> 擦除后是同一个类型，我们只能添加两个并不需要实际使用到的返回值才能完成重载，**这是一种毫无优雅和美感可言的解决方案**，并且存在一定语意上的混乱。

另外，从 Signature 属性的出现我们还可以得出结论，擦除法所谓的擦除，仅仅是对方法的 Code 属性中的字节码进行擦除，实际上元数据中还是保留了泛型信息，这也是我们能通过**反射手段取得参数化类型的根本依据**。

### 自动装箱、拆线与遍历循环 ###

纯技术的角度来讲，自动装箱、自动拆箱与遍历循环（Foreach 循环）这些语法糖，无论是实现上还是思想上都不能和上文介绍的泛型相比，两者的难度和深度都有很大差距。专门拿出一节来讲解它们只有一个理由：毫无疑问，它们是 Java 语言里使用得最多的语法糖。

	public static void main(String[] args) {
		
		List<Integer> list = Arrays.asList(1, 2, 3, 4);
		// 如果在 JDK 1.8 中，还有另外一颗语法糖
		// 能让上面这句代码进一步简写成 List<Integer> list = [1, 2, 3, 4];
		int sum = 0;
		for (int i : list) {
			sum += i;
		}
		
		System.out.println(sum);
		
	}

编译之后

![](image/AutoboxingAndForeach.png)

上面代码中一共包含了泛型、自动装箱、自动拆箱、遍历循环与变长参数 5 种语法糖，上图则展示了它们在编译后的变化。泛型就不必说了，自动装箱、拆箱在编译之后被转换成了对应的包装和还原方法，如本例中的 Integer.valueOf() 与 Integer.intValue() 方法，而遍历循环则把代码还原 成了迭代器的实现，这也是为何遍历循环需要被遍历的类实现Iterable 接口的原因。最后再看看变长参数，它在调用的时候变成了一个数组类型的参数，在变长参数出现之前，程序员就是使用数组来完成类似功能的。

---

这些语法糖虽然看起来很简单，但也不见得就没有任何值得我们注意的地方，下面演示了自动装箱的一些错误用法。

	public static void main(String[] args) {
		Integer a = 1;
		Integer b = 2;
		Integer c = 3;
		Integer d = 3;
		Integer e = 321;
		Integer f = 321;
		Long g = 3L;
		System.out.println(c == d);
		System.out.println(e == f);
		System.out.println(c == (a + b));
		System.out.println(c.equals(a + b));
		System.out.println(g == (a + b));
		System.out.println(g.equals(a + b));
	}

反编译后

![](image/TrapOfAutoboxing.png)

运行结果：

	true
	false
	true
	true
	true
	false

鉴于包装类的“==” 运算在不遇到算术运算的情况下不会自动拆箱，以及它们 equals() 方法不处理数据转型的关系，**建立在实际编码中尽量避免这样使用自动装箱与拆箱**。

### 条件编译 ###

许多程序设计语言都提供了条件编译的途径，如 C、C++ 中使用预处理器指示符（#ifdef）来完成条件编译。C、C++ 的预处理其最初的任务是解决编译时的**代码依赖关系**（如非常常用的 #include 预处理命令），而在 Java 语言之中并没有使用预处理器，因为 Java 语言天然的编译方式（编译器并非一个个地编译 Java 文件，而是将所有编译单元的语法树顶级节点输入到待处理列表后再进行编译，因此各个文件直接能够互相提供符号信息）无须使用预处理器。那 Java 语言是否有办法实现条件编译呢？

Java 语言当然也可以进行条件编译，方法就是使用条件为常量的 if 语法。如下面代码所示，此代码中的 if 语句不同于其他 Java 代码，它在编译阶段就会被 “运行”，生成的字节码之中之包括 “System.out.println("block 1"); ” 一条语句，并不会包含 if 语句及另外一个分子中的 “System.out.println("block 2);”

	public static void main(String[] args) {
		if (true) {
			System.out.println("block 1");
		} else {
			System.out.println("block 2");
		}
	}

上述代码编译后 Class 文件的反编译结果：

![](image/conditional.png)

只能使用条件为常量的 if 语句才能达到上述效果，如果使用常量与其他带有条件判断能力的语句搭配，则可能在控制流分析中提示错误，被拒绝编译，如下面代码就会被编译器拒绝编译。

	public static void main(String[] args) {
		// 编译器将会提示 "Unreachable code"
		while (false) {
			System.out.println("");
		}
	}

Java 语言中条件编译的实现，也是 Java 语言的一颗语法糖，根据布尔常量值的真假，编译器将会把分支中不成立的代码块消除掉，这一工作将在编译器解除语法糖阶段（com.sun.tools.javac.comp.Lower类中完成）。由于这种条件编译的实现方式使用了 if 语句，所以它必须遵循最基本的 Java 语法，只能写在方法体内部，因此它只能实现语句基本块（Block）级别的条件编译，而没有办法实现根据条件调整整个 Java 类的结构。

除了泛型、自动装箱、自动拆箱、遍历循环、变长参数和条件编译之外，Java 语言还有不少其他的语法糖，如内部类、枚举类、断言语句、对枚举和字符串（在 JDK 1.7 中支持）的 switch 支持、try 语句中定义和关闭资源（在 JDK 1.7 中支持）等，可以通过跟踪 Javac 源码、反编译 Class 文件等方式了解它们的本质实现。

## 实战：插入式注解处理器 ##

JDK 编译优化部分在本书中并没有设置独立的实战章节，因为我们开发程序，**考虑的主要是程序会如何运行，很少会有针对程序编译的需求**。也因为这个原因，在 JDK 的编译子系统里面，提供给用户直接控制的功能相对较少，除了后面会介绍的虚拟机 JIT 编译的几个相关参数以外，我们就只能使用 JSR-296 中**定义的插入式注解处理器** API 来对 JDK 编译子系统的行为产生一些影响。

但是并不认为相对于前两部分介绍的内存管理子系统和字节码执行子系统，JDK 的编译子系统就不那么重要。一套编程语言中编译子系统的优劣，很大程度上决定了程序运行性能的好坏和编码效率的高低，尤其在 Java 语言中，运行期即时编译与虚拟机执行子系统非常紧密地互相依赖、配合运作。了解 JDK 如何编译和优化代码，有助于我们写出适合 JDK 自优化的程序。

### 实战目标 ###

通过阅读 Javac 编译器的源码，我们知道编译器在把 Java 程序编译为字节码的时候，会对 Java 程序源码做各方面的检查校验。这些校验主要以程序 “写得对不对” 为出发点，虽然也有各种 WARNING 的信息，但总体来将还是较少去校验程序 “写得好不好”。有鉴于此，业界出现了许多针对程序 “写得好不好” 的辅助校验工具，如CheckStyle、FindBug、Klocwork等。这些代码校验工具有一些是基于 Java 的源码进行校验，还有一些是通过扫描字节码来完成，在本节的实战中，我们将会使用注解处理器 API 来编写一款拥有自己编码风格的校验工具：NameCheckProcessor。

当然，由于我们的实战都是为了学习和演示技术原理，而不是为了做出一款能媲美 CheckStyle 等工具的产品来，所以 NameCheckProcessor 的目标也仅定为对 Java 程序命名进行检查，根据 《Java 语言规范（第3版）》中第 6.8 节的要求，Java 程序命名应当符合下列格式的书写规范。

- 类（或接口）：符合驼式命名法，首字母大写。
- 方法：符合驼式命名法，首字母小写。
- 字段：
	- 类或实例变量：符合驼式命名法、首字母小写。
	- 常量：要求全部由大写字母或下划线构成，并且第一个字符不能是下划线。

上文提到的驼式命名法（Camel Case Name），正如它的名称所表示的那样，是指混合使用大小写字母来分割构成变量或函数的名字，犹如驼峰一般，这是当前 Java 语言中主流的命名规范，我们的**实战目标就是为 Javac 编译器添加一个额外的功能，在编译程序时检测程序名是否符合上述对类（或接口）、方法、字段的命名要求**。

### 代码实现 ###

要通过注解处理器 API 实现一个编译器插件，首先需要了解这组 API 的一些基本知识。我们实现注解处理器的代码需要继承抽象类 javax.annotation.processing.AbstractProcessor，这个抽象类中只有一个必须覆盖的 abstract 方法：“process()”，它是 Javac 编译器在执行注解处理器代码时要调用的过程，我们可以从这个方法的第一个参数 “annotations” 中获取到此注解处理器所要处理的注解集合，从第二个参数 “roundEnv” 中访问到当前这个 Round 中的语法树节点，每个语法树节点在这里表示为一个 Element。在 JDK 1.6 新增的 javax.lang.model 包中定义了 16 类 Element，包括了 Java 代码中最常用的元素，如：

1. “包（PACKAGE）、
2. 枚举（Enum）、
3. 类（CLASS）、
4. 注解（ANNOTATION_TYPE）、
5. 接口（INTERFACE）、
6. 枚举值（ENUM_CONSTANT）、
7. 字段（FIELD）、
8. 参数（PARAMETER）、
9. 本地变量（LOCAL_VARIABLE）、
10. 异常（EXCEPTION_PARAMETER）、
11. 方法（METHOD）、
12. 构造函数（CONSTRUCTOR）、
13. 静态语句块（STATIC_INIT，即 static{} 块）、
14. 实例语句块（INSTANCE_INIT，即 {} 块）、
15. 参数化类型（TYPE_PARAMERTER，既泛型尖括号内的类型）和
16. 未定义的其他语法树节点（OTHER）”。

除了 process() 方法的传入参数之外，还有一个很常用的实例变量 “processingEnv”，它是 AbstractProcessor 中的一个 protected 变量，在注解处理器初始化的时候（init() 方法执行的时候）创建，继承了 AbstractProcessor 的注解处理器代码可以直接访问到它。它代表了注解处理器框架提供的一个上下文环境，要创建新的代码、向编译器输出信息、获取其他工具类等都需要用到这个实例变量。

注解处理器除了 process() 方法及其参数之外，还有两个可以配合使用的 Annotations：@SupportedAnnotationTypes 和 @SupportedSourceVersion，前者代表了这个注解处理器对哪些注解感兴趣，可以使用星号 “*” 作为通配符代表对所有的注解都感兴趣，后者指出这个注解处理器可以处理哪些版本的 Java 代码。

每一个注解处理器在运行的时候都是单例的，如果不需要改变或生成语法树的内容，process() 方法就可以返回一个值为 false 的布尔值，通知编译器这个 Round 中的代码未发生变化，无须构造新的 JavaCompiler 实例，在这次实战的注解处理器中只对程序命名进行检查，不需要改变语法树的内容，因此 process() 方法的返回值都是 false。关于注解处理器的 API，对这个领域有兴趣的可以阅读相关的帮助文档。下面来看看注解处理器 NameCheckProcessor 的具体代码，代码如下所示。

[NameCheckProcessor](NameCheckProcessor.java)

从上面代码可以看出，NameCheckProcessor 能处理基于 JDK 1.6 的源码，它不限于特定的注解，对任何代码都 “感兴趣”，而在 process() 方法中是把把当前 Round 中的每一个 RootElement 传递到一个名为 NameChecker 的检查器中执行名称检查逻辑，NameCheck 的代码如下所示。

>PS. 我改成1.8的 @SupportedSourceVersion(SourceVersion.RELEASE_8)

[NameChecker](NameChecker.java)

NameChecker 的代码看起来有点长，但实际上注释占了很大一部分，其实即使算上注释也不到 190 行。它通过一个继承于 javax.lang.model.util.ElementScanner6 的 NameCheckScanner 类，以 Visitor 模式来完成对语法树的遍历，分别执行 visitType()、visitVariable() 和 visitExecutable() 方法来访问类、字段和方法，这 3 个 visit 方法对各自的命名规则做相应的检查，checkCamelCase() 与  checkAllCaps() 方法则用于实现驼式命名法和全大写命名规则的检查。

整个注解处理器只需 NameCheckProcessor 和 NameChecker 两个类就可以全部完成，为了验证我们的实战成果，下面代码中提供了一段命名规范的 “反面教材” 代码，其中的每一个类、方法及字段的命名都存在问题，但是使用普通的 Javac 编译这段代码时不会提示任何一个 Warning 信息。

[BADLY_NAMED_CODE](BADLY_NAMED_CODE.java)

### 运行与测试 ###

可以通过 javac 命令的 “-processor” 参数来执行编译时需要附带的注解处理器，如果有多个注解处理器的话，用逗号分隔。还可以使用-XprintRounds 和 -XprintProcessorInfo 参数来查看注解处理器运作的详细信息，本次实战中的 NameCheckProcessor 的编译与执行过程如下所示：

![](image/result.png)

### 其它应用案例 ###

NameCheckProcessor 的实战例子只演示了 JSR-269 嵌入式注解处理器 API 中的一部分功能，基于这组 API 支持的项目还有用于校验 Hibernate 标签使用正确性的[Hibernate Validator Annotation Processor](http://www.hibernate.org/subprojects/validator.html)（本质上与 NameCheckProcessor 所做的事情差不多）、自动为字段生成 getter 和 setter 方法 [Project Lombok](http://projectlombok.org)（根据已有元素生成新的语法树元素）等，有兴趣的话可以参考它们官方站点的相关内容。



