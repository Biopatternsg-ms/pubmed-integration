package com.biopatternsg.domain.ports.out.repositories;

import com.biopatternsg.mongo.PairsCollection;

public interface PairRepository {
    void save(PairsCollection pairsCollection);
    void deleteByPipelineId(String pipelineId);
}
