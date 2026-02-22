package com.biopatternsg.domain.ports.in;

public interface BuildPubmedPairs {
    void execute(String pipelineId, boolean useShortName, int levels);
}
