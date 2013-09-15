package com.traderapist.prediction

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 9/5/13
 * Time: 4:23 PM
 * To change this template use File | Settings | File Templates.
 */
class TestMyFantasyAssistantPredictor {
	MyFantasyAssistantPredictor predictor

	@Before
	void setUp() {
		predictor = new MyFantasyAssistantPredictor()
	}

	@After
	void tearDown() {
		predictor = null
	}

	@Test
	void testCleanData_ValuesMissingInSalary() {
		// Set up projections
		predictor.projections["QB"] = ["tom brady":10,"aaron rodgers":15]
		predictor.projections["RB"] = ["adrian peterson":18,"arian foster":15]

		// Set up salaries
		predictor.salaries["tom brady"] = 9800
		predictor.salaries["aaron rodgers"] = 10000
		predictor.salaries["adrian peterson"] = 8000

		predictor.cleanData()

		assert predictor.projections["RB"].size() == 1
		assert predictor.projections["RB"]["arian foster"] == null
	}
}
