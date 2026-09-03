set shell := ["bash", "-eu", "-o", "pipefail", "-c"]

# Show this help message
help:
    @just --list

# Install pre-commit hooks
setup:
    pre-commit install --hook-type pre-push

# Apply code formatting
format:
    ./gradlew spotlessApply
