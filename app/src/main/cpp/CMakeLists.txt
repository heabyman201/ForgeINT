cmake_minimum_required(VERSION 3.22.1)

project("forgeint_local")

# --- FIX START: Define C++ Standard ---
# This fixes the "undefined symbol: std::__ndk1::to_string" error
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
# --- FIX END ---

# Define where your llama.cpp source code lives
set(LLAMA_DIR ${CMAKE_SOURCE_DIR}/llama.cpp)

# 1. OPTIMIZATION & DEFINITIONS
add_compile_definitions(GGML_VERSION=\"beta\")
add_compile_definitions(GGML_COMMIT=\"unknown\")
add_compile_definitions(GGML_SCHED_MAX_COPIES=4)

# Add -fexceptions (llama.cpp might use them) and optimization flags
add_compile_options(-O3 -DGGML_USE_K_QUANTS -fexceptions -frtti)

if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_compile_options(
            -DGGML_CPU_ARM_ARCH=8
            -D__ARM_FEATURE_DOTPROD=1
            -D__ARM_FEATURE_FP16_VECTOR_ARITHMETIC=1
    )
    elseif(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    add_compile_options(
            -DGGML_CPU_ARM_ARCH=7
            -march=armv7-a+dotprod+fp16
            -mfpu=neon-vfpv4
            -mfloat-abi=softfp
            -D__ARM_NEON=1
    )
endif()

# 2. GATHER SOURCE FILES
file(GLOB_RECURSE LLAMA_SOURCES ${LLAMA_DIR}/src/*.cpp)
file(GLOB GGML_SOURCES ${LLAMA_DIR}/ggml/src/*.c ${LLAMA_DIR}/ggml/src/*.cpp)

# 3. CREATE THE LIBRARY
add_library(
        forgeint_local
        SHARED
        ai.cpp
        native-libs.cpp
        ${LLAMA_SOURCES}
        ${GGML_SOURCES}
)

# 4. INCLUDE HEADERS
target_include_directories(forgeint_local PRIVATE
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
        ${LLAMA_DIR}/ggml/src
        ${LLAMA_DIR}/ggml/src/ggml-cpu
)

# 5. LINK SYSTEM LIBRARIES
find_library(log-lib log)

target_link_libraries(forgeint_local
        ${log-lib}
        m
        android
        # Explicitly link standard library (usually handled by Gradle, but safe to add)
        c++
)