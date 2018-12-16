Hunspeller
==========

## Motivation
I created this project in order to help me construct my hunspell language files (particularly the Venetan language, you can find some tools [here](http://parnodexmentegar.orgfree.com/)). I mean `.aff` and `.dic` files, along with spellchecking and thesaurus.

The name I give to the project is temporary...

## Whant the application do
This application is able to do many correctness checks about the files structure and content. It is able to tell you if some rule is missing or redundant. You can test rules and compound rules. You can also test hyphenation and eventually add rules. It is also able to manage thesaurus.

## How to enhance its capabilities
You can customize the tests the application made by simply add another package along with `vec`, named as the ISO639-3 code, and extending the [DictionaryCorrectnessChecker](src/main/java/unit731/hunspeller/languages/CorrectnessChecker.java), [Orthography](src/main/java/unit731/hunspeller/languages/Orthography.java), and [DictionaryBaseData](src/main/java/unit731/hunspeller/languages/DictionaryBaseData.java) classes (this last class is used to drive the Bloom filter).

Along with these classes you can insert your `rules.properties`, a file that describes variuos constraints about the rules in the `.dic` file.

After that you have to tell the application that exists those files editing the [BaseBuilder](src/main/java/unit731/hunspeller/languages/BaseBuilder.java) class and adding a `LanguageData` to the `DATAS` hashmap.

The application automatically recognize which checker to use based on the code in the `LANG` option present in the `.aff` file.
