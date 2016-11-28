/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.agarwalamit.opdyts;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionFromPlansAnalyzer;
import playground.agarwalamit.analysis.legMode.distributions.LegModeBeelineDistanceDistributionHandler;
import playground.agarwalamit.analysis.modalShare.FilteredModalShareEventHandler;
import playground.agarwalamit.opdyts.patna.PatnaCMPDistanceDistribution;

/**
 * Created by amit on 20/09/16.
 */

public class OpdytsModalStatsControlerListner implements StartupListener, IterationStartsListener, IterationEndsListener, ShutdownListener {

    @Inject
    private EventsManager events;

    private final FilteredModalShareEventHandler modalShareEventHandler = new FilteredModalShareEventHandler();
    private final PatnaCMPDistanceDistribution referenceStudyDistri ;
    private LegModeBeelineDistanceDistributionHandler beelineDistanceDistributionHandler;

    private final SortedMap<String, SortedMap<Double, Integer>> initialMode2DistanceClass2LegCount = new TreeMap<>();

    private BufferedWriter writer;
    private final Set<String> mode2consider;
    private final OpdytsScenarios opdytsScenarios ;

    public OpdytsModalStatsControlerListner(final Set<String> modes2consider, final OpdytsScenarios opdytsScenarios) {
        this.mode2consider = modes2consider;
        this.opdytsScenarios = opdytsScenarios;

        this.referenceStudyDistri = new PatnaCMPDistanceDistribution(this.opdytsScenarios);
    }

    public OpdytsModalStatsControlerListner() {
        this(new HashSet<>(Arrays.asList(TransportMode.car, TransportMode.pt)), null);
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/modalStats.txt";
        writer = IOUtils.getBufferedWriter(outFile);
        try {
            writer.write("iterationNr" + "\t" +
                    "modes" + "\t" +
                    "numberOfLegs" + "\t" +
                    "modes" + "\t" +
                    "ascs" + "\t" +
                    "util_trav" + "\t" +
                    "util_dist" + "\t" +
                    "money_dist_rate" + "\t" +
                    "objectiveFunctionValue");
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        this.events.addHandler(modalShareEventHandler);

        // initializing it here so that scenario can be accessed.
        List<Double> dists = Arrays.stream(this.referenceStudyDistri.getDistClasses()).boxed().collect(Collectors.toList());
        this.beelineDistanceDistributionHandler = new LegModeBeelineDistanceDistributionHandler(dists, event.getServices().getScenario().getNetwork());
        this.events.addHandler(this.beelineDistanceDistributionHandler);

        // this must be processed here, so that the output remains unchanged.
//        probably, just take this from first iteration, then just use the same beelineDistanceDistributionHandler.
        LegModeBeelineDistanceDistributionFromPlansAnalyzer initialBeelineDistribution = new LegModeBeelineDistanceDistributionFromPlansAnalyzer(dists);
        initialBeelineDistribution.init(event.getServices().getScenario());
        initialBeelineDistribution.preProcessData();
        initialBeelineDistribution.postProcessData();
        this.initialMode2DistanceClass2LegCount.clear();
        this.initialMode2DistanceClass2LegCount.putAll(initialBeelineDistribution.getMode2DistanceClass2LegCount());
    }

    @Override
    public void notifyIterationStarts(final IterationStartsEvent event) {
        this.beelineDistanceDistributionHandler.reset(event.getIteration());
        this.modalShareEventHandler.reset(event.getIteration());
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        SortedMap<String, Integer> mode2Legs = modalShareEventHandler.getMode2numberOflegs();

        // evaluate objective function value
        ObjectiveFunctionEvaluator objectiveFunctionEvaluator = new ObjectiveFunctionEvaluator();
        Map<String, double []> realCounts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
        Map<String, double []> simCounts = new TreeMap<>();

        SortedMap<String, SortedMap<Double, Integer>> simCountsHandler = this.beelineDistanceDistributionHandler.getMode2DistanceClass2LegCounts();
        for (Map.Entry<String, SortedMap<Double, Integer>> e : simCountsHandler.entrySet() ) {
            double [] counts = new double [simCountsHandler.get(e.getKey()).size()] ;
            int index = 0;
            for (Integer count : simCountsHandler.get(e.getKey()).values()) {
                counts [index++] = count;
            }
            simCounts.put(e.getKey(),counts);
        }

        try {
            writer.write(event.getIteration() + "\t" +
                    mode2Legs.keySet().toString() + "\t" +
                    mode2Legs.values().toString() + "\t");

            // write modalParams
            Map<String, PlanCalcScoreConfigGroup.ModeParams> mode2Params = event.getServices().getConfig().planCalcScore().getModes();

            SortedMap<String, Double> ascs = new TreeMap<>();
            SortedMap<String, Double> travs = new TreeMap<>();
            SortedMap<String, Double> dists = new TreeMap<>();
            SortedMap<String, Double> moneyRates = new TreeMap<>();

            for (String mode : mode2Params.keySet()) {
                if (mode2consider.contains(mode)) {
                    ascs.put(mode, mode2Params.get(mode).getConstant());
                    travs.put(mode, mode2Params.get(mode).getMarginalUtilityOfTraveling());
                    dists.put(mode, mode2Params.get(mode).getMarginalUtilityOfDistance());
                    moneyRates.put(mode, mode2Params.get(mode).getMonetaryDistanceRate());
                }
            }

            writer.write(ascs.keySet().toString() + "\t" +
                    ascs.values().toString() + "\t" +
                    travs.values().toString() + "\t" +
                    dists.values().toString() + "\t" +
                    moneyRates.values().toString());

            writer.write( "\t" + objectiveFunctionEvaluator.getObjectiveFunctionValue(realCounts,simCounts));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }

        // dist-distribution file
        String outFile = event.getServices().getConfig().controler().getOutputDirectory() + "/ITERS/it."+event.getIteration()+"/"+event.getIteration()+".distanceDistri.txt";
        writeDistanceDistribution(outFile);
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("File not found.");
        }
    }

