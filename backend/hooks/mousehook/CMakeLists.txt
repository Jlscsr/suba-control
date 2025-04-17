cmake_minimum_required(VERSION 3.10)
project(mousehook)

# Include Node addon API headers
include_directories(${CMAKE_JS_INC})
include_directories("${CMAKE_CURRENT_SOURCE_DIR}/node_modules/node-addon-api")

# Set runtime output directory for the built module
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/build)

# Use C++17
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Build the shared library (module)
add_library(${PROJECT_NAME} SHARED src/mousehook.cpp "${CMAKE_JS_SRC}")

# Set module properties: no prefix, .node suffix
set_target_properties(${PROJECT_NAME} PROPERTIES PREFIX "" SUFFIX ".node")

# Link Node.js library
target_link_libraries(${PROJECT_NAME} ${CMAKE_JS_LIB})

# Disable exceptions for N-API compatibility
add_definitions(-DNAPI_DISABLE_CPP_EXCEPTIONS)
