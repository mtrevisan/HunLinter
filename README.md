Hunspeller
==========
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

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
5. [Screenshots](#screenshots)
    1. [Productions](#screenshots-productions)
    2. [Dictionary correctness checking](#screenshots-correctness)
    3. [Thesaurus](#screenshots-thesaurus)
    4. [Hyphenation](#screenshots-hyphenation)
    5. [Rule reducer](#screenshots-reducer)
    6. [Font selection](#screenshots-font)
6. [Changelog](#changelog)
    1. [version 1.8.0](#changelog-1.8.0)


<a name="motivation"></a>
## Motivation
I created this project in order to help me construct my hunspell language files (particularly for the Venetan language, you can find some tools [here](http://parnodexmentegar.orgfree.com/)). I mean `.aff` and `.dic` files, along with hyphenation and thesaurus.

**The name I give to the project is kind of temporary...**


<a name="can-do"></a>
## What the application can do
This application is able to do many correctness checks about the files structure and its content. It is able to tell you if some rule is missing or redundant. You can test rules and compound rules. You can also test hyphenation and eventually add rules. It is also able to manage and build the thesaurus.

This application can also sort the dictionary, counting words (unique and total count), gives some statistics, duplicate extraction, wordlist extraction, minimal pairs extraction, and package creation in order to build an `.oxt` or `.xpi` for deploy.


<a name="enhancements"></a>
## How to enhance its capabilities
You can customize the tests the application made by simply add another package along with `vec`, named as the [ISO 639-3](https://en.wikipedia.org/wiki/ISO_639-3) or [ISO 639-2](https://en.wikipedia.org/wiki/ISO_639-2) code, and extending the [DictionaryCorrectnessChecker](src/main/java/unit731/hunspeller/languages/DictionaryCorrectnessChecker.java), [Orthography](src/main/java/unit731/hunspeller/languages/Orthography.java), and [DictionaryBaseData](src/main/java/unit731/hunspeller/languages/DictionaryBaseData.java) classes (this last class is used to drive the Bloom filter).

Along with these classes you can insert your `rules.properties`, a file that describes various constraints about the rules in the `.dic` file.

After that you have to tell the application that exists those files editing the [BaseBuilder](src/main/java/unit731/hunspeller/languages/BaseBuilder.java) class and adding a `LanguageData` to the `DATAS` hashmap.

The application automatically recognize which checker to use based on the code in the `LANG` option present in the `.aff` file.


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

![alt text](https://i.postimg.cc/Jz67gSX3/Thesaurus.png "Thesaurus")

<a name="screenshots-hyphenation"></a>
### Hyphenation
![alt text](https://i.postimg.cc/k5SrcHvg/Hyphenation.png "Hyphenation")

<a name="screenshots-reducer"></a>
### Rule reducer
![alt text](https://i.postimg.cc/wMHN7HJ1/rule-Reducer.png "Rule reducer")

<a name="screenshots-font"></a>
### Font selection
![alt text](https://i.postimg.cc/CKFs6GdZ/font.png "Font selection")


<a name="changelog"></a>
## Changelog
<a name="changelog-1.8.0"></a>
### version 1.8.0
- introduced the possibility to choose the font (you can select it whenever you've loaded an .aff file, it will give you a list of all the fonts that can render the loaded language -- once selected the font, it will be used that for all the .aff files in that language)
