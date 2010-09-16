/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;

import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.features.refQueueNetworkFeature.RefQueueNetworkFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.CarDepartureHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanSimulationImpl;
import playground.mrieser.core.mobsim.impl.TeleportationHandler;

public class RefSimFactory implements MobsimFactory {

	@Override
	public Simulation createMobsim(final Scenario scenario, final EventsManager eventsManager) {

		PlanSimulationImpl planSim = new PlanSimulationImpl(scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
		planSim.setSimEngine(engine);

		// setup network
		RefQueueNetworkFeature netFeature = new RefQueueNetworkFeature(scenario.getNetwork(), engine);
//		SimNetwork simNetwork = netFeature.getSimNetwork();

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		// setup DepartureHandlers
		CarDepartureHandler carHandler = new CarDepartureHandler(engine, netFeature, scenario);
		carHandler.setTeleportVehicles(true);
		lh.setDepartureHandler(TransportMode.car, carHandler);
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		lh.setDepartureHandler(TransportMode.pt, teleporter);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);
		lh.setDepartureHandler(TransportMode.ride, teleporter);

		// register all features at the end in the right order
		planSim.addSimFeature(new StatusFeature());
		planSim.addSimFeature(teleporter); // how should a user know teleporter is a simfeature?
		planSim.addSimFeature(ah); // how should a user know ah is a simfeature, bug lh not?
		planSim.addSimFeature(netFeature); // order of features is important!

		return planSim;
	}

}
