package org.garrit.judge;

import java.util.HashMap;
import java.util.Scanner;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.JudgementCase;

/**
 * The line judge performs a line-by-line comparison between the problem case
 * and execution output, interpreting the bytes as text. A line-by-line
 * judgement is preferable to byte-by-byte judgement in situations where line
 * endings are to be considered irrelevant.
 * 
 * The marked value for each case is boolean: 1/1 if all lines match, 0/1 if
 * they don't.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class LineJudge extends Judge
{
    private HashMap<String, ExecutionCase> executionCases = new HashMap<>();

    public LineJudge(Execution execution)
    {
        super(execution);

        for (ExecutionCase executionCase : execution.getCases())
            this.executionCases.put(executionCase.getName(), executionCase);
    }

    @Override
    public JudgementCase evaluate(ProblemCase problemCase)
    {
        ExecutionCase executionCase = this.executionCases.get(problemCase.getName());

        if (executionCase == null)
        {
            JudgementCase judgementCase = new JudgementCase();
            judgementCase.setName(problemCase.getName());
            judgementCase.setErrorOccurred(true);
            judgementCase.setError("No execution for case");
            judgementCase.setValue(0);
            return judgementCase;
        }

        JudgementCase judgementCase = new JudgementCase(executionCase);
        judgementCase.setValue(1);
        judgementCase.setValueMin(0);
        judgementCase.setValueMax(1);

        try (
                Scanner problemScanner = new Scanner(new String(problemCase.getOutput()));
                Scanner executionScanner = new Scanner(new String(executionCase.getOutput())))
        {
            while (problemScanner.hasNextLine() && executionScanner.hasNextLine())
            {
                if (!problemScanner.nextLine().equals(executionScanner.nextLine()))
                {
                    judgementCase.setValue(0);
                    return judgementCase;
                }
            }

            /* If one output has fewer lines than the other... */
            if (problemScanner.hasNextLine() != executionScanner.hasNextLine())
            {
                judgementCase.setValue(0);
                return judgementCase;
            }
        }

        return judgementCase;
    }
}