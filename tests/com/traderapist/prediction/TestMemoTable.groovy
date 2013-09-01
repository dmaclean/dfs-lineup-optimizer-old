package com.traderapist.prediction

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/31/13
 * Time: 10:51 PM
 * To change this template use File | Settings | File Templates.
 */
class TestMemoTable{
	MemoTable table

	@Before
	void setUp() {
		table = new MemoTable()
	}

	@After
	void tearDown() {
		table = null
	}

	@Test
	void testWriteSolution_FirstItem() {
		MemoItem item = new MemoItem(cost: 3500, points: 100, roster: ["Dan"])

		table.writeSolution(Predictor.FLEX, item)

		assert table.items[0].size() == 0
		assert table.items[1].size() == 0
		assert table.items[2].size() == 0
		assert table.items[3].size() == 0
		assert table.items[4].size() == 0
		assert table.items[5].size() == 0
		assert table.items[6].size() == 0
		assert table.items[7].size() == 0
		assert table.items[8].size() == 1
	}

	@Test
	void testWriteSolution_OverlappingItems_SamePoints() {
		MemoItem item = new MemoItem(cost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, item)

		assert table.items[Predictor.FLEX].size() == 1

		MemoItem dupe = new MemoItem(cost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, dupe)

		assert table.items[Predictor.FLEX].size() == 1
        assert table.items[Predictor.FLEX][0].points == 100
	}

	@Test
	void testWriteSolution_OverlappingItems_LowerPoints() {
		MemoItem item = new MemoItem(cost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, item)

        assert table.items[Predictor.FLEX].size() == 1

		MemoItem dupe = new MemoItem(cost: 3500, points: 90, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, dupe)

        assert table.items[Predictor.FLEX].size() == 1
        assert table.items[Predictor.FLEX][0].points == 100
	}

    @Test
    void testWriteSolution_OverlappingItems_HigherPoints() {
        MemoItem item = new MemoItem(cost: 3500, points: 100, roster: ["Dan"])
        table.writeSolution(Predictor.FLEX, item)

        assert table.items[Predictor.FLEX].size() == 1

        MemoItem dupe = new MemoItem(cost: 3500, points: 200, roster: ["Dan"])
        table.writeSolution(Predictor.FLEX, dupe)

        assert table.items[Predictor.FLEX].size() == 1
        assert table.items[Predictor.FLEX][0].points == 200
    }
}
