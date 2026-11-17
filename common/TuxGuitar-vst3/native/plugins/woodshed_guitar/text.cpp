#include "text.h"

#include <cstring>
#include <iterator>
#include <vector>

#ifdef _WIN32
#include <windows.h>
#endif

namespace Woodshed {

namespace {

// English, the first language: every key has its text here
const Phrase kEnglishTexts[] = {
	{"profile.none", "No profile: the dry guitar is heard"},
	{"profile.name", "Profile: {0}"},
	{"profile.loading", "Loading {0}..."},
	{"profile.refused", "Not loaded: {0}"},
	{"profile.changed", "The profile file has changed since it was loaded."},
	{"editor.load", "Load profile..."},
	{"editor.update", "Update from file"},
	{"editor.input-gain", "Input gain"},
	{"editor.output-level", "Output level"},
	{"editor.quality", "Quality"},
	{"editor.quality.full", "Full"},
	{"editor.quality.light", "Light"},
	{"dialog.title", "Load profile"},
	{"dialog.captures", "NAM captures (*.nam)"},
	{"dialog.all", "All files (*.*)"},
	{"refusal.inputs", "{0} could not be loaded: it has {1} inputs, and Woodshed Guitar plays captures of one"},
	{"refusal.error", "{0} could not be loaded: {1}"},
	{"refusal.not-json", "{0} could not be loaded: it is not the JSON of a .nam file"},
	{"refusal.not-nam", "{0} is not a NAM capture: Woodshed Guitar plays .nam files"},
	{"refusal.unreadable", "{0} could not be read"},
};

// a language is added here with a table of its own, after English
const Language kLanguages[] = {{kEnglish, kEnglishTexts, std::size(kEnglishTexts)}};

// the language's text of the key; null when it has none
const char* find(const Language& language, const char* key) {
	for (size_t i = 0; i < language.count; i++) {
		if (std::strcmp(language.phrases[i].key, key) == 0) {
			return language.phrases[i].text;
		}
	}
	return nullptr;
}

// the given language's table; null when there is none
const Language* table(const Language* languages, size_t count, unsigned short language) {
	for (size_t i = 0; i < count; i++) {
		if (languages[i].id == language) {
			return &languages[i];
		}
	}
	return nullptr;
}

// where the argument goes: "{0}", "{1}"...
std::string mark(int index) {
	return "{" + std::to_string(index) + "}";
}

// the index of the argument (of so many) whose mark starts at the position; -1 when none does
int markAt(const std::string& pattern, size_t at, size_t count) {
	for (int index = 0; index < (int) count; index++) {
		if (pattern.compare(at, mark(index).size(), mark(index)) == 0) {
			return index;
		}
	}
	return -1;
}

unsigned short systemLanguage() {
#ifdef _WIN32
	return (unsigned short) PRIMARYLANGID(GetUserDefaultUILanguage());
#else
	return kEnglish;
#endif
}

}  // namespace

const char* phrase(const Language* languages, size_t count, unsigned short language, const char* key) {
	const Language* chosen = table(languages, count, language);
	const char* text = (chosen ? find(*chosen, key) : nullptr);
	if (!text) {
		text = find(languages[0], key);
	}
	return (text ? text : key);
}

// in one pass over the pattern, so an argument is never filled in turn
std::string fill(const std::string& pattern, std::initializer_list<std::string> arguments) {
	std::vector<std::string> given(arguments);
	std::string text;
	size_t at = 0;
	while (at < pattern.size()) {
		int index = markAt(pattern, at, given.size());
		if (index < 0) {
			text += pattern[at++];
		} else {
			text += given[(size_t) index];
			at += mark(index).size();
		}
	}
	return text;
}

std::string say(const char* key, std::initializer_list<std::string> arguments) {
	return fill(phrase(kLanguages, std::size(kLanguages), systemLanguage(), key), arguments);
}

}  // namespace Woodshed
