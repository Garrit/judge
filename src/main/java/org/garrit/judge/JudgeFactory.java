package org.garrit.judge;

import org.garrit.common.messages.Execution;

/**
 * Provide access to {@link Judge judges}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class JudgeFactory
{
    /**
     * Get a judge suitable for a given execution.
     * 
     * @param execution the execution
     * @return an instance of a suitable judge
     */
    public static Judge getJudge(Execution execution)
    {
        /* Currently, we only have a line-by-line judge. */
        return new LineJudge(execution);
    }
}