MAKEFLAGS += --always-make
SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

setup: ## Install pre-commit hooks
	pre-commit install --hook-type pre-push

format: ## Apply code formatting
	./gradlew spotlessApply

