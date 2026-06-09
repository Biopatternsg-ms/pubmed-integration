package com.biopatternsg.infrastructure.adapters.out.external_repositories;

import com.biopatternsg.domain.model.NcbiSearchResult;
import com.biopatternsg.domain.ports.out.external_repositories.NcbiSearchRepoWeb;
import com.biopatternsg.infrastructure.external_services.QueryNcbiESearch;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NcbiSearchAdapter implements NcbiSearchRepoWeb {

    private final QueryNcbiESearch queryNcbiESearch;

    public NcbiSearchAdapter(QueryNcbiESearch queryNcbiESearch) {
        this.queryNcbiESearch = queryNcbiESearch;
    }

    @Override
    public NcbiSearchResult search(String term, int retmax) {
        return queryNcbiESearch.search(term, retmax);
    }
}
