/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vehicles;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class MatsimVehicleWriterTest extends MatsimTestCase {
	private static final Logger log = Logger.getLogger( MatsimVehicleWriterTest.class ) ;

	private static final String TESTXML  = "testVehicles_v1.xml";

	private final Id<Vehicle> id23 = Id.create("23", Vehicle.class);
	private final Id<Vehicle> id42 = Id.create("42", Vehicle.class);
	private final Id<Vehicle> id42_23 = Id.create(" 42  23", Vehicle.class); //indeed this should be double blank in the middle but due to collapse this is only one blank


	public void testWriter() throws FileNotFoundException, IOException {
		{
			String outfileName = this.getOutputDirectory() + "testOutputVehicles.xml";

			// create empty vehicles container:
			Vehicles vehicles = VehicleUtils.createVehiclesContainer();

			// read, which will be v1:
			MatsimVehicleReader reader = new MatsimVehicleReader( vehicles );
			reader.readFile( this.getPackageInputDirectory() + TESTXML );

			VehicleType vehType = vehicles.getVehicleTypes().get(Id.create("normal&Car", VehicleType.class));

			// write, which will be the newest fmt:
			MatsimVehicleWriter writer = new MatsimVehicleWriter( vehicles );
			writer.writeFile( outfileName );
			assertTrue( new File( outfileName ).exists() );
		}
		{
			// read, which will now be in the newest fmt:
			Vehicles vehicles = VehicleUtils.createVehiclesContainer();
			MatsimVehicleReader reader = new MatsimVehicleReader( vehicles );
			reader.readFile( this.getOutputDirectory() + "testOutputVehicles.xml" );

			VehicleType vehType = vehicles.getVehicleTypes().get(Id.create("normal&Car", VehicleType.class));

			//check it, check it, check it now!
			this.checkContent( vehicles );
		}
	}

	private void checkContent(Vehicles vehdef) {
		Map<Id<VehicleType>, VehicleType> vehicleTypes = vehdef.getVehicleTypes();
		Map<Id<Vehicle>, Vehicle> vehicles = vehdef.getVehicles();

		assertNotNull(vehicleTypes);
		assertEquals(2, vehicleTypes.size());
		VehicleType vehType = vehicleTypes.get(Id.create("normal&Car", VehicleType.class));
		assertNotNull(vehType);
		assertEquals(9.5, vehType.getLength(), EPSILON);
		assertEquals(3.0, vehType.getWidth(), EPSILON);
		assertEquals(42.0, vehType.getMaximumVelocity(), EPSILON);
		assertNotNull(vehType.getCapacity());
		assertEquals(Integer.valueOf(5), vehType.getCapacity().getSeats());
		assertEquals(Integer.valueOf(20), vehType.getCapacity().getStandingRoom());
		assertNotNull(vehType.getCapacity().getFreightCapacity());
		assertEquals(23.23, vehType.getCapacity().getFreightCapacity().getVolume(), EPSILON);
		assertNotNull(vehType.getEngineInformation());
		assertEquals(EngineInformation.FuelType.diesel, vehType.getEngineInformation().getFuelType());
		assertEquals(0.23, VehicleUtils.getFuelConsumption(vehType), EPSILON);
		assertEquals(23.23, VehicleUtils.getAccessTime(vehType), EPSILON);
		assertEquals(42.42, VehicleUtils.getEgressTime(vehType), EPSILON);
		assertEquals(DoorOperationMode.parallel, VehicleUtils.getDoorOperationMode(vehType));
		assertEquals(2.0, vehType.getPcuEquivalents());

		vehType = vehicleTypes.get(Id.create("defaultValue>Car", VehicleType.class));
		assertNotNull(vehType);
		assertEquals(7.5, vehType.getLength(), EPSILON);
		assertEquals(1.0, vehType.getWidth(), EPSILON);
		assertTrue(Double.isInfinite(vehType.getMaximumVelocity()));
		assertNull(vehType.getCapacity());
		assertEquals(DoorOperationMode.serial, VehicleUtils.getDoorOperationMode(vehType));
		assertEquals(1.0, vehType.getPcuEquivalents());

		assertNotNull(vehicles);
		assertEquals(3, vehicles.size());

		assertNotNull(vehicles.get(id23));
		assertEquals(id23, vehicles.get(id23).getId());
		assertEquals(Id.create("normal&Car", VehicleType.class), vehicles.get(id23).getType().getId());

		assertNotNull(vehicles.get(id42));
		assertEquals(id42, vehicles.get(id42).getId());
		assertEquals(Id.create("defaultValue>Car", VehicleType.class), vehicles.get(id42).getType().getId());

		assertNotNull(vehicles.get(id42_23));
		assertEquals(id42_23, vehicles.get(id42_23).getId());


	}

}
