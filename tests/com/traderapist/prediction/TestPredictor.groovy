package com.traderapist.prediction

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/25/13
 * Time: 11:24 AM
 * To change this template use File | Settings | File Templates.
 */
class TestPredictor {
	Predictor predictor

	@Before
	void setUp() {
		predictor = new Predictor()
	}

	@After
	void tearDown() {
		predictor = null
	}

	@Test
	void testCleanData() {
		// Demaryius Thomas not in salaries
		predictor.readSalaries("data/test/DKSalaries.csv")

		// Kevin Boss not in projections
		predictor.readProjections()

		// Make sure DT is in projections and KB is in salaries prior to cleanup
		assert predictor.projections_wr["demaryius thomas"]
		assert predictor.salaries["kevin boss"]

		// As a sanity check, make sure Aaron Rodgers exists in both (he should)
		assert predictor.projections_qb["aaron rodgers"]
		assert predictor.salaries["aaron rodgers"]

		predictor.cleanData()

		assert !predictor.projections_wr["demaryius thomas"]
		assert !predictor.salaries["kevin boss"]

		assert predictor.projections_qb["aaron rodgers"]
		assert predictor.salaries["aaron rodgers"]
	}
}
