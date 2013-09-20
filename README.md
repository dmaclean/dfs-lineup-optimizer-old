one-day-fantasy-leagues
=======================
This application takes in fantasy projections and salary data for One-Day Fantasy Leagues and uses dynamic programming to compute the optimal roster.

The data comes from (right now) three separate sources:

<h3>NumberFire</h3>
NumberFire provides fantasy projections and salary data, so we can actually get it all from one source.  For this source, we grab data from the projection table on the site, parse out the player name, position, projection, and salary, and add that to the appropriate CSV file under data/numberfire.<br/>
The NumberFireProjector, given the appopriate parameters, will read projections and salary from this file and perform the configuration.

<h3>MyFantasyAssistant.com</h3>
MyFantasyAssistant provides projections only.  Because of this, we need to grab salary data from the ODFL site and put it in its own CSV file.  From here, the application will parse the projection and salary files separately, clean the data (delete projection entries with no corresponding salary, and vice-versa), and generate a projection.

<h3>DailyFantasyProjections.com</h3>
