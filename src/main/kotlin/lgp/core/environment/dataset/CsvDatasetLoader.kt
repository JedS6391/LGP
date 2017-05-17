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
 * @property reader A reader that will provide the contents of a CSV file.
 * @property featureParseFunction A function to parse the features of each row in the CSV file.
 * @property targetParseFunction A function to parse the target of each row in the CSV file.
 */
class CsvDatasetLoader<out T> constructor(
        val reader: Reader,
        val featureParseFunction: (Header, Row) -> Sample<T>,
        val targetParseFunction: (Header, Row) -> T
) : DatasetLoader<T> {

    private constructor(builder: Builder<T>)
            : this(builder.reader, builder.featureParseFunction, builder.targetParseFunction)

    /**
     * Builds an instance of [CsvDatasetLoader].
     *
     * @param U the type that the [CsvDatasetLoader] will load features as.
     */
    class Builder<U> : ComponentLoaderBuilder<CsvDatasetLoader<U>> {

        lateinit var reader: Reader
        lateinit var featureParseFunction: (Header, Row) -> Sample<U>
        lateinit var targetParseFunction: (Header, Row) -> U

        /**
         * Sets the filename of the CSV file to load the data set from.
         *
         * A reader will be automatically created for the file with the name given.
         */
        fun filename(name: String): Builder<U> {
            this.reader = FileReader(name)

            return this
        }

        /**
         * Sets the reader that provides a CSV files contents.
         */
        fun reader(reader: Reader): Builder<U> {
            this.reader = reader

            return this
        }

        /**
         * Sets the function to use when parsing features from the data set file.
         */
        fun featureParseFunction(function: (Header, Row) -> Sample<U>): Builder<U> {
            this.featureParseFunction = function

            return this
        }

        /**
         * Sets the function to use when parsing target values from the data set file.
         */
        fun targetParseFunction(function: (Header, Row) -> U): Builder<U> {
            this.targetParseFunction = function

            return this
        }

        /**
         * Builds the instance with the given configuration information.
         */
        override fun build(): CsvDatasetLoader<U> {
            return CsvDatasetLoader(this)
        }
    }

    /**
     * Loads a data set from the CSV file specified when the loader was built.
     *
     * @throws [java.io.IOException] when the file given does not exist.
     * @returns a data set containing values parsed appropriately.
     */
    override fun load(): Dataset<T> {
        val reader: CSVReader = CSVReader(this.reader)
        val lines: MutableList<Array<String>> = reader.readAll()

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
        description = "A loader than can load data sets from CSV files."
    )
}
