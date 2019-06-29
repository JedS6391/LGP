package nz.co.jedsimson.lgp.core.environment.dataset

import com.opencsv.CSVReader
import nz.co.jedsimson.lgp.core.environment.ComponentLoaderBuilder
import nz.co.jedsimson.lgp.core.environment.MemoizedComponentProvider
import java.io.FileReader
import java.io.Reader
import nz.co.jedsimson.lgp.core.modules.ModuleInformation

// These type aliases help to make the code look nicer.
typealias Header = Array<String>
typealias Row = Array<String>

/**
 * Exception given when a CSV file that does not match the criteria the system expects
 * is given to a [CsvDatasetLoader] instance.
 */
class InvalidCsvFileException(message: String) : Exception(message)

/**
 * Loads a collection of samples and their target values from a CSV file.
 *
 * @param TData Type of the features in the samples.
 * @param TTarget Type of the outputs in the dataset.
 * @property reader A reader that will provide the contents of a CSV file.
 * @property featureParseFunction A function to parse the features of each row in the CSV file.
 * @property targetParseFunction A function to parse the target of each row in the CSV file.
 */
class CsvDatasetLoader<TData, TTarget : Target<TData>> constructor(
    val reader: Reader,
    val featureParseFunction: (Header, Row) -> Sample<TData>,
    val targetParseFunction: (Header, Row) -> TTarget
) : DatasetLoader<TData, TTarget> {

    private constructor(builder: Builder<TData, TTarget>)
        : this(builder.reader, builder.featureParseFunction, builder.targetParseFunction)

    private val datasetProvider = MemoizedComponentProvider("Dataset") { this.initialiseDataset() }

    /**
     * Builds an instance of [CsvDatasetLoader].
     *
     * @param UData the type that the [CsvDatasetLoader] will load features as.
     * @param UData the type that the [CsvDatasetLoader] will load features as.
     */
    class Builder<UData, UTarget : Target<UData>> : ComponentLoaderBuilder<CsvDatasetLoader<UData, UTarget>> {

        lateinit var reader: Reader
        lateinit var featureParseFunction: (Header, Row) -> Sample<UData>
        lateinit var targetParseFunction: (Header, Row) -> UTarget

        /**
         * Sets the filename of the CSV file to load the data set from.
         *
         * A reader will be automatically created for the file with the name given.
         */
        fun filename(name: String) = apply {
            this.reader = FileReader(name)
        }

        /**
         * Sets the reader that provides a CSV files contents.
         */
        fun reader(reader: Reader) = apply {
            this.reader = reader
        }

        /**
         * Sets the function to use when parsing features from the data set file.
         */
        fun featureParseFunction(function: (Header, Row) -> Sample<UData>) = apply {
            this.featureParseFunction = function
        }

        /**
         * Sets the function to use when parsing target values from the data set file.
         */
        fun targetParseFunction(function: (Header, Row) -> UTarget) = apply {
            this.targetParseFunction = function
        }

        /**
         * Builds the instance with the given configuration information.
         */
        override fun build(): CsvDatasetLoader<UData, UTarget> {
            return CsvDatasetLoader(this)
        }
    }

    /**
     * Loads a data set from the CSV file specified when the loader was built.
     *
     * @throws [java.io.IOException] when the file given does not exist.
     * @returns a data set containing values parsed appropriately.
     */
    override fun load(): Dataset<TData, TTarget> {
        return this.datasetProvider.component
    }

    private fun initialiseDataset(): Dataset<TData, TTarget> {
        val reader = CSVReader(this.reader)
        val lines: MutableList<Array<String>> = reader.readAll()

        // Make sure there is data before we continue. There should be at least two lines in the file
        // (a header and one row of data). This check will let through a file with 2 data rows, but
        // there is not much that can be done -- plus things will probably break down later on...
        if (lines.size < 2)
            throw InvalidCsvFileException("CSV file should have a header row and one or more data rows.")

        // Assumes the header is in the first row (a reasonable assumption with CSV files).
        val header = lines.removeAt(0)

        // Parse features and target values individually.
        val features = lines.map { line ->
            this.featureParseFunction(header, line)
        }

        val targets = lines.map { line ->
            this.targetParseFunction(header, line)
        }

        return Dataset(features, targets)
    }

    override val information = ModuleInformation(
        description = "A loader that can load data sets from CSV files."
    )
}

/**
 * Provides a collection of parsing functions that can be used by a [CsvDatasetLoader] instance.
 */
object ParsingFunctions {

    /**
     * A feature parsing function that will create a sample of features by mapping each header row
     * to its corresponding data row (using the feature indices provided). The function expects to
     * be creating features with [Double] values.
     *
     * @param featureIndices The indices of any feature variables in the current [Row] being processed.
     * @return A function that creates [Sample] instances from a CSV data row and header.
     */
    fun indexedDoubleFeatureParsingFunction(featureIndices: IntRange): (Header, Row) -> Sample<Double> {
        return { header: Header, row: Row ->
            val features = row.zip(header)
                    .slice(featureIndices)
                    .map { (featureValue, featureName) ->

                        Feature(
                                name = featureName,
                                value = featureValue.toDouble()
                        )
                    }

            Sample(features)
        }
    }

    /**
     * A target parsing function that simply takes a specific value from a [Row] using its index.
     * The function expects to return a single target variable of the [Double] type.
     *
     * @param targetIndex The index of the target variable in the current [Row] being processed.
     * @return A target parsing function that returns the target variable as a [Double] value.
     */
    fun indexedDoubleSingleTargetParsingFunction(targetIndex: Int): (Header, Row) -> Targets.Single<Double> {
        return { _: Header, row: Row ->
            Targets.Single(row[targetIndex].toDouble())
        }
    }

    /**
     * A target parsing function that takes a range of values from a [Row] using a set of indices.
     * The function expects to return target variables of the [Double] type.
     *
     * @param targetIndices The indices of any target variables in the current [Row] being processed.
     * @return A target parsing function that returns the target variables as a collection of [Double] values.
     */
    fun indexedDoubleMultipleTargetParsingFunction(targetIndices: IntRange): (Header, Row) -> Targets.Multiple<Double> {
        return { _: Header, row: Row ->
            Targets.Multiple(
                row.slice(targetIndices).map(String::toDouble)
            )
        }
    }
}
