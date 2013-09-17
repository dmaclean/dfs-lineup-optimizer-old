import httplib,urllib,re

positionURLs = ["P","C","1B","2B","3B","SS","OF"]
conn = httplib.HTTPConnection("www.myfantasyassistant.com")

f = open('myfantasyassistant.csv', 'w')

for position in positionURLs:
	conn.request("GET", "/daily-fantasy-baseball-projections/?a=" + position)
	response = conn.getresponse()

	print "Fetching " + position, response.status, response.reason

	data = response.read()

	tableStartIndex = data.find("<table id='hor-minimalist-b'>")
	tableEndIndex = tableStartIndex + data[tableStartIndex:].find("</table>")

	m = re.findall('<tr><td>(.*?)(<a.*?<\/a>)?<\/td>.*?<td>([\d\.]+)<\/td><\/tr>', data[tableStartIndex:tableEndIndex])

	for result in m:
		f.write( ",".join([result[0], position, result[2] + "\n"]) )

f.close()