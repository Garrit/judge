package org.garrit.judge;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.garrit.common.ProblemCase;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.ExecutionCase;
import org.garrit.common.messages.JudgementCase;
import org.junit.Test;

/**
 * Test the {@link LineJudge line judge}.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class LineJudgeTest
{
    @Test
    public void testCorrectInput() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("correct");
        executionCase.setOutput("line one\nline two".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("correct");
        problemCase.setOutput("line one\nline two".getBytes());

        LineJudge judge = new LineJudge(execution);
        JudgementCase judgeCase = judge.evaluate(problemCase);

        assertEquals(1, judgeCase.getValue());
    }

    @Test
    public void testIgnoresLineEndings() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("endings");
        executionCase.setOutput("line one\nline two".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("endings");
        problemCase.setOutput("line one\r\nline two".getBytes());

        LineJudge judge = new LineJudge(execution);
        JudgementCase judgeCase = judge.evaluate(problemCase);

        assertEquals(1, judgeCase.getValue());
    }

    @Test
    public void testWrongInput() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("wrong");
        executionCase.setOutput("line one\nline owt".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("wrong");
        problemCase.setOutput("line one\nline two".getBytes());

        LineJudge judge = new LineJudge(execution);
        JudgementCase judgeCase = judge.evaluate(problemCase);

        assertEquals(0, judgeCase.getValue());
    }

    @Test
    public void testShortExecution() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("short");
        executionCase.setOutput("line one".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("short");
        problemCase.setOutput("line one\nline two".getBytes());

        LineJudge judge = new LineJudge(execution);
        JudgementCase judgeCase = judge.evaluate(problemCase);

        assertEquals(0, judgeCase.getValue());
    }

    @Test
    public void testShortProblem() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("short");
        executionCase.setOutput("line one\nline two".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("short");
        problemCase.setOutput("line one".getBytes());

        LineJudge judge = new LineJudge(execution);
        JudgementCase judgeCase = judge.evaluate(problemCase);

        assertEquals(0, judgeCase.getValue());
    }

    @Test(expected = IOException.class)
    public void testFailsForMissingCase() throws IOException
    {
        ExecutionCase executionCase = new ExecutionCase();
        executionCase.setName("one");
        executionCase.setOutput("".getBytes());

        Execution execution = new Execution();
        execution.getCases().add(executionCase);

        ProblemCase problemCase = new ProblemCase();
        problemCase.setName("two");
        problemCase.setOutput("".getBytes());

        LineJudge judge = new LineJudge(execution);
        judge.evaluate(problemCase);
    }
}