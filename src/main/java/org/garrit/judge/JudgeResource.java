package org.garrit.judge;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.garrit.common.messages.Execution;

/**
 * Expose judge functionality via HTTP.
 *
 * @author Samuel Coleman <samuel@seenet.ca>
 * @since 1.0.0
 */
@Path("/execute")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JudgeResource
{
    private final JudgementManager manager;

    public JudgeResource(JudgementManager manager)
    {
        this.manager = manager;
    }

    @POST
    public Response executeSubmission(Execution execution)
    {
        this.manager.enqueue(execution);

        return Response.status(Status.ACCEPTED).build();
    }
}