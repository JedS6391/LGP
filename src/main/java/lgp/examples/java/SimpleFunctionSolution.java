package lgp.examples.java;

import lgp.core.evolution.Solution;
import lgp.core.evolution.TrainingResult;
import org.jetbrains.annotations.NotNull;

/**
 * A re-implementation of {@link lgp.examples.SimpleFunctionSolution} to showcase Java interoperability.
 */
public class SimpleFunctionSolution implements Solution<Double> {

    private String problem;
    private TrainingResult<Double> result;

    SimpleFunctionSolution(String problem, TrainingResult<Double> result) {
        this.problem = problem;
        this.result = result;
    }

    @NotNull
    @Override
    public String getProblem() {
        return this.problem;
    }

    public TrainingResult<Double> getResult() {
        return this.result;
    }
}
