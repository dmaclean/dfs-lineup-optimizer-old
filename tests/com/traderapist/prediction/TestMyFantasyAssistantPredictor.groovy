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

	@Test
	void testReadInputFootball() {
		predictor.sport = Predictor.SPORT_FOOTBALL
		predictor.site = Predictor.DRAFT_KINGS

		predictor.readInputFootball("data/test/MyFantasyAssistant/football.csv")

		assert predictor.projections["QB"]["colin kaepernick"] == 16.66
		assert predictor.projections["QB"]["russell wilson"] == 21.22

		assert predictor.projections["RB"]["marshawn lynch"] == 15.3
		assert predictor.projections["RB"]["frank gore"] == 15.5

		assert predictor.projections["WR"]["aj green"] == 16.3
		assert predictor.projections["WR"]["anquan boldin"] == 16.3

		assert predictor.projections["TE"]["vernon davis"] == 14.9
		assert predictor.projections["TE"]["tyler eifert"] == 12.2

		assert predictor.projections["DEF"]["Seahawks"] == 5.03
		assert predictor.projections["DEF"]["49ers"] == 6.15

		assert predictor.projections["K"]["steven hauschka"] == 7.08
		assert predictor.projections["K"]["phil dawson"] == 8.58
	}

	@Test
	void testReadInputBaseball() {
		predictor.sport = Predictor.SPORT_BASEBALL
		predictor.site = Predictor.DRAFT_KINGS

		predictor.readInputBaseball("data/test/MyFantasyAssistant/baseball.csv")

		assert predictor.projections["P"]["cliff lee"] == 12.4
		assert predictor.projections["P"]["aj burnett"] == 11.63

		assert predictor.projections["C"]["hector gimenez"] == 7.595
		assert predictor.projections["C"]["john hester"] == 6.175

		assert predictor.projections["1B"]["carlos pena"] == 6.9375
		assert predictor.projections["1B"]["travis ishikawa"] == 6.3225

		assert predictor.projections["2B"]["ryan roberts"] == 6.5
		assert predictor.projections["2B"]["brendan harris"] == 4.4925

		assert predictor.projections["3B"]["trevor plouffe"] == 3.37
		assert predictor.projections["3B"]["luis jimenez"] == 3.31

		assert predictor.projections["SS"]["alex gonzalez"] == 6.835
		assert predictor.projections["SS"]["cody ransom"] == 5.4175

		assert predictor.projections["OF"]["tyler colvin"] == 6.805
		assert predictor.projections["OF"]["shelley duncan"] == 6.4625
		assert predictor.projections["OF"]["carlos pena"] == 6.9375
	}

	@Test
	void testReadSalaries_Baseball_FanDuel() {
		predictor.sport = Predictor.SPORT_BASEBALL
		predictor.site = Predictor.FAN_DUEL

		predictor.readInputBaseball("data/test/MyFantasyAssistant/baseball.csv")
		predictor.readSalaries("data/test/FAN_DUEL/salaries_baseball.csv")

		assert predictor.salaries["cliff lee"] == 9800
		assert predictor.salaries["aj burnett"] == 9000

		assert predictor.salaries["hector gimenez"] == 5500
		assert predictor.salaries["john hester"] == 4000

		assert predictor.salaries["carlos pena"] == 6000
		assert predictor.salaries["travis ishikawa"] == 3200

		assert predictor.salaries["ryan roberts"] == 4000
		assert predictor.salaries["brendan harris"] == 4100

		assert predictor.salaries["trevor plouffe"] == 7000
		assert predictor.salaries["luis jimenez"] == 6500

		assert predictor.salaries["alex gonzalez"] == 4400
		assert predictor.salaries["cody ransom"] == 3400

		assert predictor.salaries["tyler colvin"] == 6000
		assert predictor.salaries["shelley duncan"] == 7000
	}

	@Test
	void testReadSalaries_Baseball_DraftKings() {
		predictor.sport = Predictor.SPORT_BASEBALL
		predictor.site = Predictor.DRAFT_KINGS

		predictor.readInputBaseball("data/test/MyFantasyAssistant/baseball.csv")
		predictor.readSalaries("data/test/${predictor.site}/salaries_baseball.csv")

		assert predictor.salaries["cliff lee"] == 12700
		assert predictor.salaries["aj burnett"] == 9300

		assert predictor.salaries["hector gimenez"] == 3400
		assert predictor.salaries["john hester"] == 2000

		assert predictor.salaries["carlos pena"] == 2800
		assert predictor.salaries["travis ishikawa"] == 2000

		assert predictor.salaries["ryan roberts"] == 2000
		assert predictor.salaries["brendan harris"] == 2400

		assert predictor.salaries["trevor plouffe"] == 3800
		assert predictor.salaries["luis jimenez"] == 2300

		assert predictor.salaries["alex gonzalez"] == 3100
		assert predictor.salaries["cody ransom"] == 2800

		assert predictor.salaries["tyler colvin"] == 4200
		assert predictor.salaries["shelley duncan"] == 2300
	}

	@Test
	void testOptimalLineup() {
		predictor.budget = 18000
		predictor.positionTypes = "P,C,1B"

		predictor.readInputBaseball("data/test/MyFantasyAssistant/baseball.csv")
		predictor.readSalaries("data/test/FAN_DUEL/salaries_baseball.csv")

		predictor.cleanData()

		predictor.initializePositionTypes()
		predictor.table.initializeItemsList(predictor.positionTypes)

		long start = System.currentTimeMillis()
		predictor.generateOptimalTeamMemoization(0, predictor.budget, 0, [])
		long end = System.currentTimeMillis()
		println "Computed optimal roster in ${ (end-start)/1000.0 } seconds."

		predictor.printOptimalRoster()

		assert predictor.bestRosters.size() == 1
		assert predictor.bestRosters[0][0] == "aj burnett"
		assert predictor.bestRosters[0][1] == "hector gimenez"
		assert predictor.bestRosters[0][2] == "travis ishikawa"

		assert predictor.bestPoints == 25.5475
	}
}
