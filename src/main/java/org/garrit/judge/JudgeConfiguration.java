package org.garrit.judge;

import io.dropwizard.Configuration;

import java.net.URI;
import java.nio.file.Path;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JudgeConfiguration extends Configuration
{
    private String name;
    private URI negotiator;
    private Path problems;
}