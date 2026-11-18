#include "text.h"

#include <atomic>
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

// German
const Phrase kGermanTexts[] = {
	{"profile.none", "Kein Profil: die trockene Gitarre ist zu hören"},
	{"profile.name", "Profil: {0}"},
	{"profile.loading", "{0} wird geladen..."},
	{"profile.refused", "Nicht geladen: {0}"},
	{"profile.changed", "Die Profildatei hat sich seit dem Laden geändert."},
	{"editor.load", "Profil laden..."},
	{"editor.update", "Aus Datei aktualisieren"},
	{"editor.input-gain", "Eingangspegel"},
	{"editor.output-level", "Ausgangspegel"},
	{"editor.quality", "Qualität"},
	{"editor.quality.full", "Voll"},
	{"editor.quality.light", "Leicht"},
	{"dialog.title", "Profil laden"},
	{"dialog.captures", "NAM-Captures (*.nam)"},
	{"dialog.all", "Alle Dateien (*.*)"},
	{"refusal.inputs", "{0} ließ sich nicht laden: es hat {1} Eingänge, und Woodshed Guitar spielt Captures mit einem"},
	{"refusal.error", "{0} ließ sich nicht laden: {1}"},
	{"refusal.not-json", "{0} ließ sich nicht laden: es ist nicht das JSON einer .nam-Datei"},
	{"refusal.not-nam", "{0} ist kein NAM-Capture: Woodshed Guitar spielt .nam-Dateien"},
	{"refusal.unreadable", "{0} ließ sich nicht lesen"},
};

// a language is added here with a table of its own, after English
const Language kLanguages[] = {{kEnglish, kEnglishTexts, std::size(kEnglishTexts)}, {kGerman, kGermanTexts, std::size(kGermanTexts)}};

// the language TuxGuitar told, -1 while it told none
std::atomic<int> told(-1);

// the languages by the part of TuxGuitar's tag before "_" (TuxGuitar's root language is English)
const struct {
	const char* tag;
	unsigned short id;
} kTags[] = {{"", kEnglish}, {"en", kEnglish}, {"de", kGerman}};

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

unsigned short languageOf(const std::string& tag) {
	std::string language = tag.substr(0, tag.find_first_of("_-"));
	for (const auto& known : kTags) {
		if (language == known.tag) {
			return known.id;
		}
	}
	return 0;
}

void tell(const std::string& tag) {
	told = languageOf(tag);
}

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
	int language = told;
	return fill(phrase(kLanguages, std::size(kLanguages), (unsigned short) (language >= 0 ? language : systemLanguage()), key), arguments);
}

}  // namespace Woodshed
