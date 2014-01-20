one-day-fantasy-leagues
=======================
This application takes in fantasy projections and salary data for One-Day Fantasy Leagues and uses dynamic programming to compute the optimal roster.

The data can come from any source, but needs to be in the format of <player name>,<position(s)>,<projection>,<salary>.

The application (executed from Predictor.groovy) takes the following arguments:
<dd>
<dt>1- Site name (this can be one of <b>DRAFT_DAY</b>, <b>DRAFT_KINGS</b>, <b>DRAFT_STREET</b>, <b>FAN_DUEL</b>, <b>STAR_STREET</b>)</dt>
<dt>2- Total salary available</dt>
<dt>3- Roster composition (i.e. StarStreet NBA would be PG,SG,SF,PF,C,G,F,C,FLEX,FLEX)</dt>
<dt>4- Sport (basketball|football)</dt>
</dd>

The input file should be placed in the data/ directory and have a naming convention of [site name]_[sport].csv.  This will be properly picked up, parsed, and analyzed.

Processing for all sites but DraftStreet (the more specific, and therefore less common, player prices there are, the longer processing takes) should take about 4 seconds at the most.  Once processing is done, the lineup that yields the most points based on the provided projections given the salary constraints is displayed.
