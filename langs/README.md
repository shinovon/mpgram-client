# How to make a translation

## Creating

- Take `en.jsonc` or `ru.jsonc` as an example and copy it
- Name the new file with your language code
- Edit `LocaleInfo` with your language name in English and your name
- Use UTF-8 encoding
- Do not edit comments

## Plurals

If plurals in your language have two different forms,\
 set `LocaleSlavicPlurals` to "1" and fill all keys ending with "2" with second form\
 (see `ru.jsonc`)

If plurals in your language don't come right after the number,\
 set `LocaleCustomPlurals` to "1" and put "%" character in every plural key where the number should be placed\
 (see `ca.jsonc`)

## Adding

- To add language, put language code and name into LANGS array at MP.java, line ~63.

## Updating

- When updating, translate the lines with ` // Untranslated`, then remove the comment

## Dialects

Dialects are implemented as patches to their base language to reduce file size.

To create one:

- Create .jsonc file with JSON object
- Set `LocaleInfo` to language name in English and your name
- Set `LocaleBase` to file that will be used as the base language
- Add keys that are different in your dialect (see `en_gb.jsonc`)
