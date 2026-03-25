cmake_minimum_required(VERSION 3.22.1)

project("forgeint_local")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(
        forgeint_local
        SHARED
        native-libs.cpp
)

target_compile_options(forgeint_local PRIVATE
        -Wall
        -Wextra
        -Werror=return-type
        -ffunction-sections
        -fdata-sections
        -fvisibility=hidden
        -fvisibility-inlines-hidden
        $<$<CONFIG:Release>:-Oz>
        $<$<CONFIG:Release>:-fno-exceptions>
        $<$<CONFIG:Release>:-fno-rtti>
        $<$<CONFIG:Release>:-fomit-frame-pointer>
        $<$<CONFIG:MinSizeRel>:-Oz>
        $<$<CONFIG:MinSizeRel>:-fno-exceptions>
        $<$<CONFIG:MinSizeRel>:-fno-rtti>
        $<$<CONFIG:MinSizeRel>:-fomit-frame-pointer>
)

target_link_options(forgeint_local PRIVATE
        -Wl,--gc-sections
        -Wl,--exclude-libs,ALL
        $<$<CONFIG:Release>:-Wl,--icf=safe>
        $<$<CONFIG:MinSizeRel>:-Wl,--icf=safe>
)

find_library(log-lib log)

target_link_libraries(forgeint_local
        c++_shared
        ${log-lib}
        android
)
