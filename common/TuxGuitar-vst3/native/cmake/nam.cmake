# NeuralAmpModelerCore, the engine that plays NAM captures in Woodshed Guitar, pinned: release v0.6.0 of
# github.com/sdatkinson/NeuralAmpModelerCore (commit 0b3d3c97b0859a3a8c92a8628c4dd89a25eb5842), MIT-licensed, with the
# nlohmann/json 3.12.0 it holds (Dependencies/nlohmann, MIT); and Eigen, the submodule that release names
# (gitlab.com/libeigen/eigen at bc3b39870ecb690a623a3f49149a358b95c5781d, Eigen 3.5 in development, MPL2). Each is
# downloaded as its archive at that commit and checked against its SHA256 (fetch.cmake). Only what the plugin builds
# from is extracted: NAM/, the json header, the licences, and NAM's two example captures that the timing tool plays.
# Set TG_NAM_CORE_DIR (holding NAM, Dependencies/nlohmann and LICENSE) and TG_EIGEN_DIR (holding Eigen and
# COPYING.MPL2) to build from local copies instead (offline).

include("${CMAKE_CURRENT_LIST_DIR}/fetch.cmake")

set(TG_NAM_CORE_COMMIT 0b3d3c97b0859a3a8c92a8628c4dd89a25eb5842)
set(TG_EIGEN_COMMIT bc3b39870ecb690a623a3f49149a358b95c5781d)

if(NOT TG_NAM_CORE_DIR)
	set(TG_NAM_CORE_DIR "${CMAKE_BINARY_DIR}/nam/NeuralAmpModelerCore")
	tg_fetch_archive("${TG_NAM_CORE_DIR}"
		"https://github.com/sdatkinson/NeuralAmpModelerCore/archive/${TG_NAM_CORE_COMMIT}.tar.gz"
		"NeuralAmpModelerCore-${TG_NAM_CORE_COMMIT}" "${TG_NAM_CORE_COMMIT}"
		ca8bd36a66c0861c4359660440210455f436b48ba28529c7134a09a08eaaf616
		"Set TG_NAM_CORE_DIR to a local copy."
		"NAM/*" "Dependencies/nlohmann/*" "LICENSE" "example_models/wavenet_a1_standard.nam" "example_models/A2.nam")
endif()
if(NOT TG_EIGEN_DIR)
	set(TG_EIGEN_DIR "${CMAKE_BINARY_DIR}/nam/eigen")
	tg_fetch_archive("${TG_EIGEN_DIR}"
		"https://gitlab.com/libeigen/eigen/-/archive/${TG_EIGEN_COMMIT}/eigen-${TG_EIGEN_COMMIT}.tar.gz"
		"eigen-${TG_EIGEN_COMMIT}" "${TG_EIGEN_COMMIT}"
		5f6557d1aca6e959a4b955c55f2935261fd30de59c0c79e35bc6d5cbe1de22d1
		"Set TG_EIGEN_DIR to a local copy."
		"Eigen/*" "unsupported/Eigen/FFT" "unsupported/Eigen/src/FFT/*" "COPYING.MPL2" "COPYING.README")
endif()

# MIT and MPL2 both fit TuxGuitar's LGPL-2.1 (and the Windows build's GPLv3)
tg_check_licence("${TG_NAM_CORE_DIR}/LICENSE" "MIT License")
tg_check_licence("${TG_EIGEN_DIR}/COPYING.MPL2" "Mozilla Public License Version 2.0")
file(STRINGS "${TG_NAM_CORE_DIR}/Dependencies/nlohmann/json.hpp" TG_JSON_LICENCE REGEX "SPDX-License-Identifier")
if(NOT TG_JSON_LICENCE MATCHES "SPDX-License-Identifier: MIT")
	message(FATAL_ERROR "${TG_NAM_CORE_DIR}/Dependencies/nlohmann/json.hpp is not MIT-licensed (${TG_JSON_LICENCE})")
endif()

file(GLOB TG_NAM_CORE_SOURCES "${TG_NAM_CORE_DIR}/NAM/*.cpp" "${TG_NAM_CORE_DIR}/NAM/*/*.cpp")
