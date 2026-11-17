# Downloads a pinned source archive once, checked against its SHA256, so the build is exact and needs no git.
#
#   tg_fetch_archive(<directory> <url> <top> <marker> <sha256> <hint> [<pattern>...])
#
# extracts the archive's top folder <top> (only the patterns under it, when given: an archive's samples or tests may hold
# paths too long for Windows) into <directory>, and leaves the file .tg-<marker> there: a directory holding it is not
# downloaded again. A download that fails stops the build, saying <hint> (how to build from a local copy instead).

function(tg_fetch_archive directory url top marker sha256 hint)
	if(EXISTS "${directory}/.tg-${marker}")
		return()
	endif()
	get_filename_component(destination "${directory}" DIRECTORY)
	set(archive "${destination}/${top}.tar.gz")
	message(STATUS "Downloading ${url}")
	file(DOWNLOAD "${url}" "${archive}" EXPECTED_HASH SHA256=${sha256} STATUS status TLS_VERIFY ON)
	list(GET status 0 code)
	if(NOT code EQUAL 0)
		message(FATAL_ERROR "Could not download ${url} (${status}). ${hint}")
	endif()
	set(patterns "${top}/*")
	if(ARGN)
		list(TRANSFORM ARGN PREPEND "${top}/" OUTPUT_VARIABLE patterns)
	endif()
	file(REMOVE_RECURSE "${directory}" "${destination}/${top}")
	file(ARCHIVE_EXTRACT INPUT "${archive}" DESTINATION "${destination}" PATTERNS ${patterns})
	file(RENAME "${destination}/${top}" "${directory}")
	file(TOUCH "${directory}/.tg-${marker}")
	file(REMOVE "${archive}")
endfunction()

# fails the build unless the first line of the file is the licence expected
function(tg_check_licence file expected)
	file(STRINGS "${file}" first LIMIT_COUNT 1)
	if(NOT first STREQUAL expected)
		message(FATAL_ERROR "${file} is not the licence expected (${expected}): ${first}")
	endif()
endfunction()
