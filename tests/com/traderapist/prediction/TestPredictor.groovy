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
		predictor = new Predictor(sport: Predictor.SPORT_FOOTBALL)
	}

	@After
	void tearDown() {
		predictor = null
	}

	@Test
	void testInitializePositionTypes_QB_RB_WR_FLEX() {
		predictor.positionTypes = "QB,RB,WR,FLEX"

		predictor.readInputFootball("data/test/test_initialize_2.csv")
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

		predictor.readInputFootball("data/test/test_initialize_2.csv")
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

		predictor.readInputFootball("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "adrian peterson"])
		assert !predictor.isDuplicate(2, "adrian peterson", ["aaron rodgers", "fred jackson"])
	}

	@Test
	void testIsDuplicate_3OfSamePosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"

		predictor.readInputFootball("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "cj spiller", "adrian peterson"])
		assert !predictor.isDuplicate(3, "adrian peterson", ["aaron rodgers", "doug martin", "fred jackson"])
	}

	@Test
	void testIsDuplicate_1OfPosition() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"

		predictor.readInputFootball("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert !predictor.isDuplicate(0, "tom brady", [])
	}

	@Test
	void testIsDuplicate_RB_FLEX() {
		predictor.positionTypes = "RB,FLEX"

		predictor.readInputFootball("data/test/test_initialize_2.csv")
		predictor.initializePositionTypes()

		assert !predictor.isDuplicate(1, "calvin johnson", ["calvin johnson"])
	}

	@Test
	void testIsCorrectStartingIndex() {
		predictor.positionTypes = "QB,RB,RB,RB,WR,TE,DEF,K,FLEX"

		predictor.readInputFootball("data/test/test_correct_starting_index.csv")
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
	void testNormalizeBaseballPosition() {
		predictor.sport = Predictor.SPORT_BASEBALL

		assert predictor.normalizeBaseballPosition("P") == "P"
		assert predictor.normalizeBaseballPosition("SP") == "P"
		assert predictor.normalizeBaseballPosition("RP") == "P"
		assert predictor.normalizeBaseballPosition("C") == "C"
		assert predictor.normalizeBaseballPosition("1B") == "1B"
		assert predictor.normalizeBaseballPosition("2B") == "2B"
		assert predictor.normalizeBaseballPosition("3B") == "3B"
		assert predictor.normalizeBaseballPosition("SS") == "SS"
		assert predictor.normalizeBaseballPosition("RF") == "OF"
		assert predictor.normalizeBaseballPosition("CF") == "OF"
		assert predictor.normalizeBaseballPosition("LF") == "OF"
		assert predictor.normalizeBaseballPosition("OF") == "OF"
		assert predictor.normalizeBaseballPosition("DH") == "1B"
	}

	@Test
	void testValidateInputs() {
		def args = new String[4]

		// bad length
		assert !predictor.validateInputs(args)

		// Site
		args = ["Bad site", "NumberFire", "35000", "QB,RB,RB,WR,WR", Predictor.SPORT_FOOTBALL].toArray()
		assert !predictor.validateInputs(args)

		args[0] = Predictor.DRAFT_KINGS
		assert predictor.validateInputs(args)
		args[0] = Predictor.DRAFT_STREET
		assert predictor.validateInputs(args)
		args[0] = Predictor.FAN_DUEL
		assert predictor.validateInputs(args)

		// Projection source
		args[1] = "Bad source"
		assert !predictor.validateInputs(args)

		args[1] = "NumberFire"
		assert predictor.validateInputs(args)
		args[1] = "MyFantasyAssistant"
		assert predictor.validateInputs(args)
		args[1] = "DailyFantasyProjections"
		assert predictor.validateInputs(args)

		// Budget
		args[2] = "This shouldn't be a string"
		assert !predictor.validateInputs(args)

		args[2] = "50000"
		assert predictor.validateInputs(args)

		// Roster - we don't currently check this

		// Sport
		args[4] = "wnba"
		assert !predictor.validateInputs(args)

		args[4] = Predictor.SPORT_BASEBALL
		assert predictor.validateInputs(args)
		args[4] = Predictor.SPORT_FOOTBALL
		assert predictor.validateInputs(args)
	}

	@Test
	void testOptimalNumberFire() {
		predictor.site = Predictor.DRAFT_KINGS
		predictor.positionTypes = "RB,RB,WR,WR,TE,DEF,K,FLEX,QB"

		predictor.readInputFootball("data/test/DRAFT_KINGS_football_optimal_test.csv")
		predictor.initializePositionTypes()
		predictor.table.initializeItemsList(predictor.positionTypes)

		predictor.generateOptimalTeamMemoization(0, 50000, 0, [])
	}

	@Test
	void testOptimalNumberFire2() {
		predictor.site = Predictor.DRAFT_KINGS
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE,DEF,K,FLEX"

		predictor.readInputFootball("data/test/DRAFT_KINGS_football_optimal_test_2.csv")
		predictor.initializePositionTypes()
		predictor.table.initializeItemsList(predictor.positionTypes)

		predictor.generateOptimalTeamMemoization(0, 50000, 0, [])

		assert predictor.bestPoints > 156
	}

	@Test
	void testReadConsistency() {
		predictor.site = Predictor.FAN_DUEL
		predictor.positionTypes = "QB,RB,RB,WR,WR,TE"

		predictor.readConsistencies("data/test/consistency/consistency.csv")

		assert !predictor.consistency["tom brady"]
		assert !predictor.consistency["cj spiller"]
		assert !predictor.consistency["maurice jones-drew"]

		assert predictor.consistency["ej manuel"] == 60
	}

	@Test
	void testReadInputBasketball() {
		predictor.positionTypes = "PG,PG,SG,SG,SF,SF,PF,PF,C"

		predictor.readInputBasketball("data/test/FAN_DUEL_basketball.csv")

		/*
		 * PROJECTIONS
		 */
		assert predictor.projections.size() == 5
		assert predictor.projections["PG"]["C.J. Watson(P)"] == 20.07
		assert predictor.projections["PG"]["Jeff Teague"] == 32.06
		assert predictor.projections["PG"]["Damian Lillard"] == 35.44

		assert predictor.projections["SG"].size() == 3
		assert predictor.projections["SG"]["Greivis Vasquez"] == 22.16
		assert predictor.projections["SG"]["Nate Robinson"] == 18.11
		assert predictor.projections["SG"]["Alec Burks"] == 22.78

		assert predictor.projections["SF"].size() == 3
		assert predictor.projections["SF"]["Wesley Johnson"] == 16.82
		assert predictor.projections["SF"]["Omri Casspi"] == 17.31
		assert predictor.projections["SF"]["Francisco Garcia"] == 14.35

		assert predictor.projections["PF"].size() == 3
		assert predictor.projections["PF"]["Andre Drummond"] == 33.59
		assert predictor.projections["PF"]["Udonis Haslem"] == 14.15
		assert predictor.projections["PF"]["LaMarcus Aldridge"] == 39.2

		assert predictor.projections["C"].size() == 3
		assert predictor.projections["C"]["Tyson Chandler"] == 27.24
		assert predictor.projections["C"]["Brook Lopez"] == 29.13
		assert predictor.projections["C"]["Greg Monroe"] == 29.79


		/*
		 * SALARIES
		 */
		assert predictor.salaries.size() == 15
		assert predictor.salaries["C.J. Watson(P)"] == 5000
		assert predictor.salaries["Jeff Teague"] == 11850
		assert predictor.salaries["Damian Lillard"] == 13250

		assert predictor.salaries["Greivis Vasquez"] == 7100
		assert predictor.salaries["Nate Robinson"] == 6100
		assert predictor.salaries["Alec Burks"] == 8500

		assert predictor.salaries["Wesley Johnson"] == 5000
		assert predictor.salaries["Omri Casspi"] == 5750
		assert predictor.salaries["Francisco Garcia"] == 5000

		assert predictor.salaries["Andre Drummond"] == 11750
		assert predictor.salaries["Udonis Haslem"] == 5000
		assert predictor.salaries["LaMarcus Aldridge"] == 13900

		assert predictor.salaries["Tyson Chandler"] == 10100
		assert predictor.salaries["Brook Lopez"] == 10900
		assert predictor.salaries["Greg Monroe"] == 11800

		/*
		 * MIN COST
		 */
		assert predictor.minCost.size() == 5
		assert predictor.minCost["PG"] == 5000
		assert predictor.minCost["SG"] == 6100
		assert predictor.minCost["SF"] == 5000
		assert predictor.minCost["PF"] == 5000
		assert predictor.minCost["C"] == 10100
	}
}
