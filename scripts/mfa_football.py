import httplib,urllib,re

positionURLs = ["QB","RB","WR","TE","K","DEF"]
conn = httplib.HTTPConnection("www.myfantasyassistant.com")

f = open('myfantasyassistant.csv', 'w')

for position in positionURLs:
	conn.request("GET", "/weekly-rankings/?pos=" + position)
	response = conn.getresponse()

	print "Fetching " + position, response.status, response.reason

	data = response.read()
	tableStartIndex = data.find("<table id=\"hor-minimalist-b\">")
	tableEndIndex = tableStartIndex + data[tableStartIndex:].find("</table>")
	
	# Redefine data to be everything between the <table> tags.
	data = data[tableStartIndex:tableEndIndex].replace("\n","")
	m = re.findall('<tr><td>\d+\.\s<\/td><td>(.*?)(<a.*?<\/a>)?<\/td>.*?<td>([\d\.]+)<\/td><\/tr>', data)
	
	for result in m:
		f.write( ",".join([result[0], position, result[2] + "\n"]) )

f.close()