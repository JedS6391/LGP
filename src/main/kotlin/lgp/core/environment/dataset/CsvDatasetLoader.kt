package lgp.core.environment.dataset

import com.opencsv.CSVReader
import lgp.core.environment.ComponentLoaderBuilder
import lgp.core.modules.ModuleInformation
import java.io.FileReader

class Row<out T>(val data: List<Attribute<T>>)

class CsvDataset<out T>(val rows: List<Row<T>>) : Dataset<T>

class CsvDatasetLoader<T> private constructor(val filename: String,
                                              val parseFunction: (String) -> T)
    : DatasetLoader<T> {

    private constructor(builder: Builder<T>) : this(builder.filename, builder.parseFunction)

    class Builder<U> : ComponentLoaderBuilder<CsvDatasetLoader<U>> {

        lateinit var filename: String
        lateinit var parseFunction: (String) -> U

        fun filename(name: String): Builder<U> {
            this.filename = name

            return this
        }

        fun parseFunction(function: (String) -> U): Builder<U> {
            this.parseFunction = function

            return this
        }

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
    override fun load(): CsvDataset<T> {
        val reader: CSVReader = CSVReader(FileReader(this.filename))
        val lines: MutableList<Array<String>> = reader.readAll()

        // Assumes the header is in the first row (a reasonable assumption with CSV files).
        val header = lines.removeAt(0)

        val rows = lines.map { line -> Row(this.readAttributesFromRow(line, header)) }

        return CsvDataset(rows)
    }

    private fun readAttributesFromRow(line: Array<String>, header: Array<String>): List<Attribute<T>> {
        return line.mapIndexed { index, value ->
            Attribute(header[index], this.parseFunction(value))
        }
    }

    override val information: ModuleInformation = object : ModuleInformation {
        override val description: String
            get() = "A loader than can load data sets from CSV files."
    }
}