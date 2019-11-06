Hunspeller
==========
![Java-12+](https://img.shields.io/badge/java-12%2B-orange.svg) [![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f2a1759913c44e66bd265efc1881cbf4)](https://www.codacy.com/app/mauro-trevisan/Hunspeller?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mtrevisan/Hunspeller&amp;utm_campaign=Badge_Grade)
<a href="https://codeclimate.com/github/mtrevisan/Hunspeller/maintainability"><img src="https://api.codeclimate.com/v1/badges/cb5a4859fb27ecaea77d/maintainability" /></a>

[![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](https://www.repostatus.org/badges/latest/active.svg)](https://www.repostatus.org/#active)
==========

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
    1. [Create an extension](#how-to-extension)
    2. [Rule flags aid](#how-to-aid)
    3. [Ordering table columns](#how-to-ordering)
    4. [Copying text](#how-to-copy)
    5. [Rule/dictionary insertion](#how-to-insertion)
6. [Screenshots](#screenshots)
    1. [Productions](#screenshots-productions)
    2. [Dictionary correctness checking](#screenshots-correctness)
    3. [Thesaurus](#screenshots-thesaurus)
    4. [Hyphenation](#screenshots-hyphenation)
    5. [Dictionary sorter](#screenshots-sorter)
    6. [Rule reducer](#screenshots-reducer)
    7. [Font selection](#screenshots-font)
    8. [Statistics](#screenshots-statistics)
7. [Changelog](#changelog)
    1. [version 1.10.0](#changelog-1.10.0)
    2. [version 1.9.1](#changelog-1.9.1)
    3. [version 1.9.0](#changelog-1.9.0)
    4. [version 1.8.1](#changelog-1.8.1)
    5. [version 1.8.0](#changelog-1.8.0)


<br/>

<a name="motivation"></a>
## Motivation
I created this project in order to help me construct my hunspell language files (particularly for the Venetan language, you can find some tools [here](http://parnodexmentegar.orgfree.com/), and the language pack [here (for the LibreOffice tools)](https://extensions.libreoffice.org/extensions/spelling-dictionary-and-hyphenator-for-the-venetan-language) and [here (for the Mozilla tools)](https://addons.mozilla.org/en-US/firefox/addon/dithionario-de-lengua-v%C3%A8neta/)). I mean `.aff` and `.dic` files, along with hyphenation and thesaurus.

**The name I give to the project is kind of temporary...**


<br/>

<a name="can-do"></a>
## What the application can do
This application is able to do many correctness checks about the files structure and its content. It is able to tell you if some rule is missing or redundant. You can test rules and compound rules. You can also test hyphenation and eventually add rules. It is also able to manage and build the thesaurus.

This application can also sort the dictionary, counting words (unique and total count), gives some statistics, duplicate extraction, wordlist extraction, minimal pairs extraction, and package creation in order to build an `.oxt` or `.xpi` for deploy.


<br/>

<a name="enhancements"></a>
## How to enhance its capabilities
You can customize the tests the application made by simply add another package along with `vec`, named as the [ISO 639-3](https://en.wikipedia.org/wiki/ISO_639-3) or [ISO 639-2](https://en.wikipedia.org/wiki/ISO_639-2) code, and extending the [DictionaryCorrectnessChecker](src/main/java/unit731/hunspeller/languages/DictionaryCorrectnessChecker.java), [Orthography](src/main/java/unit731/hunspeller/languages/Orthography.java), and [DictionaryBaseData](src/main/java/unit731/hunspeller/languages/DictionaryBaseData.java) classes (this last class is used to drive the Bloom filter).

Along with these classes you can insert your `rules.properties`, a file that describes various constraints about the rules in the `.dic` file.

After that you have to tell the application that exists those files editing the [BaseBuilder](src/main/java/unit731/hunspeller/languages/BaseBuilder.java) class and adding a `LanguageData` to the `DATAS` hashmap.

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
<a name="how-to-extension"></a>
### Create an extension
In order to create an extension (eg. for LibreOffice, or for Mozilla products) you have to use the option `File|Create package`. This will package the directory in which the `.aff/.dic` resides into a zip file. All there is to do afterwards is to rename the extensions into `.oxt` (LibreOffice), or `.xpi` (Mozilla).

Remember that the package will have the same name of the directory, but the directory itself is not included, just the content.

<a name="how-to-aid"></a>
### Rule flags aid
An external text file can be put int the directory `aids` (on the same level of the executable jar) whose content will be displayed in the drop-down element in the Dictionary tab (blank lines are ignored).

This file could be used as a reminder of all the flag that can be added to a word and their meaning.

The filename has to be the language (as specified in the option `LANG` inside the `.aff` file), and the extension `.aid` (eg. for Venetan: `vec-IT.aid`).

<a name="how-to-ordering"></a>
### Ordering table columns
It is possible to sort certain columns of the tables, just click on the header of the column. The sort order will cycle between ascending, descending, and unsorted.

<a name="how-to-copy"></a>
### Copying text
Is it possible to copy content of tables and words in the statistics section. Also the graph in the statistics section can be exported into images.

<a name="how-to-insertion"></a>
### Rule/dictionary insertion
This is **NOT** an editor tool<sup>1</sup>! If you want to add affix rules, add words in the dictionary, add hyphenation rules, or change them, you have plenty of tools around you. For Windows I suggest [Notepad++](https://notepad-plus-plus.org/ "Notepad++ homepage") (for example, you will see immediately while typing if a word is already present in the dictionary).

<sup>1</sup>: Even if for the hyphenation file a new rule can actually be added...


<br/>

<a name="screenshots"></a>
## Screenshots
<a name="screenshots-productions"></a>
### Productions
Entries can be a single word followed by a slash and all the flags that have to be applied, followed optionally by one or more morphological fields.

![alt text](https://i.postimg.cc/25DLks6s/Production.png "Production")

<a name="screenshots-correctness"></a>
### Dictionary correctness checking
![alt text](https://i.postimg.cc/6QcJ7ZW9/Dictionary-correctness-checking.png "Dictionary correctness checking")

<a name="screenshots-thesaurus"></a>
### Thesaurus
Entries can be inserted in two ways:
1. (pos)|word1|word2|word3
2. pos:word1,word2,word3

Once something is written, an automatic filtering is executed to find all the words (and part-of-speech if given) that are already contained into the thesaurus.

It is possible to click on the first column to select the row (for cancelling it, for example), or the second column (to enter the edit mode).

![alt text](https://i.postimg.cc/Jz67gSX3/Thesaurus.png "Thesaurus")

<a name="screenshots-hyphenation"></a>
### Hyphenation
![alt text](https://i.postimg.cc/k5SrcHvg/Hyphenation.png "Hyphenation")

<a name="screenshots-sorter"></a>
### Dictionary sorter
![alt text](https://i.postimg.cc/fTyL5Jww/dictionary-Sorter.png "Dictionary sorter")

<a name="screenshots-reducer"></a>
### Rule reducer
![alt text](https://i.postimg.cc/wMHN7HJ1/rule-Reducer.png "Rule reducer")

<a name="screenshots-font"></a>
### Font selection
![alt text](https://i.postimg.cc/CKFs6GdZ/font.png "Font selection")

<a name="screenshots-statistics"></a>
### Statistics
![alt text](https://i.postimg.cc/c1PRTr5Q/statistics-lengths.png "Statistics - word lengths")
![alt text](https://i.postimg.cc/T3th7sYZ/statistics-syllabes.png "Statistics - word syllabes")
![alt text](https://i.postimg.cc/NfpgBHqX/statistics-stresses.png "Statistics - word stresses")


<br/>

<a name="changelog"></a>
## Changelog
<a name="changelog-1.10.0"></a>
### version 1.10.0 - 20191106
- added buttons to open relevant files

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
