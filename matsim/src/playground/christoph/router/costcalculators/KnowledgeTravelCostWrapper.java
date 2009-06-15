package playground.christoph.router.costcalculators;

import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.router.util.TravelCost;
import org.matsim.knowledges.Knowledges;

import playground.christoph.router.util.KnowledgeTools;
import playground.christoph.router.util.KnowledgeTravelCost;

public class KnowledgeTravelCostWrapper extends KnowledgeTravelCost{
	
	protected TravelCost travelCostcalculator;

	private Knowledges knowledges;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelCostWrapper.class);
	
	public KnowledgeTravelCostWrapper(Knowledges knowledges, TravelCost travelCost)
	{
		this.knowledges = knowledges;
		this.travelCostcalculator = travelCost;
	}
	
	public void setTravelCostCalculator(TravelCost travelCost)
	{
		this.travelCostcalculator = travelCost;
	}
	
	public TravelCost getTravelCostCalculator()
	{
		return this.travelCostcalculator;
	}
	
	public double getLinkTravelCost(final Link link, final double time) 
	{
		Map<Id, Node> knownNodesMap = null;
		
		// try getting Nodes from the Persons Knowledge
		knownNodesMap = KnowledgeTools.getKnownNodes(this.knowledges, this.person);
		
		// if the Person doesn't know the link -> return max costs 
		if (!KnowledgeTools.knowsLink(link, knownNodesMap))
		{
//			log.info("Link is not part of the Persons knowledge!");
			return Double.MAX_VALUE;
		}
		else
		{
//			log.info("Link is part of the Persons knowledge!");
			return travelCostcalculator.getLinkTravelCost(link, time);
		}
	}
	
	@Override
	public KnowledgeTravelCostWrapper clone()
	{
		TravelCost travelCostCalculatorClone;
		
		if(this.travelCostcalculator instanceof KnowledgeTravelCost)
		{
			travelCostCalculatorClone = ((KnowledgeTravelCost)this.travelCostcalculator).clone();
		}
		else
		{
			log.error("Could not clone the CostCalculator - use reference to the existing Calculator and hope the best...");
			travelCostCalculatorClone = this.travelCostcalculator;
		}
		
		KnowledgeTravelCostWrapper clone = new KnowledgeTravelCostWrapper(this.knowledges, travelCostCalculatorClone);

		return clone;
	}


}
