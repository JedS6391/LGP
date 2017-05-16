package lgp.core.environment.dataset

import com.opencsv.CSVReader
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation
import java.io.FileReader
import java.io.Reader

typealias Header = Array<String>
typealias Row = Array<String>

/**
 * Loads a collection of samples and their target values from a CSV file.
 *
 * @param T Type of the features in the samples.
 * @property filename CSV file to load samples from.
 * @property parseFunction Function to parse each attribute in the file.
 */
class CsvDatasetLoader<TInput, TOutput> constructor(
        val reader: Reader,
        val featureParseFunction: (Header, Row) -> Sample<TInput>,
        val targetParseFunction: (Header, Row) -> TOutput
) : DatasetLoader<TInput, TOutput> {

    private constructor(builder: Builder<TInput, TOutput>)
            : this(builder.reader, builder.featureParseFunction, builder.targetParseFunction)

    /**
     * Builds an instance of [CsvDatasetLoader].
     *
     * @param U the type that the [CsvDatasetLoader] will load features as.
     */
    class Builder<U, V> : ComponentLoaderBuilder<CsvDatasetLoader<U, V>> {

        lateinit var reader: Reader
        lateinit var featureParseFunction: (Header, Row) -> Sample<U>
        lateinit var targetParseFunction: (Header, Row) -> V

        /**
         * Sets the filename of the CSV file to load the data set from.
         */
        fun filename(name: String): Builder<U, V> {
            this.reader = FileReader(name)

            return this
        }

        fun reader(reader: Reader): Builder<U, V> {
            this.reader = reader

            return this
        }

        /**
         * Sets the function to use when parsing features from the data set file.
         */
        fun featureParseFunction(function: (Header, Row) -> Sample<U>): Builder<U, V> {
            this.featureParseFunction = function

            return this
        }

        fun targetParseFunction(function: (Header, Row) -> V): Builder<U, V> {
            this.targetParseFunction = function

            return this
        }

        /**
         * Builds the instance with the given configuration information.
         */
        override fun build(): CsvDatasetLoader<U, V> {
            return CsvDatasetLoader(this)
        }
    }

    /**
     * Loads a data set from the CSV file specified when the loader was built.
     *
     * @throws [java.io.IOException] when the file given does not exist.
     * @returns a data set containing values parsed appropriately.
     */
    override fun load(): Dataset<TInput, TOutput> {
        val reader: CSVReader = CSVReader(this.reader)
        val lines: MutableList<Array<String>> = reader.readAll()

        // Assumes the header is in the first row (a reasonable assumption with CSV files).
        val header = lines.removeAt(0)

        val features = lines.map { line ->
            this.featureParseFunction(header, line)
        }

        val targets = lines.map { line ->
            this.targetParseFunction(header, line)
        }

        return Dataset(features, targets)
    }

    override val information = ModuleInformation(
        description = "A loader than can load data sets from CSV files."
    )
}
