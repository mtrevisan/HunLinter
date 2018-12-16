Hunspeller

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg)](http://niceue.com/licenses/MIT-LICENSE.txt)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f2a1759913c44e66bd265efc1881cbf4)](https://www.codacy.com/app/mauro-trevisan/Hunspeller?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=mtrevisan/Hunspeller&amp;utm_campaign=Badge_Grade)
<a href="https://codeclimate.com/github/mtrevisan/Hunspeller/maintainability"><img src="https://api.codeclimate.com/v1/badges/cb5a4859fb27ecaea77d/maintainability" /></a>
==========

## Motivation
I created this project in order to help me construct my hunspell language files (particularly the Venetan language, you can find some tools [here](http://parnodexmentegar.orgfree.com/)). I mean `.aff` and `.dic` files, along with spellchecking and thesaurus.

The name I give to the project is temporary...

## Whant the application can do
This application is able to do many correctness checks about the files structure and content. It is able to tell you if some rule is missing or redundant. You can test rules and compound rules. You can also test hyphenation and eventually add rules. It is also able to manage thesaurus.

## How to enhance its capabilities
You can customize the tests the application made by simply add another package along with `vec`, named as the ISO639-3 code, and extending the [DictionaryCorrectnessChecker](src/main/java/unit731/hunspeller/languages/CorrectnessChecker.java), [Orthography](src/main/java/unit731/hunspeller/languages/Orthography.java), and [DictionaryBaseData](src/main/java/unit731/hunspeller/languages/DictionaryBaseData.java) classes (this last class is used to drive the Bloom filter).

Along with these classes you can insert your `rules.properties`, a file that describes variuos constraints about the rules in the `.dic` file.

After that you have to tell the application that exists those files editing the [BaseBuilder](src/main/java/unit731/hunspeller/languages/BaseBuilder.java) class and adding a `LanguageData` to the `DATAS` hashmap.

The application automatically recognize which checker to use based on the code in the `LANG` option present in the `.aff` file.

## Recognized flags
### General
SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
### Suggestions
REP
### Compounding
COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE
### Affix creation
PFX, SFX
### Others
CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX

## Screenshots
### Production
![alt text](https://i.postimg.cc/25DLks6s/Production.png "Production")

### Dictionary correctness checking
![alt text](https://i.postimg.cc/6QcJ7ZW9/Dictionary-correctness-checking.png "Dictionary correctness checking")

### Thesaurus
![alt text](https://i.postimg.cc/Jz67gSX3/Thesaurus.png "Thesaurus")

### Hyphenation
![alt text](https://i.postimg.cc/k5SrcHvg/Hyphenation.png "Hyphenation")

### Help
![alt text](https://i.postimg.cc/k5SrcHvg/Help.png "Help")
