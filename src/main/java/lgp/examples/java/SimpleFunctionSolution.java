package lgp.examples.java;

import lgp.core.evolution.Solution;
import lgp.core.evolution.fitness.Outputs;
import lgp.core.evolution.training.TrainingResult;
import org.jetbrains.annotations.NotNull;

/**
 * A re-implementation of {@link lgp.examples.SimpleFunctionSolution} to showcase Java interoperability.
 */
public class SimpleFunctionSolution implements Solution<Double> {

    private String problem;
    private TrainingResult<Double, Outputs.Single<Double>> result;

    SimpleFunctionSolution(String problem, TrainingResult<Double, Outputs.Single<Double>> result) {
        this.problem = problem;
        this.result = result;
    }

    @NotNull
    @Override
    public String getProblem() {
        return this.problem;
    }

    public TrainingResult<Double, Outputs.Single<Double>> getResult() {
        return this.result;
    }
}
