cmake_minimum_required(VERSION 3.22.1)
project("bridge_worker")

add_library(
    bridge_worker
    SHARED
    bridge_worker.c
)

find_library(
    log-lib
    log
)

target_link_libraries(
    bridge_worker
    ${log-lib}
)
