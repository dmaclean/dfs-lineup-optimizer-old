package com.traderapist.prediction

import com.traderapist.parsers.DraftKingsParser
import com.traderapist.parsers.FanDuelParser

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/24/13
 * Time: 7:38 PM
 * To change this template use File | Settings | File Templates.
 */
class Predictor {
	/**
	 * The service we are doing predictions for - FanDuel or Draft Kings.
	 */
	def site

	def draftKingsSalaryFile = "data/draftkings/DKSalaries.csv"
    def fanDuelSalaryFile = "data/fanduel/salaries.txt"
	def qbs = "data/FantasyPros_Fantasy_Football_Rankings_QB.csv"
	def rbs = "data/FantasyPros_Fantasy_Football_Rankings_RB.csv"
	def wrs = "data/FantasyPros_Fantasy_Football_Rankings_WR.csv"
	def tes = "data/FantasyPros_Fantasy_Football_Rankings_TE.csv"
	def ds = "data/FantasyPros_Fantasy_Football_Rankings_DEF.csv"
	def ks = "data/FantasyPros_Fantasy_Football_Rankings_K.csv"

    def table = new MemoTable()

	/**
	 * A map of the salaries for players, key is player name and value is their salary.
	 * Because the key is the player name, we need to clean the data beforehand to ensure
	 * there are no duplicates.  Usually, a sufficient way is to append the team name
	 * to resolve collisions.
	 */
	def salaries = [:]
	def projections_qb = [:]
	def projections_rb = [:]
	def projections_wr = [:]
	def projections_te = [:]
	def projections_d = [:]
	def projections_k = [:]
	def projections_flex = [:]

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

	def wr1Index = 0
	def rb1Index = 0

	/**
	 * A map that keeps track of which indices in the roster are for each position type.
	 * The key is the position and value is an array of indices.
	 */
	def positionIndices = [:]

	def positionCounter = [:]

	/**
	 * Keeps track of which player is currently being evaluated at each roster position.
	 */
	def indexTracker = []

    def tableHits = 0
    def tableMisses = 0
    def tableHitRate = [0,0,0,0,0,0,0,0,0]

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

