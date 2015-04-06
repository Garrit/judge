package org.garrit.judge;

import java.io.IOException;

import lombok.Getter;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.JudgementCase;

/**
 * A judge compares the output of each {@link ExecutionCase case} of an
 * {@link Execution execution} to the expected output given in the
 * {@link ProblemCase problem case}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public abstract class Judge
{
    @Getter
    private final Execution execution;

    /**
     * Set up the judge for a given execution.
     * 
     * @param execution
     */
    public Judge(Execution execution)
    {
        this.execution = execution;
    }

    /**
     * Evaluate the execution for a given problem case.
     * 
     * @param problemCase the problem case
     * @return the result of problem judgement
     * @throws IOException if an error occurs while judging
     */
    public abstract JudgementCase evaluate(ProblemCase problemCase) throws IOException;
}