    private void writeDistanceDistribution(String outputFile) {
        final BufferedWriter writer2 = IOUtils.getBufferedWriter(outputFile);
        try {
            writer2.write( "distBins" + "\t" );
            for(Double d : referenceStudyDistri.getDistClasses()) {
                writer2.write(d + "\t");
            }
            writer2.newLine();

            // from initial plans
            {
                writer2.write("===== begin writing distribution from initial plans ===== ");
                writer2.newLine();

                for (String mode : this.initialMode2DistanceClass2LegCount.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : this.initialMode2DistanceClass2LegCount.get(mode).keySet()) {
                        writer2.write(this.initialMode2DistanceClass2LegCount.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from initial plans ===== ");
                writer2.newLine();
            }

            // from objective function
            {
                writer2.write("===== begin writing distribution from objective function ===== ");
                writer2.newLine();

                SortedMap<String, double []> mode2counts = this.referenceStudyDistri.getMode2DistanceBasedLegs();
                for (String mode : mode2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2counts.get(mode)) {
                        writer2.write(d + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from objective function ===== ");
                writer2.newLine();
            }

            // from simulation
            {
                writer2.write("===== begin writing distribution from simulation ===== ");
                writer2.newLine();

                SortedMap<String, SortedMap<Double, Integer>> mode2dist2counts = this.beelineDistanceDistributionHandler.getMode2DistanceClass2LegCounts();
                for (String mode : mode2dist2counts.keySet()) {
                    writer2.write(mode + "\t");
                    for (Double d : mode2dist2counts.get(mode).keySet()) {
                        writer2.write(mode2dist2counts.get(mode).get(d) + "\t");
                    }
                    writer2.newLine();
                }

                writer2.write("===== end writing distribution from simulation ===== ");
                writer2.newLine();
            }
            writer2.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }
}