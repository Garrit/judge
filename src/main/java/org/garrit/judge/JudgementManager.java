package org.garrit.judge;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClients;
import org.garrit.common.Problem;
import org.garrit.common.ProblemCase;
import org.garrit.common.Problems;
import org.garrit.common.messages.ErrorSubmission;
import org.garrit.common.messages.ErrorType;
import org.garrit.common.messages.Execution;
import org.garrit.common.messages.Judgement;
import org.garrit.common.messages.JudgementCase;
import org.garrit.common.messages.RegisteredSubmission;
import org.garrit.common.messages.statuses.CapabilityType;
import org.garrit.common.messages.statuses.JudgeStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Handle judgement of executions.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Slf4j
public class JudgementManager implements JudgeStatus, Closeable
{
    /**
     * The path containing problem definitions.
     */
    private final Path problems;
    private final JudgementThread judgementThread;
    private final ReportThread reportThread;
    private final ErrorThread errorThread;

    /**
     * Executions lined up and waiting to be judged.
     */
    LinkedBlockingQueue<Execution> submissionQueue = new LinkedBlockingQueue<>();
    /**
     * Submissions which have been judged and need to be sent back to the
     * mediator.
     */
    LinkedBlockingQueue<Judgement> outgoingQueue = new LinkedBlockingQueue<>();
    /**
     * Errors in execution which need to be indicated to the negotiator.
     */
    LinkedBlockingQueue<ErrorSubmission<Execution>> errorQueue = new LinkedBlockingQueue<>();

    public JudgementManager(Path problems, URI negotiator)
    {
        this.problems = problems;
        this.judgementThread = new JudgementThread();
        this.reportThread = new ReportThread(negotiator);
        this.errorThread = new ErrorThread(negotiator);
    }

    /**
     * Enqueue a submission for execution.
     * 
     * @param submission the submission
     */
    public void enqueue(Execution execution)
    {
        this.submissionQueue.add(execution);
    }

    @Override
    public Iterable<String> getLanguages()
    {
        return Arrays.asList("*");
    }

    @Override
    public Iterable<String> getProblems()
    {
        try
        {
            return Problems.availableProblems(this.problems);
        }
        catch (IOException e)
        {
            log.warn("Failure evaluating available problems", e);
            return Arrays.asList();
        }
    }

    @Override
    public ArrayList<Integer> getQueued()
    {
        ArrayList<RegisteredSubmission> frozenQueue = new ArrayList<>(this.submissionQueue);
        ArrayList<Integer> queuedIds = new ArrayList<>(frozenQueue.size());

        frozenQueue.forEach(submission -> queuedIds.add(submission.getId()));

        return queuedIds;
    }

    /**
     * Start processing queued submissions.
     */
    public void start()
    {
        log.info("Starting judgement manager");
        this.judgementThread.start();
        this.reportThread.start();
        this.errorThread.start();
    }

    @Override
    public void close() throws IOException
    {
        log.info("Closing judgement manager");
        this.judgementThread.interrupt();
        this.reportThread.interrupt();
        this.errorThread.interrupt();
    }

    /**
     * Thread to perform the actual executions.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class JudgementThread extends Thread
    {
        public JudgementThread()
        {
            super("Judgement thread");
        }

        @Override
        public void run()
        {
            log.info("Starting judgement thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    Execution execution = JudgementManager.this.submissionQueue.take();

                    Problem problem;
                    Judge judge;

                    /* We may not need to report an error, but here's one
                     * half-constructed and ready to go in the event we do. */
                    ErrorSubmission<Execution> error = new ErrorSubmission<>();
                    error.setId(execution.getId());
                    error.setStage(CapabilityType.JUDGE);
                    error.setSubmission(execution);

                    try
                    {
                        problem = Problems.problemByName(JudgementManager.this.problems, execution.getProblem());
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to retrieve problem definition", e);

                        error.setType(ErrorType.E_INTERNAL);
                        error.setMessage("Failed to retrieve problem definition");
                        JudgementManager.this.errorQueue.offer(error);

                        continue;
                    }

                    judge = JudgeFactory.getJudge(execution);

                    ArrayList<JudgementCase> judgementCases = new ArrayList<>();
                    for (ProblemCase problemCase : problem.getCases())
                    {
                        try
                        {
                            judgementCases.add(judge.evaluate(problemCase));
                        }
                        catch (IOException e)
                        {
                            log.error("Failure while judging case", e);

                            error.setType(ErrorType.E_JUDGING);
                            error.setMessage(e.getMessage());
                            JudgementManager.this.errorQueue.offer(error);

                            continue;
                        }
                    }

                    Judgement judgement = new Judgement(execution);
                    judgement.setCases(judgementCases);

                    JudgementManager.this.outgoingQueue.offer(judgement);
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing judgement thread");
        }
    }

    /**
     * Thread to report back to the negotiator.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ReportThread extends Thread
    {
        private final URI negotiator;

        public ReportThread(URI negotiator)
        {
            super("Negotiator reporting thread");
            this.negotiator = negotiator;
        }

        @Override
        public void run()
        {
            log.info("Starting negotiator reporting thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    Judgement judgement = JudgementManager.this.outgoingQueue.take();

                    ObjectMapper mapper = new ObjectMapper();

                    HttpClient client;
                    HttpPost post;
                    HttpEntity body;

                    client = HttpClients.createDefault();
                    post = new HttpPost(this.negotiator.resolve("report/" + judgement.getId()));
                    try
                    {
                        body = new ByteArrayEntity(mapper.writeValueAsBytes(judgement));
                    }
                    catch (JsonProcessingException e)
                    {
                        log.error("Failed to encode outgoing execution object to JSON", e);
                        continue;
                    }

                    post.setHeader("Content-Type", "application/json");
                    post.setEntity(body);

                    try
                    {
                        client.execute(post);
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to call negotiator with outgoing execution object", e);
                        continue;
                    }
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing negotiator reporting thread");
        }
    }

    /**
     * Thread to send errors back to the negotiator.
     *
     * @author Samuel Coleman <samuel@seenet.ca>
     * @since 1.0.0
     */
    private class ErrorThread extends Thread
    {
        private final URI negotiator;

        public ErrorThread(URI negotiator)
        {
            super("Error reporting thread");
            this.negotiator = negotiator;
        }

        @Override
        public void run()
        {
            log.info("Starting error reporting thread");

            try
            {
                while (true)
                {
                    if (Thread.interrupted())
                        break;

                    ErrorSubmission<Execution> error = JudgementManager.this.errorQueue.take();

                    ObjectMapper mapper = new ObjectMapper();

                    HttpClient client;
                    HttpPost post;
                    HttpEntity body;

                    client = HttpClients.createDefault();
                    post = new HttpPost(this.negotiator.resolve("error/" + error.getId()));
                    try
                    {
                        body = new ByteArrayEntity(mapper.writeValueAsBytes(error));
                    }
                    catch (JsonProcessingException e)
                    {
                        log.error("Failed to encode outgoing error object to JSON", e);
                        continue;
                    }

                    post.setHeader("Content-Type", "application/json");
                    post.setEntity(body);

                    try
                    {
                        client.execute(post);
                    }
                    catch (IOException e)
                    {
                        log.error("Failed to call negotiator with outgoing error object", e);
                        continue;
                    }
                }
            }
            catch (InterruptedException e)
            {
                /* If we've been interrupted, just finish execution. */
            }

            log.info("Finishing error reporting thread");
        }
    }
}