plugins {
    jacoco
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform() {}
    testLogging {
        setExceptionFormat("full")
        events("skipped", "failed")
    }

    val outputByTest = mutableMapOf<String, StringBuilder>()

    addTestOutputListener { descriptor, event ->
        val key = "${descriptor.className}.${descriptor.name}"
        outputByTest.getOrPut(key) { StringBuilder() }.append(event.message)
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) {}
        override fun afterSuite(suite: TestDescriptor, result: TestResult) {}
        override fun beforeTest(testDescriptor: TestDescriptor) {}

        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
            val key = "${testDescriptor.className}.${testDescriptor.name}"
            val output = outputByTest.remove(key)
            if (result.resultType == TestResult.ResultType.FAILURE && !output.isNullOrEmpty()) {
                println("\n-- Output for ${testDescriptor.displayName} --\n$output")
            }
        }
    })
}

tasks.withType<JacocoReport>().configureEach {
    reports {
        xml.required = true
        xml.outputLocation = project.layout.buildDirectory.file("jacoco.xml")
        html.required = true
    }
}
