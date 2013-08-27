package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/26/13
 * Time: 1:10 PM
 * To change this template use File | Settings | File Templates.
 */
class MemoItem implements Comparable<MemoItem> {
	/*
	 * We support a range of values for the cost.  This is the smaller value of the range.
	 */
	Double lowCost

	/*
	 * * We support a range of values for the cost.  This is the larger value of the range.
	 */
	Double highCost

	/*
	 * The number of points projected for this collection of players
	 */
	Double points

	/*
	 * The player's name
	 */
	def roster = []

	@Override
	int compareTo(MemoItem t) {
		if(highCost < t.lowCost) {
			return -1
		}
		else if(lowCost > t.highCost) {
			return 1
		}

		return 0
	}
}
