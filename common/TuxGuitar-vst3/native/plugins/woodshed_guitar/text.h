// Woodshed Guitar's texts, those its editor shows and the reasons it gives: a table of its own, as TuxGuitar's
// translation files are out of a plugin's reach. Each text has a key, so a language can be added beside English as a
// table of its own (kLanguages in text.cpp): English and German ship. The language is the one TuxGuitar shows, once
// TuxGuitar told it (tell: "Woodshed.language"), else the system's UI language (GetUserDefaultUILanguage on Windows;
// English elsewhere); English for a language it lacks and for a text a language lacks. Plain names and units ("dB")
// are not texts.
#pragma once

#include <cstddef>
#include <initializer_list>
#include <string>

namespace Woodshed {

// a language's text for a key, UTF-8; "{0}", "{1}"... stand for its arguments
struct Phrase {
	const char* key;
	const char* text;
};

// a language's texts: its primary language id (the low 10 bits of a Windows LANGID, 0x09 for English)
struct Language {
	unsigned short id;
	const Phrase* phrases;
	size_t count;
};

const unsigned short kEnglish = 0x09;
const unsigned short kGerman = 0x07;

// the primary language id of a language tag as TuxGuitar names its languages ("de", "de_DE", "pt_BR"; "" for its
// root language, English), among the languages Woodshed Guitar has texts in; 0 for another
unsigned short languageOf(const std::string& tag);

// the language TuxGuitar shows, from now on (a tag, see languageOf), for every Woodshed Guitar of the process
void tell(const std::string& tag);

// the key's text in the language among those given, the first given (English) when the language or its text is
// missing, the key itself when the first lacks it too
const char* phrase(const Language* languages, size_t count, unsigned short language, const char* key);

// the pattern with "{0}", "{1}"... replaced by the arguments in order
std::string fill(const std::string& pattern, std::initializer_list<std::string> arguments);

// the key's text in the system's UI language, filled with the arguments
std::string say(const char* key, std::initializer_list<std::string> arguments = {});

}  // namespace Woodshed
