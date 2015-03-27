package org.garrit.judge;

import io.dropwizard.Application;
import io.dropwizard.setup.Environment;

import org.garrit.common.messages.statuses.Status;

/**
 * Main entry point for the judge service.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
public class JudgeApplication extends Application<JudgeConfiguration>
{
    private JudgementManager judge;
    private Status status;

    public static void main(String[] args) throws Exception
    {
        new JudgeApplication().run(args);
    }

    @Override
    public String getName()
    {
        return JudgeApplication.class.getName();
    }

    @Override
    public void run(JudgeConfiguration config, Environment env) throws Exception
    {
        this.judge = new JudgementManager(config.getProblems());

        this.status = new Status(config.getName());
        this.status.setCapabilityStatus(this.judge);

        final StatusResource statusResource = new StatusResource(this.status);

        env.jersey().register(statusResource);

        final StatusHealthCheck statusHealthCheck = new StatusHealthCheck(status);

        env.healthChecks().register("status", statusHealthCheck);

        this.judge.start();
    }
}