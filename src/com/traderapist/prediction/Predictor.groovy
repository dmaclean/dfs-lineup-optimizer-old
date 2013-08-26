package com.traderapist.prediction

import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/24/13
 * Time: 7:38 PM
 * To change this template use File | Settings | File Templates.
 */
class Predictor {
	def salaryFile = "data/DKSalaries.csv"
	def qbs = "data/FantasyPros_Fantasy_Football_Rankings_QB.csv"
	def rbs = "data/FantasyPros_Fantasy_Football_Rankings_RB.csv"
	def wrs = "data/FantasyPros_Fantasy_Football_Rankings_WR.csv"
	def tes = "data/FantasyPros_Fantasy_Football_Rankings_TE.csv"
	def ds = "data/FantasyPros_Fantasy_Football_Rankings_DEF.csv"
	def ks = "data/FantasyPros_Fantasy_Football_Rankings_K.csv"

	def salaries = [:]
	def projections_qb = [:]
	def projections_rb = [:]
	def projections_wr = [:]
	def projections_te = [:]
	def projections_d = [:]
	def projections_k = [:]
	def projections_flex = [:]

	def positionAtDepth = [
			projections_qb,
			projections_rb,
			projections_rb,
			projections_wr,
			projections_wr,
			projections_te,
			projections_d,
			projections_k,
			projections_flex
		]

	def bestPoints = 0
	def bestRoster = []

	def wr1Index = 0
	def rb1Index = 0

	/**
	 * Parses the salaries from the salaries CSV into a map keyed by the player name.
	 *
	 * @return
	 */
	def readSalaries(file) {
		new File(file).eachLine { line ->
			def pieces = line.split(",")
			def name = pieces[1].replaceAll("\"", "").replaceAll("\\.", "").trim().toLowerCase()
			def salary = pieces[2].toInteger()

			if(salaries.containsKey(name)) {
				println "Salaries already contains ${ name }"
				return false
			}
			salaries[name] = salary
		}

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

	def generateOptimalTeamRecursion(depth, budget, totalPoints, roster) {
//		positionAtDepth[depth].eachWithIndex { name,points, i ->

		// Don't bother if we can't afford anyone
		if(budget < 3000)
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

	def generateOptimalTeamMemoization(depth, budget, totalPoints, roster) {
//		positionAtDepth[depth].eachWithIndex { name,points, i ->

		// Don't bother if we can't afford anyone
		if(budget < 3000)
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
		def p = new Predictor()

		if(!p.readSalaries(p.salaryFile))
			return

		p.readProjections()
		p.cleanData()
		p.projections_flex.putAll(p.projections_rb)
		p.projections_flex.putAll(p.projections_wr)
		p.projections_flex.putAll(p.projections_te)
		p.projections_flex.sort()

//		p.generateOptimalTeamByBruteForce()
		p.generateOptimalTeamRecursion(0, 50000, 0, [])
	}
}
