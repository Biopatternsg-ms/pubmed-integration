package com.biopatternsg.domain.model;

import java.util.List;

public record BiologicalObject(
        String name,
        List<String> synonyms
) {
}
