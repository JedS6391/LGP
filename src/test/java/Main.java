import lgp.core.environment.config.Config;
import lgp.core.environment.config.JsonConfigLoader;
import lgp.core.environment.dataset.Attribute;
import lgp.core.environment.dataset.CsvDataset;
import lgp.core.environment.dataset.CsvDatasetLoader;
import lgp.core.environment.dataset.Row;

public class Main {

    public static void main(String[] args) {
        String configFilename = "/Users/jedsimson/Desktop/env.json";
        String datasetFilename = "/Users/jedsimson/Desktop/dataset.csv";

        JsonConfigLoader configLoader = new JsonConfigLoader.Builder()
                                                            .filename(configFilename)
                                                            .build();

        CsvDatasetLoader<Integer> datasetLoader = new CsvDatasetLoader.Builder<Integer>()
                                                                      .filename(datasetFilename)
                                                                      .parseFunction(Integer::parseInt)
                                                                      .build();

        Config config = configLoader.load();
        CsvDataset<Integer> dataset = datasetLoader.load();

        for (Row<Integer> row : dataset.getRows()) {
            for (Attribute<Integer> attribute : row.getData()) {
                System.out.println(attribute.getName() + " : " + attribute.getValue());
            }
        }

        System.out.println(config.getRegisterType());
    }
}
