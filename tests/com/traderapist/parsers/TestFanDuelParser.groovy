package com.traderapist.parsers

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created with IntelliJ IDEA.
 * User: dmaclean
 * Date: 8/28/13
 * Time: 12:36 PM
 * To change this template use File | Settings | File Templates.
 */
class TestFanDuelParser {
    FanDuelParser parser

    @Before
    void setUp() {
        parser = new FanDuelParser()
    }

    @After
    void tearDown() {
        parser = null
    }

    @Test
    void testParse_NoSource() {
        parser.source = null
        parser.parse()
        assert parser.output == null
    }

    @Test
    void testParse() {
        def source = "<tbody>\n" +
                "\t\t<tr id=\"playerListPlayerId_6504\" data-role=\"player\" data-position=\"QB\" data-fixture=\"65677\" class=\"pR  fixtureId_65677 teamId_28  \">\n" +
                "\t\t<td><span>QB</span></td>\n" +
                "\t\t<td><div onclick=\"sSts('6504_65677_9600')\">Drew Brees</div></td>\n" +
                "\t\t<td>22.8</td>\n" +
                "\t\t<td>16</td>\n" +
                "\t\t<td>ATL@<b>NO</b></td>\n" +
                "\t\t<td>\$9,600</td>\n" +
                "\t\t<td><a data-role=\"add\" data-player-id=\"6504\" class=\"button mini primary\">Add</a></td>\n" +
                "</tr>\n" +
                "<tr id=\"playerListPlayerId_6703\" data-role=\"player\" data-position=\"RB\" data-fixture=\"65674\" class=\"pR  fixtureId_65674 teamId_21  \">\n" +
                "<td><span>RB</span></td>\n" +
                "<td><div onclick=\"sSts('6703_65674_9500')\">Adrian Peterson</div></td>\n" +
                "<td>20.5</td>\n" +
                "<td>16</td>\n" +
                "<td><b>MIN</b>@DET</td>\n" +
                "<td>\$9,500</td>\n" +
                "<td><a data-role=\"add\" data-player-id=\"6703\" class=\"button mini primary\">Add</a></td>\n" +
                "</tr></tbody>"

        parser.source = source

        parser.parse()

        assert parser.output[0] == "QB,Drew Brees,9600"
        assert parser.output[1] == "RB,Adrian Peterson,9500"
    }

    @Test
    void testParse_ClearExistingOutput() {
        def source = "<tbody>\n" +
                "<tr id=\"playerListPlayerId_6504\" data-role=\"player\" data-position=\"QB\" data-fixture=\"65677\" class=\"pR  fixtureId_65677 teamId_28  \">\n" +
                "<td><span>QB</span></td>\n" +
                "<td><div onclick=\"sSts('6504_65677_9600')\">Drew Brees</div></td>\n" +
                "<td>22.8</td>\n" +
                "<td>16</td>\n" +
                "<td>ATL@<b>NO</b></td>\n" +
                "<td>\$9,600</td>\n" +
                "<td><a data-role=\"add\" data-player-id=\"6504\" class=\"button mini primary\">Add</a></td>\n" +
                "</tr>\n" +
                "<tr id=\"playerListPlayerId_6703\" data-role=\"player\" data-position=\"RB\" data-fixture=\"65674\" class=\"pR  fixtureId_65674 teamId_21  \">\n" +
                "<td><span>RB</span></td>\n" +
                "<td><div onclick=\"sSts('6703_65674_9500')\">Adrian Peterson</div></td>\n" +
                "<td>20.5</td>\n" +
                "<td>16</td>\n" +
                "<td><b>MIN</b>@DET</td>\n" +
                "<td>\$9,500</td>\n" +
                "<td><a data-role=\"add\" data-player-id=\"6703\" class=\"button mini primary\">Add</a></td>\n" +
                "</tr></tbody>"

        parser.output = "some crap that shouldn't be here."

        parser.source = source

        parser.parse()

        assert parser.output[0] == "QB,Drew Brees,9600"
        assert parser.output[1] == "RB,Adrian Peterson,9500"
    }
}
