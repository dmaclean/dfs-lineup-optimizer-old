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
	void testInitializePositionTypes_QB_RB() {
		predictor.projections_qb["Tom Brady"] = 20
		predictor.projections_qb["Aaron Rodgers"] = 25

		predictor.projections_rb["Adrian Peterson"] = 22
		predictor.projections_rb["Doug Martin"] = 18

		predictor.positionTypes = "QB,RB"

		predictor.initializePositionTypes()

		assert predictor.positionAtDepth[0]["Tom Brady"] == 20
		assert predictor.positionAtDepth[0]["Aaron Rodgers"] == 25
		assert predictor.positionAtDepth[1]["Adrian Peterson"] == 22
		assert predictor.positionAtDepth[1]["Doug Martin"] == 18
	}

	@Test
	void testInitializePositionTypes_QB_RB_WR_FLEX() {
		predictor.projections_qb["Tom Brady"] = 20
		predictor.projections_qb["Aaron Rodgers"] = 25

		predictor.projections_rb["Adrian Peterson"] = 22
		predictor.projections_rb["Doug Martin"] = 18

		predictor.projections_wr["Calvin Johnson"] = 15
		predictor.projections_wr["A.J. Green"] = 10

		predictor.positionTypes = "QB,RB,WR,FLEX"

		predictor.initializePositionTypes()

		assert predictor.positionAtDepth[0]["Tom Brady"] == 20
		assert predictor.positionAtDepth[0]["Aaron Rodgers"] == 25
		assert predictor.positionAtDepth[1]["Adrian Peterson"] == 22
		assert predictor.positionAtDepth[1]["Doug Martin"] == 18
		assert predictor.positionAtDepth[2]["Calvin Johnson"] == 15
		assert predictor.positionAtDepth[2]["A.J. Green"] == 10
		assert predictor.positionAtDepth[3]["Adrian Peterson"] == 22
		assert predictor.positionAtDepth[3]["Doug Martin"] == 18
		assert predictor.positionAtDepth[3]["Calvin Johnson"] == 15
		assert predictor.positionAtDepth[3]["A.J. Green"] == 10
	}

	@Test
	void testCleanData() {
		predictor.site = Predictor.DRAFT_KINGS

		// Demaryius Thomas not in salaries
		predictor.readSalaries()

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

	@Test
	void testInitializePositionTypes() {
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE,DEF,K,FLEX"
		predictor.initializePositionTypes()

		assert predictor.positionIndices["QB"].size() == 1 && predictor.positionIndices["QB"][0] == 0
		assert predictor.positionIndices["RB"].size() == 2 && predictor.positionIndices["RB"][0] == 1 && predictor.positionIndices["RB"][1] == 2
		assert predictor.positionIndices["WR"].size() == 2 && predictor.positionIndices["WR"][0] == 3 && predictor.positionIndices["WR"][1] == 4
		assert predictor.positionIndices["TE"].size() == 1 && predictor.positionIndices["TE"][0] == 5
		assert predictor.positionIndices["DEF"].size() == 1 && predictor.positionIndices["DEF"][0] == 6
		assert predictor.positionIndices["K"].size() == 1 && predictor.positionIndices["K"][0] == 7
		assert predictor.positionIndices["FLEX"].size() == 1 && predictor.positionIndices["FLEX"][0] == 8
	}

	@Test
	void testIsDuplicate() {
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE,DEF,K,FLEX"
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "adrian peterson"])
		assert !predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "fred jackson"])
	}

	@Test
	void testIsDuplicate_3OfSamePosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "cj spiller", "adrian peterson"])
		assert !predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "doug martin", "fred jackson"])
	}

	@Test
	void testIsDuplicate_1OfPosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.initializePositionTypes()

		assert !predictor.isDuplicate(0, "tom brady", [])
	}

	@Test
	void testIsCorrectStartingIndex() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.initializePositionTypes()

		// Single Quarterback
		assert predictor.isCorrectStartingIndex(0,0) == true

		predictor.positionCounter["QB"][0] = 10
		assert predictor.isCorrectStartingIndex(0,10) == true

		// Set 1st RB to 10 so 2nd should start at 11
		predictor.positionCounter["RB"][0] = 10
		assert predictor.isCorrectStartingIndex(2,11) == true

		// Set 2nd RB to 20 so 3rd should start at 21
		predictor.positionCounter["RB"][1] = 20
		assert predictor.isCorrectStartingIndex(3, 20) == false
		assert predictor.isCorrectStartingIndex(3, 21) == true
	}
}
