// woodshed-guitar-text-test: checks how Woodshed Guitar chooses its texts (text.h), over a table of two languages made
// up here (only English ships), then the English it ships. Prints each check that fails; exit 0 when none does
// (TuxGuitar-vst3's TestWoodshedChecks runs it).
#include "text.h"

#include <cstdio>
#include <string>

namespace {

using namespace Woodshed;

const Phrase kFirst[] = {{"greeting", "Hello"}, {"farewell", "Bye"}, {"named", "{0} plays {1}"}};
const Phrase kSecond[] = {{"greeting", "Hallo"}};
const Language kMadeUp[] = {{kEnglish, kFirst, 3}, {0x07, kSecond, 1}};

int failures = 0;

void check(const std::string& what, const std::string& expected, const std::string& actual) {
	if (expected != actual) {
		std::printf("%s: expected \"%s\", got \"%s\"\n", what.c_str(), expected.c_str(), actual.c_str());
		failures++;
	}
}

}  // namespace

int main() {
	check("English", "Hello", phrase(kMadeUp, 2, kEnglish, "greeting"));
	check("a language that has the text", "Hallo", phrase(kMadeUp, 2, 0x07, "greeting"));
	check("a language that lacks the text", "Bye", phrase(kMadeUp, 2, 0x07, "farewell"));
	check("a language there is no table of", "Hello", phrase(kMadeUp, 2, 0x0c, "greeting"));
	check("a key no language has", "missing", phrase(kMadeUp, 2, 0x07, "missing"));
	check("the arguments in order", "Ann plays a riff", fill(phrase(kMadeUp, 2, kEnglish, "named"), {"Ann", "a riff"}));
	check("an argument twice, one not used", "a a", fill("{0} {0}", {"a", "b"}));
	check("no arguments", "{0} left", fill("{0} left", {}));
	check("an argument that holds a mark is not filled again", "{1}.nam and b", fill("{0} and {1}", {"{1}.nam", "b"}));
	check("the English shipped", "No profile: the dry guitar is heard", say("profile.none"));
	check("the English shipped, filled", "Profile: Clean.nam", say("profile.name", {"Clean.nam"}));
	return (failures == 0 ? 0 : 1);
}
