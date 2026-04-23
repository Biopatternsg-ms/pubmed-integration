package com.biopatternsg.infrastructure.adapters.out;

import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class PairRepositoryImpl implements PairRepository, PanacheMongoRepository<PairsCollection> {
    @Override
    public void save(PairsCollection pairsCollection) {
        persistOrUpdate(pairsCollection);
    }
}
