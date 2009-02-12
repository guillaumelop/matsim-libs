/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import java.util.HashMap;
import java.util.List;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.Route;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

/**
 * Abstract class for <code>LegTravelTimeEstimator</code>s
 * that estimate the travel time of a fixed route.
 *
 * @author meisterk
 *
 */
public class FixedRouteLegTravelTimeEstimator implements LegTravelTimeEstimator {

	protected TravelTime linkTravelTimeEstimator;
	protected TravelCost linkTravelCostEstimator;
	protected DepartureDelayAverageCalculator tDepDelayCalc;
	private PlansCalcRoute plansCalcRoute;

	private HashMap<Route, List<Link>> linkRoutesCache = new HashMap<Route, List<Link>>();
	
	public FixedRouteLegTravelTimeEstimator(
			TravelTime linkTravelTimeEstimator,
			TravelCost linkTravelCostEstimator,
			DepartureDelayAverageCalculator depDelayCalc,
			final NetworkLayer network) {

		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;
		this.plansCalcRoute = new PlansCalcRoute(network, linkTravelCostEstimator, linkTravelTimeEstimator);

	}

	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			Act actOrigin, Act actDestination, Leg legIntermediate) {
		
			return this.plansCalcRoute.handleLeg(legIntermediate, actOrigin, actDestination, departureTime);
		
	}

	protected double processDeparture(final Link link, final double start) {

		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(link, start);
		return departureDelayEnd;

	}

	protected double processRouteTravelTime(final CarRoute route, final double start) {

		double now = start;
		
		List<Link> links = null;
		if (this.linkRoutesCache.containsKey(route)) {
			links = this.linkRoutesCache.get(route);
		} else {
			links = route.getLinks();
			this.linkRoutesCache.put(route, links);
		}
		
		for (Link link : links) {
			now = this.processLink(link, now);
		}
		return now;

	}

	protected double processLink(final Link link, final double start) {

		double linkEnd = start + this.linkTravelTimeEstimator.getLinkTravelTime(link, start);
		return linkEnd;

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public void reset() {
		this.linkRoutesCache.clear();
	}
	
}
