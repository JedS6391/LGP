package lgp.examples.java;

import lgp.core.evolution.Problem;
import lgp.core.evolution.fitness.Outputs;
import lgp.core.evolution.model.EvolutionResult;
import lgp.lib.BaseProgram;
import lgp.lib.BaseProgramSimplifier;

import java.util.Map;

/**
 * A re-implementation of {@link lgp.examples.SimpleFunction} to showcase Java interoperability.
 */
public class SimpleFunction {

    public static void main(String[] args) {
        Problem<Double, Outputs.Single<Double>> problem = new SimpleFunctionProblem();

        problem.initialiseEnvironment();
        problem.initialiseModel();

        SimpleFunctionSolution solution = (SimpleFunctionSolution) problem.solve();
        BaseProgramSimplifier<Double, Outputs.Single<Double>> simplifier = new BaseProgramSimplifier<>();

        System.out.println("Results:");
        int run = 0;
        for (EvolutionResult<Double, Outputs.Single<Double>> res : solution.getResult().getEvaluations()) {

            System.out.println("Run " + (run++ + 1) + " (best fitness = " + res.getBest().getFitness() + ")");
            System.out.println(simplifier.simplify((BaseProgram<Double, Outputs.Single<Double>>) res.getBest()));

            System.out.println("\nStats (last run only):\n");

            int last = res.getStatistics().size() - 1;

            for (Map.Entry<String, Object> datum : res.getStatistics().get(last).getData().entrySet()) {
                System.out.println(datum.getKey() + " = " + datum.getValue());
            }

            System.out.println();
        }

        double sum = 0.0;

        for (EvolutionResult<Double, Outputs.Single<Double>> res : solution.getResult().getEvaluations()) {
            sum += res.getBest().getFitness();
        }

        double averageBestFitness = sum / ((double) solution.getResult().getEvaluations().size());

        System.out.println("Average best fitness: " + averageBestFitness);
    }
}