	def initializePositionTypes() {
//		projections_flex.putAll(projections_rb)
//		projections_flex.putAll(projections_wr)
//		projections_flex.putAll(projections_te)
		projections_flex.sort()

		def pieces = positionTypes.split(",")

		positionTypesAsArray = pieces

		pieces.eachWithIndex { position, i ->
			if(position == "QB")        positionAtDepth << projections_qb
			else if(position == "RB")   positionAtDepth << projections_rb
			else if(position == "WR")   positionAtDepth << projections_wr
			else if(position == "TE")   positionAtDepth << projections_te
			else if(position == "DEF")  positionAtDepth << projections_d
			else if(position == "K")    positionAtDepth << projections_k
			else if(position == "FLEX") positionAtDepth << projections_flex

			// Manage position indices
			(!positionIndices.containsKey(position)) ? positionIndices[position] = [i] : positionIndices[position] << i

			(!positionCounter.containsKey(position)) ? positionCounter[position] = [0] : positionCounter[position] << 0

			indexTracker << 0

			minTotalCost << 0
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
	 * <tr.*?><td.*?><a.*?>(.*?) \(([A-Z]+), [A-Z]+\)<\/a>.*?(<td class="nf strong">(.*?)<\/td>)<td.*?>.*?<\/td><td>\$(\d+)<\/td>.*?<\/tr>
	 * \1,\2,\4,\5\n
	 *
	 * @param file
	 * @return
	 */
	def readInput(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")

			def salary = Integer.parseInt(pieces[3])
			salaries[pieces[0]] = salary
			if(!minCost.containsKey(pieces[1]))
				minCost[pieces[1]] = Integer.MAX_VALUE

			if(salary > 0 && salary < minCost[pieces[1]])
				minCost[pieces[1]] = salary

			if(pieces[1] == "QB") {
				projections_qb[pieces[0]] = Double.parseDouble(pieces[2])
			}
			else if(pieces[1] == "RB") {
				projections_rb[pieces[0]] = Double.parseDouble(pieces[2])
				projections_flex[pieces[0]] = Double.parseDouble(pieces[2])
			}
			else if(pieces[1] == "WR") {
				projections_wr[pieces[0]] = Double.parseDouble(pieces[2])
				projections_flex[pieces[0]] = Double.parseDouble(pieces[2])
			}
			else if(pieces[1] == "TE") {
				projections_te[pieces[0]] = Double.parseDouble(pieces[2])
				projections_flex[pieces[0]] = Double.parseDouble(pieces[2])
			}
			else if(pieces[1] == "DEF") {
				projections_d[pieces[0]] = Double.parseDouble(pieces[2])
			}
			else if(pieces[1] == "K") {
				projections_k[pieces[0]] = Double.parseDouble(pieces[2])
			}
		}

		// Figure out the smallest FLEX cost
		def minVal = Integer.MAX_VALUE
		if(minCost["RB"] < minCost["WR"]) {
			if(minCost["TE"] < minCost["RB"])
				minVal = minCost["TE"]
			else
				minVal = minCost["RB"]
		}
		else {
			if(minCost["TE"] < minCost["WR"])
				minVal = minCost["TE"]
			else
				minVal = minCost["WR"]
		}
		minCost["FLEX"] = minVal
	}

	/**
	 * Parses the salaries from the salaries CSV into a map keyed by the player name.
	 *
	 * @return
	 */
	def readSalaries() {
        def parser
        if(site == DRAFT_KINGS) {
            parser = new DraftKingsParser()
            parser.readSalaries(draftKingsSalaryFile)
            parser.parse()
        }
        else if(site == FAN_DUEL) {
            parser = new FanDuelParser()
            parser.readSalaries(fanDuelSalaryFile)
            parser.parse()
        }

        parser.output.each { line ->
            def pieces = line.split(",")
            def name = pieces[1].replaceAll("\"", "").replaceAll("\\.", "").replaceAll("null", "").trim().toLowerCase()
            def salary = pieces[2].toInteger()
	        def position = pieces[0]
	        if(position == "D" || position == "DST")     position = "DEF"
	        else if(position.contains("null"))           position.replace("null", "")

	        if(!minCost.containsKey([position]))
		        minCost[position] = Integer.MAX_VALUE

            if(salary < minCost[position])
                minCost[position] = salary

            if(salaries.containsKey(name)) {
                println "Salaries already contains ${ name }"
                return false
            }
            salaries[name] = salary
        }

		// Figure out the smallest FLEX cost
		def minVal = Integer.MAX_VALUE
		if(minCost["RB"] < minCost["WR"]) {
			if(minCost["TE"] < minCost["RB"])
				minVal = minCost["TE"]
			else
				minVal = minCost["RB"]
		}
		else {
			if(minCost["TE"] < minCost["WR"])
				minVal = minCost["TE"]
			else
				minVal = minCost["WR"]
		}
		minCost["FLEX"] = minVal


		return true
	}

	/**
	 * Parses each of the projection files into a map keyed by the player name.
	 *
	 * @return
	 */
	def readProjections() {
		new File(qbs).eachLine { line ->
			if(!processProjectionLine(line, projections_qb))
				return
		}

		new File(rbs).eachLine { line ->
			if(!processProjectionLine(line, projections_rb))
				return
		}

		new File(wrs).eachLine { line ->
			if(!processProjectionLine(line, projections_wr))
				return
		}

		new File(tes).eachLine { line ->
			if(!processProjectionLine(line, projections_te))
				return
		}

		new File(ds).eachLine { line ->
			if(!processProjectionLine(line, projections_d))
				return
		}

		new File(ks).eachLine { line ->
			if(!processProjectionLine(line, projections_k))
				return
		}
	}

	/**
	 * Performs processing of each line of the projections.
	 *
	 * @param line
	 * @return
	 */
	def processProjectionLine(line, map) {
		def pieces = line.split(",")
		def name = pieces[0].replaceAll("\\.", "").toLowerCase()
		def projection = pieces[pieces.length-1].toDouble()

		if(map.containsKey(name)) {
			println "Projections already contains ${ name }"
			return false
		}
		map[name] = projection

		return true
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

	def cleanData() {
		// Clear out any salaries that don't have projections.
		def deletes = []
		salaries.each { k,v ->
			def points = null

			if(projections_qb.containsKey(k))   points = projections_qb[k]
			else if(projections_rb.containsKey(k))   points = projections_rb[k]
			else if(projections_wr.containsKey(k))   points = projections_wr[k]
			else if(projections_te.containsKey(k))   points = projections_te[k]
			else if(projections_d.containsKey(k))   points = projections_d[k]
			else if(projections_k.containsKey(k))   points = projections_k[k]

			if(!points) {
				println "Could not find projection for ${k}, deleting..."
				deletes << k
			}
		}

		deletes.each { name -> salaries.remove(name) }

		// Clear out any projections that don't have salaries
		deletes.clear()
		[projections_qb, projections_rb, projections_wr, projections_te, projections_d, projections_k].each { p ->
			p.each { k,v -> if(!salaries[k]) deletes << k}
			deletes.each { name -> p.remove(name) }
		}
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

		foundDupe
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

	def generateOptimalTeamRecursion(depth, budget, totalPoints, roster) {
//		positionAtDepth[depth].eachWithIndex { name,points, i ->

		// Don't bother if we can't afford anyone
		if(budget < minCost)
			return

		def i =0
		for(e in positionAtDepth[depth]) {
			/*
			 * Skip to k+1 index for RB2 and WR2.
			 */
			if(depth == 1) {
				rb1Index = i
			}
			else if(depth == 3) {
				wr1Index = i
			}
			else if( (depth == 2 && i<= rb1Index) || (depth == 4 && i<= wr1Index) ) {
				i++
				continue
			}

			def name = e.key
			def points = e.value
			def isDupe = false

			// 2nd running back or 2nd wide receiver - check for dupes
			if( (depth == 2 && name == roster[1]) || (depth == 4 && name == roster[3]) ||
					(depth == 8 && (name == roster[1] || name == roster[2] || name == roster[3] || name == roster[4] || name == roster[5])) ) {
				isDupe = true
			}

			if(!isDupe) {
				def cost = salaries[name]
				budget -= cost

				// Does adding this player put us over-budget?
				if(budget >= 0) {
					totalPoints += points
					roster << name

					if(depth < 8) {
						generateOptimalTeamRecursion(depth+1, budget, totalPoints, roster)
					}
					else if(totalPoints > bestPoints) {
						println "Found best configuration so far with ${ totalPoints } for the following players:"
						bestRoster.clear()
						roster.each { p ->
							println "\t${ p }"
							bestRoster << p
						}

						bestPoints = totalPoints
					}

					totalPoints -= points
					roster.remove(roster.size()-1)
				}
				// Is what we have up to this point the best configuration?
				else if(totalPoints > bestPoints) {
					println "Found best configuration so far with ${ totalPoints } for the following players:"
					bestRoster.clear()
					roster.each { p ->
						println "\t${ p }"
						bestRoster << p
					}

					bestPoints = totalPoints
				}

				budget += cost
			}

			i++
		}
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

	def generateOptimalTeamByBruteForce() {
		cleanData()

		def count = 0
		def budget = 50000
		long start = System.currentTimeMillis()

		// need 1 quarterback
		projections_qb.each { qb_k, qb_v ->
			budget -= qb_v

			// need 2 running backs
			projections_rb.each { rb_k, rb_v ->
				projections_rb.each { rb2_k, rb2_v ->
					if(rb_k != rb2_k) {
						// Need 2 wide receivers
						projections_wr.each { wr_k, wr_v ->
							projections_wr.each { wr2_k, wr2_v ->
								if(wr_k != wr2_k) {
									// Need 1 tight end
									projections_te.each { te_k, te_v ->
										// Need 1 kicker
										projections_k.each { k_k, k_v ->
											// Need 1 defense
											projections_d.each { d_k, d_v ->
												// Need 1 flex
												[projections_rb, projections_wr, projections_te].each { list ->
													list.each { flex_k, flex_v ->
														if(flex_k != rb_k && flex_k != rb2_k && flex_k != wr_k && flex_k != wr2_k && flex_k != te_k) {
															count++
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}

			budget += qb_v
		}
		long end = System.currentTimeMillis()
		println "Finished evaluating ${ count } combinations in ${ (end-start)/1000.0 }"
	}

	public static void main(String[] args) {
        if(args.length < 3 || !args[0].matches("${FAN_DUEL}|${DRAFT_KINGS}") || !args[1].matches("\\d+")) {
            println "Usage: Predictor <FAN_DUEL|DRAFT_KINGS> <budget> <roster types>"
            return
        }

		def p = new Predictor()
		p.positionTypes = args[2]
        p.site = (args[0] == FAN_DUEL) ? FAN_DUEL : DRAFT_KINGS
        p.budget = args[1].toInteger()

		p.site == FAN_DUEL ? p.readInput("data/numberfire/fanduel.csv") : p.readInput("data/numberfire/draftkings.csv")

		p.initializePositionTypes()
		p.table.initializeItemsList(p.positionTypes)

        long start = System.currentTimeMillis()
//		p.generateOptimalTeamByBruteForce()
//		p.generateOptimalTeamRecursion(0, 50000, 0, [])
		p.generateOptimalTeamMemoization(0, p.budget, 0, [])
        long end = System.currentTimeMillis()
        println "Computed optimal roster in ${ (end-start)/1000.0 } seconds."

        p.printOptimalRoster()
	}
}
