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
    public void execute(String pipelineId, boolean useShortName, int levels) {

        //Busqueda nivel 1
        List<BiologicalObject> biologicalObjects = biologicalObjectsService.getBiologicalObjectsByLevel(pipelineId, FIRST_LEVEL);
        Set<GeneradorPares.Par> paresUnicos = getPars(useShortName, biologicalObjects);

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

            currentLevelBiologicalObjects.stream().parallel().forEach(currentBiologicalObject -> {
                List<BiologicalObject> biologicalObjectFamily = biologicalObjectsService.getBiologicalObjectFatherBrothersAndSons(pipelineId, currentBiologicalObject.id());
                Set<GeneradorPares.Par> pars = getPars(useShortName, biologicalObjectFamily);

                //Save
                PairsCollection toSaveByLevel = new PairsCollection();
                toSaveByLevel.setPipelineId(pipelineId);
                toSaveByLevel.setPairs(
                        pars.stream()
                                .map( p -> {

                                    PairsCollection.Par par = new PairsCollection.Par();
                                    par.setFirstTerm(p.termino1);
                                    par.setSecondTerm(p.termino2);

                                    return par;
                                }).toList());

                pairRepository.save(toSaveByLevel);

                //------------------------------

                log.info("Generated unique pairs for [{}]: {}", currentBiologicalObject.id(), pars.size());
            });

        }

        log.info("Generated unique pairs FINISHED");
    }

    private Set<GeneradorPares.Par> getPars(boolean useShortName, List<BiologicalObject> biologicalObjectFamily) {
        List<List<String>> termListByObject = new ArrayList<>(biologicalObjectFamily.size());

        for (BiologicalObject biologicalObject : biologicalObjectFamily) {
            if (useShortName) {
                biologicalObject.synonyms().clear();
            }
            List<String> currentObjectTerms = new ArrayList<>(biologicalObject.synonyms().size() + 1);
            if (biologicalObject.name() != null) {
                currentObjectTerms.add(biologicalObject.name());
            }
            currentObjectTerms.addAll(biologicalObject.synonyms().stream().filter(Objects::nonNull).toList());
            termListByObject.add(currentObjectTerms);
        }

        Set<GeneradorPares.Par> uniquePairs = new HashSet<>();

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
                        uniquePairs.add(new GeneradorPares.Par(termA, termB));
                    }
                }
            }
        }

        int termsSize = termListByObject.stream().filter(Objects::nonNull).mapToInt(List::size).sum();

        log.info("Number of objects [{}], number of terms [{}],unique pairs [{}]", biologicalObjectsSize, termsSize,uniquePairs.size());

        return uniquePairs;
    }
}
