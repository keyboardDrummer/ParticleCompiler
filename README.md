Blender [![Build Status](https://travis-ci.org/keyboardDrummer/Blender.svg?branch=master)](https://travis-ci.org/keyboardDrummer/Blender) [![Join the chat at https://gitter.im/LanguageBlender/Lobby#](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/LanguageBlender/Lobby#?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
===============

Blender is a language workbench, which is a tool to construct programming languages. A popular example of a language workbench is <a href="https://www.jetbrains.com/mps/">Jetbrain's MPS</a>. 

Blender's main goal is to enable creating modular and thus re-usable languages, allowing you to combine features from existing languages to create new ones. Blender gets modularity right by allowing you to both extend and constrain an existing language. Other language workbenches support only extension, which allows you to grow a small language into a bigger one, but not to transform between arbitrary ones. 

Another differentiator of Blender is its meta language. The meta language is the language used to define new languages. Unlike other tools, Blender's meta languages are embedded in a host language, allowing you to use Blender while inside a powerful general purpose programming language. Blender uses <a href="http://www.scala-lang.org/">Scala</a> as its host language. Some workbenches like <a href="https://github.com/usethesource/rascal">Rascal</a> define a stand-alone meta language, which provides a smooth experience when using its language construction features, but leaves you without the ecosystem of a popular language. Other workbenches like <a href="http://metaborg.org/en/latest/">Spoofax</a> and <a href="https://www.jetbrains.com/mps/">MPS</a> even define several meta languages, that each focus on different aspects of language definition such as syntax or typing rules. While these languages are user-friendly, they are often not programming languages, so they miss out on a lot of power.

### Delta
The core concept of Blender is a *delta*. A delta is piece of code that applies a small change to a language, such as adding/removing a language feature, or adding an optimization. Delta's are put into an ordered list to form a language. Language re-use comes from re-using these delta's. Some delta's depend on others but there's a lot of freedom in combining them. A similar approach is described in the paper '*A Nanopass Framework for Compiler Education*'.

### BiGrammar
To allow writing both a parser and a printer at the same time, Blender defines the [BiGrammar DSL](https://github.com/keyboardDrummer/Blender/wiki/BiGrammar-1:-unified-parsing-and-printing). The approach taken here is similar to that described by the paper '*Invertible Syntax Descriptions: Unifying Parsing and Pretty Printing*'.
A BiGrammar may be defined in a left recursive fashion, which can be contributed to the use of packrat parsing as described in
'*Packrat Parsing: Simple, Powerful, Lazy, Linear Time*' to deal with problems associated with such grammars.

### GUI
Blender includes a GUI. You can use this to play around with the defined deltas and construct a compiler from them.
Once you're happy with your compiler you can play around with it in the compiler cockpit. Here you can run your compiler,
and do things like ask the compiler for its in- and output grammar.

### Build instructions
1. Install <a href="http://www.scala-sbt.org/">sbt</a>
2. Call 'sbt run' in the project root

### Introduction Video
<a href="http://www.youtube.com/watch?feature=player_embedded&v=IHFHcf61g-k
" target="_blank"><img src="http://img.youtube.com/vi/IHFHcf61g-k/0.jpg" 
alt="Introduction video" width="240" height="180" border="10" /></a>

### How to Contribute
There's an infinite amount of work to be done for Blender, so contributions are very welcome. There are many different topics to work on, some suitable for an Bachelor's or Master's thesis.

Some examples of cool features:
- Parser combinators that allow defining indentation sensitive grammars, such as those of Python and Haskell.
- A DSL for static semantics, such as name binding and type checking. See the paper [A constraint language for static semantic analysis based on scope graphs](http://delivery.acm.org/10.1145/2850000/2847543/p49-antwerpen.pdf?ip=145.129.111.38&id=2847543&acc=OA&key=4D4702B0C3E38B35%2E4D4702B0C3E38B35%2E4D4702B0C3E38B35%2E77FCF3B2F09622E1&CFID=992904318&CFTOKEN=51306518&__acm__=1507451717_5c1e5970ab3ac31fbd9849edb486a802) for inspiration
- Error correcting parsing
- Generating syntactic code completion from a grammar, as in [Principled syntactic code completion using placeholders](http://delivery.acm.org/10.1145/3000000/2997374/p163-amorim.pdf?ip=145.129.111.38&id=2997374&acc=OA&key=4D4702B0C3E38B35%2E4D4702B0C3E38B35%2E4D4702B0C3E38B35%2E77FCF3B2F09622E1&CFID=992904318&CFTOKEN=51306518&__acm__=1507451951_eb454d2173854f174d05e3c1e1526bbd)
- Incremental compilation: incremental parsing, incremental type checking, etc.
- Add a new language front-end or back-end.
