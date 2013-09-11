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
class TestBaseballPredictor {
	BaseballPredictor predictor

	@Before
	void setUp() {
		predictor = new BaseballPredictor()
	}

	@After
	void tearDown() {
		predictor = null
	}

	@Test
	void testInitializePositionTypes_QB_RB() {
		predictor.positionTypes = "QB,RB"

		predictor.readInput("data/test/test_initialize_1.csv")
		predictor.initializePositionTypes()

		assert predictor.positionAtDepth[0]["Tom Brady"] == 20.09
		assert predictor.positionAtDepth[0]["Aaron Rodgers"] == 19.61
		assert predictor.positionAtDepth[1]["Adrian Peterson"] == 21.16
		assert predictor.positionAtDepth[1]["Doug Martin"] == 13.51
	}

	@Test
	void testInitializePositionTypes_QB_RB_WR_FLEX() {
		predictor.positionTypes = "QB,RB,WR,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert predictor.positionAtDepth[0]["Tom Brady"] == 20.09
		assert predictor.positionAtDepth[0]["Aaron Rodgers"] == 19.61
		assert predictor.positionAtDepth[1]["Adrian Peterson"] == 21.16
		assert predictor.positionAtDepth[1]["Doug Martin"] == 13.51
		assert predictor.positionAtDepth[2]["Calvin Johnson"] == 14.27
		assert predictor.positionAtDepth[2]["A.J. Green"] == 10.83
		assert predictor.positionAtDepth[3]["Adrian Peterson"] == 21.16
		assert predictor.positionAtDepth[3]["Doug Martin"] == 13.51
		assert predictor.positionAtDepth[3]["Calvin Johnson"] == 14.27
		assert predictor.positionAtDepth[3]["A.J. Green"] == 10.83
	}

	@Test
	void testInitializePositionTypes() {
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE,DEF,K,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_2.csv")
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
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "adrian peterson"])
		assert !predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "fred jackson"])
	}

	@Test
	void testIsDuplicate_3OfSamePosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "cj spiller", "adrian peterson"])
		assert !predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "doug martin", "fred jackson"])
	}

	@Test
	void testIsDuplicate_1OfPosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert !predictor.isDuplicate(0, "tom brady", [])
	}

	@Test
	void testIsDuplicate_Baseball() {
		predictor.positionTypes = "2B,OF"
		predictor.sport = BaseballPredictor.SPORT_BASEBALL

		predictor.readInputBaseball("data/test/test_baseball.csv")
		predictor.initializePositionTypes()

		assert !predictor.isDuplicate(0, "ben zobrist", [])
	}

	@Test
	void testIsCorrectStartingIndex() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_correct_starting_index.csv")
		predictor.initializePositionTypes()

		// Single Quarterback
		assert predictor.isCorrectStartingIndex(0,0) == true

		predictor.indexTracker[0] = 10
		assert predictor.isCorrectStartingIndex(0,10) == true

		// Set 1st RB to 10 so 2nd should start at 11
		predictor.indexTracker[1] = 10
		assert predictor.isCorrectStartingIndex(2,11) == true

		// Set 2nd RB to 20 so 3rd should start at 21
		predictor.indexTracker[2] = 20
		assert predictor.isCorrectStartingIndex(3, 20) == false
		assert predictor.isCorrectStartingIndex(3, 21) == true
	}

	@Test
	void testInitializePositionTypes_Remove0DollarSalaries() {
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE,DEF,K,FLEX"
		predictor.sport = BaseballPredictor.SPORT_FOOTBALL

		predictor.readInput("data/test/test_initialize_zero_dollar.csv")
		predictor.initializePositionTypes()

		assert predictor.projections["QB"].size() == 1
		assert predictor.projections["RB"].size() == 1
		assert predictor.projections["WR"].size() == 1
		assert predictor.projections["TE"].size() == 1
		assert predictor.projections["DEF"].size() == 1
		assert predictor.projections["K"].size() == 1
		assert predictor.projections["FLEX"].size() == 3
	}

	@Test
	void testInitializePositionTypes_Baseball() {
		predictor.positionTypes = "P,P,1B,2B,SS,OF,OF,OF"
		predictor.sport = BaseballPredictor.SPORT_BASEBALL

		predictor.readInputBaseball("data/test/test_baseball.csv")
		predictor.initializePositionTypes()

		assert predictor.projections["P"].size() == 3
		assert predictor.projections["1B"].size() == 1
		assert predictor.projections["2B"].size() == 1
		assert predictor.projections["SS"].size() == 1
		assert predictor.projections["OF"].size() == 1
	}

	@Test
	void testReadInput_Baseball() {
		predictor.positionTypes = "P,2B,OF"
		predictor.sport = BaseballPredictor.SPORT_BASEBALL

		predictor.readInputBaseball("data/test/test_baseball.csv")

		assert predictor.projections["P"].size() == 3
		assert predictor.projections["2B"].size() == 1
		assert predictor.projections["OF"].size() == 1
	}

	@Test
	void testReadInput_Baseball_DH_to_1B() {
		predictor.positionTypes = "P,2B,1B"
		predictor.sport = BaseballPredictor.SPORT_BASEBALL

		predictor.readInputBaseball("data/test/test_baseball_dh.csv")

		assert predictor.projections["P"].size() == 3
		assert predictor.projections["2B"].size() == 1
		assert predictor.projections["1B"].size() == 2
	}
}
