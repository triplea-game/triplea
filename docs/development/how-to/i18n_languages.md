# *i18n* (Internationalization) in TripleA

## General Remarks

- [i18n](https://docs.oracle.com/javase/8/docs/technotes/guides/intl/index.html) is introduced into TripleA gradually
  over time
- Currently the focus is the extraction of translatable strings into *.properties files
- Subclasses of class I18nResourceBundle build a wrapper to allow singleton instantiation for a whole
  sub-package-branch (see class I18nEngineFramework)
- Each translatable string under the branch should be replaced via a call to the respective i18n-class
- Special rules need to be applied for numbers, currencies, dates, times and messages with
  parameters ([java formatting](https://docs.oracle.com/javase/tutorial/i18n/format/index.html))

## FAQ answered

### General Questions

### Quantity Questions

### Process Questions

## Open Questions

### General Questions

1. How many strings are there approximately in TripleA?
    - <todo: count strings>
2. What is our desired percentage to have internationalized?
    - 5%. The rest is either having only functional purpose or can be translated later.
3. How much work will that be?
    - I assume about a year.
4. Are we going to target to have every user-display string translated?
    - Yes, that would be the final goal. However, in a first step the extraction would be a good start.
5. If no, which kinds of strings will be english-only?
    - Any suggestions?

### Quantity Questions

1. How will these configurations scale according to the number of strings?
    - Lines will be added for each string to be translated.
2. How many config files is that going to be?
    - Presumably around 3 per game package and language.
3. Will it be manageable? Are we going to have dozens of additional config files that all need to be moved/renamed if we
   change classes? (This is an example where we are adding quite a bit of cost to relatively routine operations, instead
   of moving renaming 1 file, we have to update a test file too, and potentially 'n' translation files')
    - As it is gradually we can still rely on English without using the any new languages.
4. How many languages are we looking to have?
    - Depends on the support we find, currently 2.
5. Is there any evidence this will be more than a nice to have?
    - No, but isn't that the beauty of a hobby?
6. There is a cost to this capability, do the returns justify those costs (and particularly at this time)?
    - The cost will only increase in the future if we don't start at some point.

### Process Questions

1. We're going to be blocked pretty quickly by save game compability. What is your plan to address that?
    - What are you referring to here?
2. Will we ever have a feature be held up because we need translations? If yes, how can we avoid that? If no, how do
   ensure that we reasonably keep up with having translations?
    - I18nResourceBundleTest covers the completeness of the translations. Can we avoid that a specific test is blocking
      us?
3. How will we roll this out? It seems odd that simply booting would cause a smattering of strings to be translated but
   not quite everything. It feels like that would make for an unreleasable state - how can we avoid that so we can do
   this work incrementally and still have a releasable product after each increment?
    - We can control which language we allow and default back to English. It is also an option to allow a new settings
      option and start with an explorative feature.  