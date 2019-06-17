package nz.co.jedsimson.lgp.test.environment

import nz.co.jedsimson.lgp.core.environment.ComponentLoadException
import nz.co.jedsimson.lgp.core.environment.dataset.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.gherkin.Feature
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader

object DatasetLoaderFeature : Spek({
    val testDatasetFilePath = this.javaClass.classLoader.getResource("test-dataset.csv").file
    val testInvalidDatasetFilePath = this.javaClass.classLoader.getResource("test-dataset-invalid1.csv").file

    Feature("Loading a dataset from a CSV file") {
        Scenario("Load a dataset of double values from a valid file") {
            lateinit var datasetLoader: CsvDatasetLoader<Double>
            var dataset: Dataset<Double>? = null

            Given("A CsvDatasetLoader for the file test-dataset.csv") {
                val reader = BufferedReader(InputStreamReader(FileInputStream(testDatasetFilePath)))
                val featureIndices = 0 until 2
                val targetIndex = 2

                datasetLoader = CsvDatasetLoader(
                    reader = reader,
                    featureParseFunction = ParsingFunctions.indexedDoubleFeatureParsingFunction(featureIndices),
                    targetParseFunction = ParsingFunctions.indexedDoubleSingleTargetParsingFunction(targetIndex)
                )
            }

            When("The dataset is loaded") {
                dataset = datasetLoader.load()
            }

            Then("The dataset should be loaded correctly") {
                assert(dataset != null) { "Dataset was null" }
                assert(dataset!!.numFeatures() == 2) { "Number of features did not match expected" }
                assert(dataset!!.numSamples() == 3) { "Number of samples did not match expected" }

                // The dataset starts at 1.0 and increments by 1.0
                val expectedFeatures = listOf(
                    Pair(1.0, 1.0),
                    Pair(2.0, 2.0),
                    Pair(3.0, 3.0)
                )

                expectedFeatures.zip(dataset!!.inputs).forEach { (expected, sample) ->
                    assert(sample.features.size == 2) { "Sample did not have correct number of features" }
                    assert(sample.feature("x_0").value == expected.first) { "Feature x_0 did not have the correct value" }
                    assert(sample.feature("x_1").value == expected.second) { "Feature x_1 did not have the correct value" }
                }

                val expectedOutputs = listOf(1.0, 4.0, 27.0)

                expectedOutputs.zip(dataset!!.outputs).forEach { (expected, output) ->
                    assert((output as Targets.Single<Double>).value == expected) { "Output did not have the correct value" }
                }
            }
        }

        Scenario("Load a dataset of double values from an invalid file (i.e no data rows)") {
            lateinit var datasetLoader: CsvDatasetLoader<Double>
            var dataset: Dataset<Double>? = null
            var exception: Exception? = null

            Given("A CsvDatasetLoader for the file test-dataset-invalid1.csv") {
                val reader = BufferedReader(InputStreamReader(FileInputStream(testInvalidDatasetFilePath)))
                val featureIndices = 0 until 2
                val targetIndex = 2

                datasetLoader = CsvDatasetLoader(
                    reader = reader,
                    featureParseFunction = ParsingFunctions.indexedDoubleFeatureParsingFunction(featureIndices),
                    targetParseFunction = ParsingFunctions.indexedDoubleSingleTargetParsingFunction(targetIndex)
                )
            }

            When("The dataset is loaded") {
                try {
                    dataset = datasetLoader.load()
                }
                catch (ex: Exception) {
                    exception = ex
                }
            }

            Then("The dataset should not be loaded correctly") {
                assert(dataset == null) { "Dataset was not null" }
                assert(exception != null) { "Exception is null"}
                assert(exception is ComponentLoadException) { "Exception is not of correct type" }
                assert(exception!!.cause is InvalidCsvFileException) { "Inner exception is not of correct type "}
            }
        }
    }
})