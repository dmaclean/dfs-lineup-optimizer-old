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
		MemoItem item = new MemoItem(lowCost: 3000, highCost: 3500, points: 100, roster: ["Dan"])

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
	void testWriteSolution_OverlappingItems_SameValues() {
		MemoItem item = new MemoItem(lowCost: 3000, highCost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, item)

		assert table.items[8].size() == 1

		MemoItem dupe = new MemoItem(lowCost: 3000, highCost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, dupe)

		assert table.items[8].size() == 1
	}

	@Test
	void testWriteSolution_OverlappingItems_NewValueLower() {
		MemoItem item = new MemoItem(lowCost: 3000, highCost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, item)

		assert table.items[8].size() == 1

		MemoItem dupe = new MemoItem(lowCost: 2900, highCost: 3500, points: 100, roster: ["Dan"])
		table.writeSolution(Predictor.FLEX, dupe)

		assert table.items[8].size() == 1
	}
}
