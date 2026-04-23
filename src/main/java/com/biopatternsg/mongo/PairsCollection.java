package com.biopatternsg.mongo;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@MongoEntity(collection = "pairs")
public class PairsCollection extends PanacheMongoEntity {
    private String pipelineId;
    private List<Par> pairs;

    @Setter
    @Getter
    public static class Par {
        private String firstTerm;
        private String secondTerm;
    }

}
