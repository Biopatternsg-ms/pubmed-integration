package com.biopatternsg.domain.ports.out.external_repositories;

import com.biopatternsg.domain.model.NcbiSearchResult;

public interface NcbiSearchRepoWeb {
    NcbiSearchResult search(String term, int retmax);
}
