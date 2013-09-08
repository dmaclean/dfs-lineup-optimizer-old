package com.traderapist.prediction

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/24/13
 * Time: 7:38 PM
 * To change this template use File | Settings | File Templates.
 */
class BaseballPredictor {
	/**
	 * The service we are doing predictions for - FanDuel or Draft Kings.
	 */
	def site

	/**
	 * Which sport are we predicting for?
	 *
	 * - baseball
	 * - football
	 */
	def sport

    def table = new MemoTable()

	/**
	 * A map of the salaries for players, key is player name and value is their salary.
	 * Because the key is the player name, we need to clean the data beforehand to ensure
	 * there are no duplicates.  Usually, a sufficient way is to append the team name
	 * to resolve collisions.
	 */
	def salaries = [:]

	/**
	 * Map of maps containing projections for each position.  The key is the position name
	 * and the value is a map, keyed by player name with a value of their projected point total.
	 *
	 * [
	 *  "QB": ["Matt Ryan": 19.02, "Tom Brady": 21.00, ...],
	 *  "RB": [...],
	 *  ...
	 *  ...
	 * ]
	 */
	def projections = [:]

	/**
	 * An array of maps that contain the options for positions at each depth.  So, if the
	 * positionTypes string contains QB,RB,RB then the array will have three elements, at
	 * 0 will be a map for QBs, and at 1 and 2 will be the map for RBs.
	 */
	def positionAtDepth = []

	/**
	 * The input string (comma-separated) dictating which positions are used.  For example,
	 * the string might look like QB,RB,RB,WR,WR,TE,DEF,K
	 */
	def positionTypes

	def positionTypesAsArray

	def bestPoints = 0
	def bestRoster = []

	/**
	 * A map that keeps track of which indices in the roster are for each position type.
	 * The key is the position and value is an array of indices.
	 */
	def positionIndices = [:]

	/**
	 * Keeps track of which player is currently being evaluated at each roster position.
	 */
	def indexTracker = []

    def tableHits = 0
    def tableMisses = 0
    def tableHitRate = []

	/**
	 * Keeps track of how much money, at any given point, we have left
	 * to spend on remaining players to fill the roster.
	 */
    def budget

	/**
	 * The lowest-priced player at each position.
	 */
	def minCost = [:]

	def minTotalCost = []

    static def DRAFT_KINGS = "DRAFT_KINGS"
    static def FAN_DUEL = "FAN_DUEL"

	static def SPORT_BASEBALL = "baseball"
	static def SPORT_FOOTBALL = "football"

	def initializePositionTypes() {
		def pieces = positionTypes.split(",")

		positionTypesAsArray = pieces

		pieces.eachWithIndex { position, i ->
			positionAtDepth << projections[position]

			// Manage position indices
			(!positionIndices.containsKey(position)) ? positionIndices[position] = [i] : positionIndices[position] << i

			indexTracker << 0

			minTotalCost << 0

			tableHitRate << 0
		}

		def costSoFar = 0
		for(int i=minTotalCost.size()-1; i>= 0; i--) {
			def minCostAtIndex = minCost[positionTypesAsArray[i]]
			costSoFar += minCostAtIndex
			minTotalCost[i] = costSoFar
		}
	}

	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>,<salary>
	 *
	 * FOOTBALL
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td.*?>.*?<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * BASEBALL
	 * <tr.*?><td.*?>\*?<a.*?>(.*?) \(([\d\/A-Z]+), [A-Z]+\)<\/a><\/td>(<td.*?>[\d\.]+<\/td>){0,13}<td class="sep">([\d\.]+)<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * @param file
	 * @return
	 */
	def readInput(file) {
		if(sport == SPORT_FOOTBALL)
			projections["FLEX"] = [:]

		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			/*
			 * Grab the salary and figure out if this is the lowest value for this position.
			 */
			def salary = Integer.parseInt(pieces[3])
			if(salary == 0)
				return

			salaries[name] = salary
			if(!minCost.containsKey(pieces[1]))
				minCost[pieces[1]] = Integer.MAX_VALUE

			if(salary > 0 && salary < minCost[pieces[1]])
				minCost[pieces[1]] = salary

			/*
			 * Check if the map of projections for this position exists, and if not, create it.
			 */
			if(!projections.containsKey(pieces[1]))
				projections[pieces[1]] = [:]
			projections[pieces[1]][name] = Double.parseDouble(pieces[2])

			/*
			 * For football, add RB, WR, and TEs to the flex position.
			 */
			if(sport == SPORT_FOOTBALL && pieces[1].matches("RB|WR|TE"))
				projections["FLEX"][name] = Double.parseDouble(pieces[2])
		}

