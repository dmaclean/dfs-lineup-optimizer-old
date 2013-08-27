package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/26/13
 * Time: 10:31 PM
 * To change this template use File | Settings | File Templates.
 */
class MemoTable {
	def items = [
	        [],     // QB
			[],     // RB1
			[],     // RB2
			[],     // WR1
			[],     // WR2
			[],     // TE
			[],     // DEF
			[],     // K
			[]      // FLEX
	]

	def getSolution(index, budget) {
		for(item in items[index]) {
			if(budget >= item.lowCost && budget <= item.highCost) {
				return item
			}
		}

		return null
	}

	def writeSolution(index, newItem) {
		items[index] << newItem
		items[index].sort()
	}
}
