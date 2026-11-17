# Woodshed Guitar, the VST3 instrument TuxGuitar ships: plucked strings played through a NAM capture (README of the
# audio features). Its bundle goes to <build>/plugins/VST3, apart from the test plugins, as it ships in TuxGuitar's own
# plugins folder. woodshed-guitar-time (bin/) times captures played as the plugin plays them, for the CPU cost.
include(../../cmake/nam.cmake)

# the plugin's sound, apart from VST3: the strings, the resampling, the profile, the state, and NeuralAmpModelerCore
# an object library: each NAM architecture registers itself from an object nothing else refers to, which a static
# library would leave out of the link
add_library(tg_woodshed_core OBJECT voice.cpp resampler.cpp capture.cpp state.cpp ${TG_NAM_CORE_SOURCES})
target_include_directories(tg_woodshed_core PUBLIC . "${TG_NAM_CORE_DIR}" SYSTEM PUBLIC "${TG_NAM_CORE_DIR}/Dependencies/nlohmann" "${TG_EIGEN_DIR}")
target_compile_features(tg_woodshed_core PUBLIC cxx_std_20)
target_compile_definitions(tg_woodshed_core PUBLIC NAM_ENABLE_A2_FAST)
set_target_properties(tg_woodshed_core PROPERTIES POSITION_INDEPENDENT_CODE ON)
if(MSVC)
	target_compile_options(tg_woodshed_core PUBLIC /bigobj /utf-8)
	target_compile_definitions(tg_woodshed_core PUBLIC NOMINMAX WIN32_LEAN_AND_MEAN _USE_MATH_DEFINES)
endif()

set(SMTG_CUSTOM_BINARY_LOCATION "${CMAKE_BINARY_DIR}/plugins")
set(TG_WOODSHED_SOURCES plugin.cpp editor.cpp "${public_sdk_SOURCE_DIR}/source/vst/vstsinglecomponenteffect.cpp")
smtg_add_vst3plugin(tg_woodshed_guitar PACKAGE_NAME "Woodshed Guitar" SOURCES_LIST ${TG_WOODSHED_SOURCES})
target_link_libraries(tg_woodshed_guitar PRIVATE sdk tg_woodshed_core)
if(SMTG_WIN)
	target_link_libraries(tg_woodshed_guitar PRIVATE comdlg32 user32 gdi32)
endif()

# times captures played as the plugin plays them (the CPU cost a hand check cannot measure): bin/woodshed-guitar-time
add_executable(woodshed-guitar-time time.cpp)
target_link_libraries(woodshed-guitar-time PRIVATE tg_woodshed_core)
set_target_properties(woodshed-guitar-time PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
