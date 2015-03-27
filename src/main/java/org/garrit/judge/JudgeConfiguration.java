package org.garrit.judge;

import io.dropwizard.Configuration;

import java.nio.file.Path;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JudgeConfiguration extends Configuration
{
    private String name;
    private Path problems;
}