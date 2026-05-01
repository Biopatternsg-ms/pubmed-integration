package com.biopatternsg.application.usecase;

import com.biopatternsg.application.services.BiologicalObjectsService;
import com.biopatternsg.domain.model.BiologicalObject;
import com.biopatternsg.domain.ports.in.BuildPubmedPairs;
import com.biopatternsg.domain.ports.out.repositories.PairRepository;
import com.biopatternsg.mongo.PairsCollection;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class BuildPubmedPairsUseCase implements BuildPubmedPairs {

    private final BiologicalObjectsService biologicalObjectsService;
    private final PairRepository pairRepository;
    private static final int FIRST_LEVEL = 1;
    private static final int SECOND_LEVEL = 2;

    @Override
    public void execute(String pipelineId, boolean usePrincipalName, int levels) {

        //Busqueda nivel 1
        List<BiologicalObject> biologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, FIRST_LEVEL);
        Set<PairsGenerator.TermsPair> paresUnicos = getPars(usePrincipalName, biologicalObjects);

        //Save
        PairsCollection toSave = new PairsCollection();
        toSave.setPipelineId(pipelineId);
        toSave.setPairs(
                paresUnicos.stream()
                .map( p -> {

                    PairsCollection.Par par = new PairsCollection.Par();
                    par.setFirstTerm(p.termino1);
                    par.setSecondTerm(p.termino2);

                    return par;
                }).toList());

        pairRepository.save(toSave);

        log.info("Generated unique pairs for first level: {}", paresUnicos.size());

        //--------------------------------------------------------------------

        for (int currentLevel = SECOND_LEVEL; currentLevel <= levels; currentLevel++) {
            List<BiologicalObject> currentLevelBiologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, currentLevel);
            log.info("Biological objects level [{}]: {}", currentLevel, currentLevelBiologicalObjects.size());

            currentLevelBiologicalObjects.forEach(currentBiologicalObject -> {
                List<BiologicalObject> biologicalObjectFamily = biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(pipelineId, currentBiologicalObject.id());
                Set<PairsGenerator.TermsPair> termsPairs = getPairsByLists(usePrincipalName, currentBiologicalObject, biologicalObjectFamily);

                //Save
                PairsCollection toSaveByLevel = new PairsCollection();
                toSaveByLevel.setPipelineId(pipelineId);
                toSaveByLevel.setPairs(
                        termsPairs.stream()
                                .map( p -> {

                                    PairsCollection.Par par = new PairsCollection.Par();
                                    par.setFirstTerm(p.termino1);
                                    par.setSecondTerm(p.termino2);

                                    return par;
                                }).toList());

                pairRepository.save(toSaveByLevel);

                log.info("Generated unique pairs for biologicalObjectId [{}]: {}", currentBiologicalObject.id(), termsPairs.size());
            });

        }

        log.info("Generated unique pairs FINISHED");
    }

    private Set<PairsGenerator.TermsPair> getPars(boolean usePrincipalName, List<BiologicalObject> biologicalObjectFamily) {
        List<List<String>> termListByObject = new ArrayList<>(biologicalObjectFamily.size());

        for (BiologicalObject biologicalObject : biologicalObjectFamily) {
            if (usePrincipalName) {
                biologicalObject.synonyms().clear();
            }
            List<String> currentObjectTerms = new ArrayList<>(biologicalObject.synonyms().size() + 2);
            if (biologicalObject.name() != null) {
                currentObjectTerms.add(biologicalObject.name());
            }
            if (biologicalObject.symbol() != null) {
                currentObjectTerms.add(biologicalObject.symbol());
            }
            currentObjectTerms.addAll(biologicalObject.synonyms().stream().filter(Objects::nonNull).toList());
            termListByObject.add(currentObjectTerms);
        }

        Set<PairsGenerator.TermsPair> uniquePairs = new HashSet<>();

        // 3. GENERACIÓN DE PARES: Combinatoria entre objetos distintos
        int biologicalObjectsSize = termListByObject.size();

        // Bucle para el Objeto A
        for (int i = 0; i < biologicalObjectsSize; i++) {
            List<String> termsObjectA = termListByObject.get(i);

            // Bucle para el Objeto B (empieza en i + 1 para pares 0-1, 0-2, 1-2, etc.)
            for (int j = i + 1; j < biologicalObjectsSize; j++) {
                List<String> termsObjectB = termListByObject.get(j);

                // Producto cartesiano entre los términos del Objeto A y Objeto B
                for (String termA : termsObjectA) {
                    for (String termB : termsObjectB) {
                        if (termA == null || termB == null) {
                            continue;
                        }
                        uniquePairs.add(new PairsGenerator.TermsPair(termA, termB));
                    }
                }
            }
        }

        int termsSize = termListByObject.stream().filter(Objects::nonNull).mapToInt(List::size).sum();

        log.info("Number of objects [{}], number of terms [{}],unique pairs [{}]", biologicalObjectsSize, termsSize,uniquePairs.size());

        return uniquePairs;
    }

    private List<String> getTerms(BiologicalObject biologicalObject, boolean useShortName) {

        if (useShortName) {
            biologicalObject.synonyms().clear();
        }
        List<String> currentObjectTerms = new ArrayList<>();
        if (biologicalObject.name() != null) {
            currentObjectTerms.add(biologicalObject.name());
        }
        if (biologicalObject.symbol() != null) {
            currentObjectTerms.add(biologicalObject.symbol());
        }
        currentObjectTerms.addAll(biologicalObject.synonyms().stream().filter(Objects::nonNull).toList());

        return currentObjectTerms;
    }

    private Set<PairsGenerator.TermsPair> getPairsByLists(boolean useShortName, BiologicalObject currentBiologicalObject, List<BiologicalObject> biologicalObjectFamily){

        List<String> biologicalObjectTerms = getTerms(currentBiologicalObject, useShortName);

        List<String> biologicalObjectFamilyTerms = biologicalObjectFamily.stream()
                .map(bo -> getTerms(bo, useShortName))
                .flatMap(List::stream).toList();


        Set<PairsGenerator.TermsPair> paresUnicos = new HashSet<>();

        for (String s1 : biologicalObjectTerms) {
            for (String s2 : biologicalObjectFamilyTerms) {
                if (s1 == null || s2 == null) {
                    continue;
                }
                paresUnicos.add(new PairsGenerator.TermsPair(s1, s2));
            }
        }

        return paresUnicos;
    }
}