		// Figure out the smallest FLEX cost
		if(sport == SPORT_FOOTBALL)
			minCost["FLEX"] = Math.min(minCost["RB"], Math.min(minCost["WR"], minCost["TE"]))
	}

	/**
	 * Expect input in the form of <name>,<position>,<fantasy points>,<salary>
	 *
	 * FOOTBALL
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td.*?>.*?<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * BASEBALL
	 * <tr.*?><td.*?>\*?<a.*?>(.*?) \(([\d\/A-Z]+), [A-Z]+\)<\/a><\/td>(<td.*?>[\d\.]+<\/td>){0,13}<td class="sep">([\d\.]+)<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * @param file
	 * @return
	 */
	def readInputBaseball(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[0]

			/*
			 * Grab the salary and figure out if this is the lowest value for this position.
			 */
			def salary = Integer.parseInt(pieces[3])
			if(salary == 0)
				return

			/*
			 * For baseball, we need to separate players that have two different positions
			 * listed, such as 2B/RF.
			 */
			def positions = pieces[1]
			if(pieces[1].indexOf("/") > -1) {
				positions = pieces[1].split("/")
			}
			else {
				positions = [positions]
			}

			for(position in positions) {
				if(position.matches("LF|CF|RF")) {
					position = "OF"
				}
				else if(position.matches("SP|RP")) {
					position = "P"
				}

				salaries[name] = salary
				if(!minCost.containsKey(position))
					minCost[position] = Integer.MAX_VALUE

				if(salary > 0 && salary < minCost[position])
					minCost[position] = salary

				/*
				 * Check if the map of projections for this position exists, and if not, create it.
				 */
				if(!projections.containsKey(position))
					projections[position] = [:]
				projections[position][name] = Double.parseDouble(pieces[2])
			}
		}
	}

    def reportTableStats() {
        def total = tableHits + tableMisses
        if(total % 100000 == 0 && tableHits > 0) {
            println "Table hits ${ tableHits/total }% \t\tTable misses ${ tableMisses/total }%\t\t\t" +
                    "(${table.items[0].size()}/${table.items[1].size()}/${table.items[2].size()}/${table.items[3].size()}/" +
                    "${table.items[4].size()}/${table.items[5].size()}/${table.items[6].size()}/${table.items[7].size()}/${table.items[8].size()})" +
                    "\t\t\t(${tableHitRate[0]/tableHits}/${tableHitRate[1]/tableHits}/${tableHitRate[2]/tableHits}/${tableHitRate[3]/tableHits}/${tableHitRate[4]/tableHits}/" +
                    "${tableHitRate[5]/tableHits}/${tableHitRate[6]/tableHits}/${tableHitRate[7]/tableHits}/${tableHitRate[8]/tableHits})"
        }
    }

    def printOptimalRoster() {
        println "Found best configuration so far with ${ bestPoints } for the following players:"
        def sal = 0
        bestRoster.each { p -> println "\t${ p } (${ salaries[p] })"; sal += salaries[p] }
        println "\tTotal salary: ${sal}"
    }

	/**
	 * Evaluates the previous roster indices that contain the same position
	 * to make sure the currently-chosen player is not already in the roster.
	 *
	 * @param depth     The current roster index that we're at.
	 * @param player    The player being considered.
	 * @param roster    The current roster, minus the player being considered.
	 * @return          True, if the player name already exists in the roster.  False, otherwise.
	 */
	def isDuplicate(depth, player, roster) {
		if(sport == SPORT_FOOTBALL) {
			def position = positionTypesAsArray[depth]
			def foundDupe = false

			for(index in positionIndices[position]) {
				if(index >= depth)
					break

				if(roster[index] == player) {
					foundDupe = true
					break
				}
			}

			return foundDupe
		}
		else {
			for(currPlayer in roster) {
				if(player == currPlayer)
					return true
			}

			return false
		}
	}

	def isCorrectStartingIndex(depth, index) {
		// Is there a previous roster entry for this position?
		def position = positionTypesAsArray[depth]
		def closestIndex = -1
//		def i=0     // Where in positionIndices are we?
		for(currIdx in positionIndices[position]) {
			if(currIdx == depth)
				break
			if(currIdx > closestIndex && currIdx < depth) {
				closestIndex = currIdx
			}
		}

		// No previous index, whatever was passed in is fine.
		if(closestIndex == -1 || indexTracker[closestIndex] < index) {
			return true
		}

		return false
	}

	/**
	 * Dynamic programming solution for determining the optimal roster.
	 *
	 * @param depth
	 * @param budget
	 * @param totalPoints
	 * @param roster
	 * @return
	 */
	def generateOptimalTeamMemoization(depth, budget, totalPoints, roster) {

		if(depth == table.items.size()-1) {
			def bestPointsForFlex = 0
			def best = null

            /*
             * Do we have a pre-computed solution for this budget?
             */
			def result = table.getSolution(depth, budget)
			if(result) {
                tableHits++
                tableHitRate[depth]++
                return result
            }
            else {
                tableMisses++
            }

            reportTableStats()

            /*
             * No pre-computed solution.  Go through each option and determine the most valuable
             * for the budget provided.
             */
			indexTracker[depth] = 0
			for(e in positionAtDepth[depth]) {
                def name = e.key
				if(isDuplicate(depth, name, roster) || !isCorrectStartingIndex(depth, indexTracker[depth])) {
					indexTracker[depth]++
					continue
                }

				if(salaries[name] <= budget && e.value > bestPointsForFlex) {
					bestPointsForFlex = e.value
					best = name
				}

				indexTracker[depth]++
			}

			def newItem
			if(best) {
				newItem = new MemoItem(cost: budget, points: bestPointsForFlex, roster: [best])

				// Write to table
				table.writeSolution(depth, newItem)
			}

			return newItem
		}
        else {
            def result = table.getSolution(depth, budget)
            if(result) {
                tableHits++
                tableHitRate[depth]++
                return result
            }
            else {
                tableMisses++
            }

            reportTableStats()

			indexTracker[depth] = 0
            for(e in positionAtDepth[depth]) {
                if(isDuplicate(depth, e.key, roster) || !isCorrectStartingIndex(depth, indexTracker[depth])) {
	                indexTracker[depth]++
                    continue
                }

                // Can I afford this player?
                def cost = salaries[e.key]
                def points = e.value
                if(cost <= budget) {
                    result = null
                    if(budget-cost >= minTotalCost[depth+1]) {
                        roster << e.key
                        result = generateOptimalTeamMemoization(depth+1, budget - cost, totalPoints + points, roster)
                        roster.remove(e.key)

                        // Do we have an optimal solution?
                        if(result && totalPoints + e.value + result.points > bestPoints) {
                            def newRoster = []
                            newRoster.addAll(roster)
                            newRoster.add(e.key)
                            newRoster.addAll(result.roster)
                            Set set = new HashSet(newRoster)

                            if(set.size() == newRoster.size()) {
                                bestPoints = totalPoints + e.value + result.points
                                bestRoster.clear()
                                bestRoster = newRoster

                                printOptimalRoster()
                            }
                        }

                        if(result) {
                            def newItem = new MemoItem(cost: salaries[e.key] + result.cost, points: e.value + result.points, roster: [e.key])
                            newItem.roster.addAll(result.roster)
                            table.writeSolution(depth, newItem)
                        }
                    }
                }

	            indexTracker[depth]++
            }
        }
	}

	public static void main(String[] args) {
        if(args.length < 4 || !args[0].matches("${FAN_DUEL}|${DRAFT_KINGS}") || !args[1].matches("\\d+") || !args[3].matches("baseball|football")) {
            println "Usage: Predictor <FAN_DUEL|DRAFT_KINGS> <budget> <roster types> <baseball|football>"
            return
        }

		def p = new BaseballPredictor(site: args[0], budget: args[1].toInteger(), positionTypes: args[2], sport: args[3])

		if(p.sport == BaseballPredictor.SPORT_FOOTBALL)
			p.readInput("data/numberfire/${args[0]}_${args[3]}.csv")
		else if(p.sport == BaseballPredictor.SPORT_BASEBALL)
			p.readInputBaseball("data/numberfire/${args[0]}_${args[3]}.csv")

		p.initializePositionTypes()
		p.table.initializeItemsList(p.positionTypes)

        long start = System.currentTimeMillis()
		p.generateOptimalTeamMemoization(0, p.budget, 0, [])
        long end = System.currentTimeMillis()
        println "Computed optimal roster in ${ (end-start)/1000.0 } seconds."

        p.printOptimalRoster()
	}
}
