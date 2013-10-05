package com.traderapist.prediction

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 10/2/13
 * Time: 10:09 AM
 * To change this template use File | Settings | File Templates.
 */
class TestBayesFFPredictor {
	BayesFFPredictor predictor

	@Before
	void setUp() {
		predictor = new BayesFFPredictor()
	}

	@After
	void tearDown() {
		predictor = null
	}

	@Test
	void testReadSalaries_FanDuel() {
		// FAN_DUEL NumberFire 60000 QB,RB,RB,WR,WR,WR,TE,K,DEF football
		predictor.site = Predictor.FAN_DUEL
		predictor.sport = Predictor.SPORT_FOOTBALL
		predictor.projectionSource = Predictor.BAYESFF
		predictor.budget = 60000
		predictor.positionTypes = "QB,RB,RB,WR,WR,WR,TE,K,DEF"

		predictor.readSalaries("data/test/test_bayesff_salaries.csv")

		assert predictor.salaries["a.peterson"] == 10000
		assert predictor.salaries["r.griffiniii"] == 8800
		assert predictor.salaries["a.green"] == 8600
		assert predictor.salaries["49ers"] == 5700
		assert predictor.salaries["seahawks"] == 5700
		assert predictor.salaries["j.graham"] == 8400
	}

	@Test
	void testTransformName_FanDuel() {
		// FAN_DUEL NumberFire 60000 QB,RB,RB,WR,WR,WR,TE,K,DEF football
		predictor.site = Predictor.FAN_DUEL
		predictor.sport = Predictor.SPORT_FOOTBALL
		predictor.projectionSource = Predictor.BAYESFF
		predictor.budget = 60000
		predictor.positionTypes = "QB,RB,RB,WR,WR,WR,TE,K,DEF"

		assert predictor.transformName("Peyton Manning", "QB") == "p.manning"
		assert predictor.transformName("Robert Griffin III", "QB") == "r.griffiniii"
		assert predictor.transformName("San Francisco 49ers", "DEF") == "49ers"
	}

	@Test
	void testTransformName_DraftKings() {
		// FAN_DUEL NumberFire 60000 QB,RB,RB,WR,WR,WR,TE,K,DEF football
		predictor.site = Predictor.DRAFT_KINGS
		predictor.sport = Predictor.SPORT_FOOTBALL
		predictor.projectionSource = Predictor.BAYESFF
		predictor.budget = 60000
		predictor.positionTypes = "QB,RB,RB,WR,WR,WR,TE,K,DEF"

		assert predictor.transformName("Peyton Manning", "QB") == "p.manning"
		assert predictor.transformName("Robert Griffin III", "QB") == "r.griffiniii"
		assert predictor.transformName("49ers", "DEF") == "49ers"
	}
}
