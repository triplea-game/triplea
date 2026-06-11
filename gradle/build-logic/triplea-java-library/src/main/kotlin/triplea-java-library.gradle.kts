/*
  This convention defines a standard TripleA java library project.
  It applies the `java-library` plugin and applies universal configuration, code conventions, and sets up static analysis.
*/

plugins {
    `java-library`
    id("com.diffplug.spotless")
    id("triplea-base-project")
}

group = "triplea"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

spotless {
    format("allFiles") {
        target("*")
        targetExclude("gradlew.bat")
        endWithNewline()
        leadingTabsToSpaces()
        trimTrailingWhitespace()
    }

    java {
        googleJavaFormat()
        expandWildcardImports()
        removeUnusedImports()
        cleanthat()
            .sourceCompatibility("21")
            // https://jsparrow.github.io/rules/#tags
            // https://github.com/solven-eu/cleanthat/tree/master/java/src/main/java/eu/solven/cleanthat/engine/java/refactorer/mutators
            .addMutator("ArithmethicAssignment")
            .addMutator("ArraysDotStream")
            .addMutator("AvoidFileStream")
            .addMutator("AvoidMultipleUnaryOperators")
            .addMutator("AvoidUncheckedExceptionsInSignatures")
            .addMutator("CollectionIndexOfToContains")
            .addMutator("ComparisonWithNaN")
            .addMutator("CreateTempFilesUsingNio")
            .addMutator("EmptyControlStatement")
            .addMutator("EnumsWithoutEquals")
            .addMutator("GuavaInlineStringsRepeat")
            .addMutator("LambdaIsMethodReference")
            .addMutator("LambdaReturnsSingleStatement")
            .addMutator("ModifierOrder")
            .addMutator("ObjectEqualsForPrimitives")
            .addMutator("ObjectsHashCodePrimitive")
            .addMutator("OptionalNotEmpty")
            .addMutator("PrimitiveWrapperInstantiation")
            .addMutator("RedundantLogicalComplementsInStream")
            .addMutator("RemoveAllToClearCollection")
            .addMutator("RemoveExplicitCallToSuper")
            .addMutator("SimplifyBooleanExpression")
            .addMutator("SimplifyBooleanInitialization")
            .addMutator("SimplifyStartsWith")
            .addMutator("StreamAnyMatch")
            .addMutator("StreamWrappedIfToFilter")
            .addMutator("StringIndexOfToContains")
            .addMutator("StringReplaceAllWithQuotableInput")
            .addMutator("StringToString")
            .addMutator("ThreadRunToThreadStart")
            .addMutator("UnnecessaryBoxing")
            .addMutator("UnnecessaryFullyQualifiedName")
            .addMutator("UnnecessaryLambdaEnclosingParameters")
            .addMutator("UnnecessaryModifier")
            .addMutator("UseCollectionIsEmpty")
            .addMutator("UseIndexOfChar")
            .addMutator("UseStringIsEmpty")
            .addMutator("UseTextBlocks")
            .addMutator("UseUnderscoresInNumericLiterals")
    }
}
