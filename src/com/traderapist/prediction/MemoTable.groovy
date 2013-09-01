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

    /**
     * Retrieve the optimal player configuration for our budget.  This takes advantage
     * of a binary search in the Java Collections framework since our list is already
     * sorted upon insertion in writeSolution().
     *
     * @param index         The current depth we're at.
     * @param budget        How much money we have left to spend.
     * @return              The optimal configuration for our budget, if one exists.  Null, otherwise.
     */
	def getSolution(index, budget) {
//		for(item in items[index]) {
//			if(budget >= item.lowCost && budget <= item.highCost) {
//				return item
//			}
//		}
//
//		return null

        def hitIndex = Collections.binarySearch(items[index], new MemoItem(cost: budget))
        if(hitIndex < 0)
            return null
        return items[index][hitIndex]
	}

    /**
     * Adds the new solution to the list at the provided index/depth and then sorts it.  Sorting is
     * taken care of automatically by the Collections framework since our MemoItem class implements
     * Comparable.
     *
     *
     *
     * @param index
     * @param newItem
     */
	def writeSolution(index, newItem) {
        def existing = getSolution(index, newItem.cost)
        if(existing && existing.points >= newItem.points) {
            return
        }
        else if(existing && existing.points < newItem.points) {
            existing.points = newItem.points
            existing.roster = newItem.roster
            return
        }

		items[index] << newItem
		items[index].sort()

        // Consolidate overlapping entries (this shouldn't happen)
//        for(int i=0; i<items[index].size()-1; i++) {
//            if(items[index][i].highCost == items[index][i+1].highCost && items[index][i].lowCost < items[index][i+1].lowCost) {
//                if(items[index][i].points > items[index][i+1].points) {
//                    items[index].remove(i+1)
//                }
//            }
//        }
	}
}
