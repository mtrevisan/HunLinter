HunLinter
==========

![Java-16+](https://img.shields.io/badge/java-16%2B-orange.svg) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

<a href="https://codeclimate.com/github/mtrevisan/HunLinter"><img src="https://api.codeclimate.com/v1/badges/a99a88d28ad37a79dbf6/maintainability" /></a>

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)
==========

<br />

## Forewords
Please, be aware that this application requires Java 14+!

You can download and install it for free from this [link](https://www.oracle.com/java/technologies/javase-downloads.html).

<br />

## Main features
- affix file and dictionary linter
- rules reducer
- LibreOffice and Mozilla packager
- Part-of-Speech and dictionary FSA extractor for LanguageTools
- automatically choose a font to render custom language
- manages thesaurus, hyphenation, auto-correct, sentence exceptions, and word exception files
- minimal pairs extraction
- statistics
- &hellip; and many more!

<br />

## Table of Contents
1. [Motivation](#motivation)
2. [What the application can do](#can-do)
3. [How to enhance its capabilities](#enhancements)
4. [Recognized flags](#recognized-flags)
    1. [General](#recognized-flags-general)
    2. [Suggestions](#recognized-flags-suggestions)
    3. [Compounding](#recognized-flags-compounding)
    4. [Affix creation](#recognized-flags-affix)
    5. [Others](#recognized-flags-others)
5. [How to](#how-to)
    1. [Open a project](#how-to-project)
    2. [Create an extension](#how-to-extension)
    3. [Linter dictionary](#how-to-linter-dictionary)
    4. [Linter thesaurus](#how-to-linter-thesaurus)
    5. [Linter hyphenation](#how-to-linter-hyphenation)
    6. [Sort dictionary](#how-to-sort)
    7. [Reduce rules](#how-to-reducer)
    8. [Word count](#how-to-count)
    9. [Rule flags aid](#how-to-aid)
    10. [Dictionary statistics](#how-to-statistics)
    11. [Dictionary duplicates](#how-to-duplicates)
    12. [Dictionary wordlist](#how-to-wordlist)
    13. [Create a Part-of-Speech FSA](#how-to-posfsa)
    14. [Minimal pairs](#how-to-pairs)
    15. [Ordering table columns](#how-to-ordering)
    16. [Copying text](#how-to-copy)
    17. [Rule/dictionary insertion](#how-to-insertion)
6. [Screenshots](#screenshots)
    1. [Inflections](#screenshots-inflections)
    2. [Dictionary linter](#screenshots-correctness)
    3. [Thesaurus](#screenshots-thesaurus)
    4. [Hyphenation](#screenshots-hyphenation)
    5. [Dictionary sorter](#screenshots-sorter)
    6. [Rule reducer](#screenshots-reducer)
    7. [Font selection](#screenshots-font)
    8. [Statistics](#screenshots-statistics)
    9. [Autocorrections](#screenshots-autocorrection)
    10. [Sentence exceptions](#screenshots-sentence-exceptions)
    11. [Word exceptions](#screenshots-word-exceptions)
    12. [Part-of-Speech dictionary](#screenshots-pos-dictionary)
7. [Changelog](#changelog)
    1. [version 2.0.1](#changelog-2.0.1)
    2. [version 2.0.0](#changelog-2.0.0)
    3. [version 1.10.0](#changelog-1.10.0)
    4. [version 1.9.1](#changelog-1.9.1)
    5. [version 1.9.0](#changelog-1.9.0)
    6. [version 1.8.1](#changelog-1.8.1)
    7. [version 1.8.0](#changelog-1.8.0)


<br/>

<a name="motivation"></a>
## Motivation
I created this project in order to help me construct my hunspell language files (particularly for the Venetan language, you can find some tools [here](http://parnodexmentegar.orgfree.com/), and the language pack [here (for the LibreOffice tools)](https://extensions.libreoffice.org/extensions/spelling-dictionary-and-hyphenator-for-the-venetan-language) and [here (for the Mozilla tools)](https://addons.mozilla.org/en-US/firefox/addon/dithionario-de-lengua-v%C3%A8neta/)). I mean `.aff` and `.dic` files, along with hyphenation and thesaurus.


<br/>

<a name="can-do"></a>
## What the application can do
This application is able to do many correctness checks about the files structure and its content. It is able to tell you if some rule is missing or redundant. You can test rules and compound rules. You can also test hyphenation and eventually add rules. It is also able to manage and build the thesaurus.

This application can also sort the dictionary, counting words (unique and total count), gives some statistics, duplicate extraction, wordlist extraction, minimal pairs extraction, and package creation in order to build an `.oxt` or `.xpi` for deploy.


<br/>

<a name="enhancements"></a>
## How to enhance its capabilities
You can customize the tests the application made by simply add another package along with `vec`, named as the [ISO 639-3](https://en.wikipedia.org/wiki/ISO_639-3) or [ISO 639-2](https://en.wikipedia.org/wiki/ISO_639-2) code, and extending the [DictionaryCorrectnessChecker](src/main/java/io/github/mtrevisan/hunlinter/languages/DictionaryCorrectnessChecker.java), [Orthography](src/main/java/io/github/mtrevisan/hunlinter/languages/Orthography.java), and [DictionaryBaseData](src/main/java/io/github/mtrevisan/hunlinter/languages/DictionaryBaseData.java) classes (this last class is used to drive the Bloom filter).

Along with these classes you can insert your `rules.properties`, a file that describes various constraints about the rules in the `.dic` file.

After that you have to tell the application that exists those files editing the [BaseBuilder](src/main/java/io/github/mtrevisan/hunlinter/languages/BaseBuilder.java) class and adding a `LanguageData` to the `DATAS` hashmap.

The application automatically recognize which checker to use based on the code in the `LANG` option present in the `.aff` file.


<br/>

<a name="recognized-flags"></a>
## Recognized flags
<a name="recognized-flags-general"></a>
### General
SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
<a name="recognized-flags-suggestions"></a>
### Suggestions
REP
<a name="recognized-flags-compounding"></a>
### Compounding
COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE
<a name="recognized-flags-affix"></a>
### Affix creation
PFX, SFX
<a name="recognized-flags-others"></a>
### Others
CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX


<br/>

<a name="how-to"></a>
## How to
<a name="how-to-project"></a>
### Open a project
Select `File|Open Project`. A dialog will appear, and a blue folder (this marks a valid project) should be selected.

A `META-INF` folder containing a `manifest.xml` file is loaded, and all the information of where a particular file is are retrieved from it.

Upon loading a font is chosen that can render the content of the project. If you want another font, just select `File|Select font` and choose another one.

The font will be linked to the project so, opening it again later, the same font will be used.

<a name="how-to-extension"></a>
### Create an extension
In order to create an extension (eg. for LibreOffice, or for Mozilla products) you have to use the option `File|Create package`. This will package the directory in which the `.aff/.dic` resides into a zip file. All there is to do afterwards is to rename the extensions into `.oxt` (LibreOffice), or `.xpi` (Mozilla).

Remember that the package will have the same name of the directory, but the directory itself is not included, just the content is.

<a name="how-to-linter-dictionary"></a>
### Linter dictionary
To linter a dictionary just select `Dictionary tools|Check correctness`.

Each line is then linted following the rules of a particular language (IF the corresponding files are present in the project, e.g. for Venetan). If no such file is present a general linter is applied.

<a name="how-to-linter-thesaurus"></a>
### Linter thesaurus
To linter the thesaurus just select `Thesaurus tools|Check correctness`.

Each thesaurus entry is linted checking for the presence of each synonym as a definition (with same Part-of-Speech).

In case of error it is suggested to copy _all_ the synonyms for the indicated words (and all that came out from the filtering using those two words), remove _each_ of them, and reinsert again.

<a name="how-to-linter-hyphenation"></a>
### Linter hyphenation
To linter the hyphenation just select `Hyphenation tools|Check correctness`.

Each hyphenation code is then linted following certain rules (among them the one that says that a breakpoint should not be on the boundary, that a code should have at least a breakpoint, etc).

<a name="how-to-sort"></a>
### Sort dictionary
By selecting `Dictionary tools|Sort dictionary` you can sort specific parts of a dictionary file selecting the highlighted sections between a comment or empty line and the following.

The sorting order is language-dependent.

<a name="how-to-reducer"></a>
### Reduce rules
Use `Dictionary tools|Rules reducer` to find the minimum set of rules that can be applied following the current dictionary file.

E.g. If a dictionary file has the lines `aa/b` and `bb/b` and in the affix file are present the rules `SFX b 0 A a`, `SFX b 0 B b`, and `SFX b 0 C c` (where the last is not used), then this tools returns the minimum set of `SFX b 0 A a` and `SFX b 0 B b`.

<a name="how-to-count"></a>
### Word count
Use `Dictionary tools|Word count` to count all the words generated by the affix files, as long as unique word (not considering part-of-speech).

Note: There is an uncertainty about the uniqueness count, but it should be small. Deal with it :p.

<a name="how-to-statistics"></a>
### Dictionary statistics
Use `Dictionary tools|Statistics` to produce some statistics (graphs and values are exportable with a right click!) about word and compound word count, mode of words' length, mode of words' syllabe, most common syllabes, longest words (by letters and by syllabes).

If you want to include hyphenation statistics be sure to use `Hyphenation tools|Statistics` instead, but expect a 3.6&times; or so increase in running time.

<a name="how-to-duplicates"></a>
### Dictionary duplicates
To obtain a list of word duplicates (same word, same part-of-speech), the tool you want to use is under `Dictionary tools|Extract duplicates`.

<a name="how-to-wordlist"></a>
### Dictionary wordlist
To obtain a list of all the words generated by a dictionary and affix file, the menus `Dictionary tools|Extract wordlist` and  `Dictionary tools|Extract wordlist (plain words)` should be used.

<a name="how-to-posfsa"></a>
### Create a Part-of-Speech FSA
In order to create an [FSA](https://en.wikipedia.org/wiki/Finite-state_machine) for Part-of-Speech, suitable for use in [LanguageTool](https://languagetool.org/) you have to use the option `File|Extract PoS FSA` selecting the output folder. This will create an FSA using a provided `<language>.info` file (or automatically generated).

Remember that the FSA file will have the same name as specified in the `LANG` option in the `.aff` file, and extension `.dict`.

<a name="how-to-pairs"></a>
### Minimal paris
To obtain a list of [minimal pairs](https://en.wikipedia.org/wiki/Minimal_pair) use the menu `Dictionary tools|Extract minimal pairs`.


<a name="how-to-aid"></a>
### Rule flags aid
An external text file can be put into the directory `aids` (on the same level of the executable jar) whose content will be displayed in the drop-down element in the Dictionary tab (blank lines are ignored).

This file could be used as a reminder of all the flag that can be added to a word and their meaning.

The filename has to be the language (as specified in the option `LANG` inside the `.aff` file), and the extension `aid` (eg. for Venetan: `vec-IT.aid`).

<a name="how-to-ordering"></a>
### Ordering table columns
It is possible to sort certain columns of the tables, just click on the header of the column. The sort order will cycle between ascending, descending, and unsorted.

<a name="how-to-copy"></a>
### Copying text
Is it possible to copy content of tables and words in the statistics section. Also, the graph in the statistics section can be exported into images.

Use `Ctrl+C` after selecting the row, or use the right click of the mouse to access the popup menu.

<a name="how-to-insertion"></a>
### Rule/dictionary insertion
This is **NOT** an editor tool<sup>1</sup>! If you want to add affix rules, add words in the dictionary, or change them, you have plenty of tools around you. For Windows I suggest [Notepad++](https://notepad-plus-plus.org/ "Notepad++ homepage") (for example, you will see immediately while typing if a word is already present in the dictionary).

<sup>1</sup>: Even if for the hyphenation file a new rule can actually be added…


<br/>

<a name="screenshots"></a>
## Screenshots
<a name="screenshots-inflections"></a>
### Inflections
Entries can be a single word followed by a slash and all the flags that have to be applied, followed optionally by one or more morphological fields.

![alt text](https://i.postimg.cc/mkjFn6CD/production.png "Inflection")

<a name="screenshots-correctness"></a>
### Dictionary linter
![alt text](https://i.postimg.cc/9FX99CHq/dictionary-correctness-checking.png "Dictionary linter")

<a name="screenshots-thesaurus"></a>
### Thesaurus
Entries can be inserted in two ways:
1. (pos)|word1|word2|word3
2. pos:word1,word2,word3

Once something is written, an automatic filtering is executed to find all the words (and part-of-speech if given) that are already contained into the thesaurus.

It is possible to right click on a row to bring up the popup menu and select whether to copy it, remove it (and all the other rows in which the selected definition appears), or merge with the current synonyms.

![alt text](https://i.postimg.cc/yx1D0Xtz/thesaurus.png "Thesaurus")

![alt text](https://i.postimg.cc/XYS0h7Nr/thesaurus-merger.png "Thesaurus - Merger")

<a name="screenshots-hyphenation"></a>
### Hyphenation
![alt text](https://i.postimg.cc/QNzDQJRk/hyphenation.png "Hyphenation")

<a name="screenshots-sorter"></a>
### Dictionary sorter
![alt text](https://i.postimg.cc/hvNqKK93/dictionary-sorter.png "Dictionary sorter")

<a name="screenshots-reducer"></a>
### Rule reducer
![alt text](https://i.postimg.cc/x8X2rXbz/dictionary-rules-reducer.png "Rule reducer")

<a name="screenshots-font"></a>
### Font selection
![alt text](https://i.postimg.cc/zfqYztg5/font-chooser.png "Font selection")

<a name="screenshots-statistics"></a>
### Statistics
![alt text](https://i.postimg.cc/ZRyGjGk8/dictionary-statistics-lengths.png "Statistics - word lengths")
![alt text](https://i.postimg.cc/cCyVzFZV/dictionary-statistics-syllabes.png "Statistics - word syllabes")
![alt text](https://i.postimg.cc/T2n8bvJG/dictionary-statistics-stresses.png "Statistics - word stresses")

<a name="screenshots-autocorrection"></a>
### Autocorrection
![alt text](https://i.postimg.cc/L8CGS92k/autocorrect.png "Autocorrect")

<a name="screenshots-sentence-exceptions"></a>
### Sentence exceptions
![alt text](https://i.postimg.cc/6QFS7M7y/sentence-exceptions.png "Sentence exceptions")

<a name="screenshots-word-exceptions"></a>
### Word exceptions
![alt text](https://i.postimg.cc/6qXmcTfK/word-exceptions.png "Word exceptions")

<a name="screenshots-pos-dictionary"></a>
### Part-of-Speech dictionary
![alt text](https://i.postimg.cc/Gmg8vC15/pos-dictionary.png "PoS FSA")

<br/>

<a name="changelog"></a>
## Changelog
<a name="changelog-2.0.1"></a>
### version 2.0.1 - 20210805
- added warn for unused rules after dictionary linter
- added the possibility to hide selected columns from dictionary table
- (finally) added a windows installer
- some minor improvements on speed and linting capabilities

<a name="changelog-2.0.0"></a>
### version 2.0.0 - 20200524
- made update process stoppable
- added a linter for thesaurus
- added a menu to generate Dictionary FSA (used in [LanguageTools](https://languagetool.org/), for example)
- added a section to see the PoS FSA execution
- fixed a bug on hyphenation: when the same rule is being added (with different breakpoints), the old one is being lost
- substituted charting library
- added undo/redo capabilities on input fields
- completely revised thread management
- fixed a nasty memory leak
- now the sort dialog remains open after a sort
- categorized the errors in (true) errors and warnings, now the warning are no longer blocking
- reduced compiled size by 52% (from 6 201 344 B to 3 002 671 B)
- reduced memory footprint by 13% (for dictionary linter: from 728 MB to 630 MB)
- increased speed by 53% (for dictionary linter: from 4m 44s to 2m 13s)
- various minor bugfixes and code revisions

<a name="changelog-1.10.0"></a>
### version 1.10.0 - 20200131
- (finally) given a decent name to the project: HunLinter
- fixed a bug while selecting the font once a project is loaded
- fixed a bug while storing thesaurus information (only lowercase words are allowed)
- added update capability (the new jar will be copied in the directory of the old jar and started)
- added buttons to open relevant files
- added management of SentenceExceptList.xml and WordExceptList.xml
- added a menu to generate Part-of-Speech FSA (used in [LanguageTools](https://languagetool.org/), for example)
- made tables look more standard (copy and edit operations)
- improved thesaurus merging

<a name="changelog-1.9.1"></a>
### version 1.9.1 - 20191028
- completely revised how the loading of a project works, now it is possible to load and manage all the languages in an extension (or package), all the relevant files are read from manifest.xml and linked `.xcu` files
- the way a project is loaded in the application is changed, now the project folder (signed by a blue icon) has to be selected instead of an `.aff` file
- added the possibility to change the options for hyphenation

<a name="changelog-1.9.0"></a>
### version 1.9.0 - 20191027
- added the parsing and management of auto-correct files (only `DocumentList.xml` can be edited for now, `SentenceExceptList.xml` and `WordExceptList.xml` are currently read only)
- now all the relevant files are loaded by reading the `META-INF\manifest.xml` file, no assumptions was made
- enhancement for hyphenation section: now it is possible also to insert custom hyphenations
- bug fix on duplicate extraction
- some simplifications was made in the main menu (removed thesaurus validation on request because it will be done anyway at loading)
- improvements on thesaurus table filtering
- prevented the insertion of a new thesaurus if it is already contained
- revised the dictionary sort dialog from scratch to better handle sections between comments
- minor GUI adjustments and corrections

<a name="changelog-1.8.1"></a>
### version 1.8.1 - 20190930
- added the link to the online help
- corrected the font size on the dictionary sorter dialog
- bugfix: scroll on dictionary sorter dialog

<a name="changelog-1.8.0"></a>
### version 1.8.0 - 20190928
- introduced the possibility to choose the font (you can select it whenever you've loaded an .aff file, it will give you a list of all the fonts that can render the loaded language -- once selected the font, it will be used that for all the .aff files in that language)
