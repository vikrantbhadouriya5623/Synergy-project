#!/bin/bash
# Linux / macOS:  ./run.sh   (needs JDK 17+)
mkdir -p out
javac -d out $(find src -name "*.java") && java -cp out com.autosorter.Main "${1:-8080}"
