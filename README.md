one-day-fantasy-leagues
=======================
This application takes in fantasy projections and salary data for One-Day Fantasy Leagues and uses dynamic programming to compute the optimal roster.

The data can come from any source, but needs to be in the format of <player name>,<position(s)>,<projection>,<salary>.

The application takes the following arguments:
1- Site name (this can be one of DRAFTDAY, DRAFTKINGS, DRAFTSTREET, FANDUEL, STARSTREET)
2- Total salary available
3- Roster composition (i.e. StarStreet NBA would be PG,SG,SF,PF,C,G,F,C,FLEX,FLEX)
4- Sport (basketball|football)

The input file should be placed in the data/ directory and have a naming convention of <site name>_<sport>.csv.  This will be properly picked up, parsed, and analyzed.

Processing for all sites but DraftStreet should take about 4 seconds at the most.  Once processing is done, the lineup that yields the most points based on the provided projections given the salary constraints is displayed.
