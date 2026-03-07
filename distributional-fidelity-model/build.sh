#!/bin/bash

# Build script for Distributional Fidelity Model

echo "========================================="
echo "Distributional Fidelity Model Build Script"
echo "========================================="
echo ""

# Create necessary directories
mkdir -p bin
mkdir -p lib

echo "Creating directory structure... [OK]"

# Check if Java is installed
if ! command -v javac &> /dev/null; then
    echo "Error: javac not found. Please install Java JDK."
    exit 1
fi

echo "Checking Java installation... [OK]"

# Compile all Java files
echo ""
echo "Compiling Java source files..."
javac -d bin -sourcepath src/main/java src/main/java/com/simulation/distributional/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful... [OK]"
else
    echo "Compilation failed. Please check for errors."
    exit 1
fi

# Create a JAR file
echo ""
echo "Creating JAR file..."
jar cf distributional-fidelity-model.jar -C bin .

if [ $? -eq 0 ]; then
    echo "JAR file created... [OK]"
else
    echo "JAR file creation failed."
    exit 1
fi

echo ""
echo "========================================="
echo "Build completed successfully!"
echo "========================================="
echo ""
echo "To run the simulation:"
echo "  java -cp bin com.simulation.distributional.DistributionalFidelityModel"
echo ""
echo "Or run from JAR:"
echo "  java -jar distributional-fidelity-model.jar"
echo ""
