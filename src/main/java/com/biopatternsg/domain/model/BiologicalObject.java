package com.biopatternsg.domain.model;

import java.util.List;

public record BiologicalObject(
        String id,
        String name,
        String symbol,
        List<String> synonyms
) {
}
