Hunspeller

[![License MIT](https://img.shields.io/badge/license-MIT-blue.svg)](http://niceue.com/licenses/MIT-LICENSE.txt)&nbsp;[![Codacy Badge](https://api.codacy.com/project/badge/grade/1335b9b55ebd40bc934789ea5f5af751)](https://www.codacy.com/app/mauro-trevisan/library)&nbsp;<a href="https://codeclimate.com/github/mtrevisan/Hunspeller/maintainability"><img src="https://api.codeclimate.com/v1/badges/78fbcee7524a9fbe1d47/maintainability" /></a>
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
