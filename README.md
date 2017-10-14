# Xchangecore-OpenFeedProcessor
Polls XChangeCore 2.0 to get XML, KML, or RSS feeds

<b>Xchangecore</b> (http://www.xchangecore.org/) is a project developed under directive from the Dept. of Homeland Security and was originally called UICDS.

Its purpose is simply to share information.  It has the abiity to ingest several different standards and output
XML, KML, JSON, and RSS feeds.  This "adapter" or program, simple reads a configuration file, and polls these feeds
writing the output into a file.

<b><u>The configuration file:</u></b><br/>
The configuration file must contain 5 entries, the Xchangecore URL, username, password, output filename and interval to poll.
The code is written to handle multiple feed, so eache parameter has a number suffix
url1<br/>
username1<br/>
password1<br/>
pollInterval1 <i>(in seconds)</i><br/>
outputfile1<br/>

The URL parameter is really the only one that needs explination, as it requires a little background with Xchangecore operation.
Xchangecore has an search interface that takes parameters that include what to include in the result and how to format the output.
<br/>
The query will look similar to this:<br/>
<font size="2">
<i>https://host.domain.com/xchangecore/pub/search?full=true&productType=Incident&productType=Alert&productType=SOI&productType=MapViewContext&format=xml</i>
</font>
<br/>
You will notice that multile productType parameters can be specificed to include in the output.
The format parameter can be one of:   <b>xml, kml, json, or rss</b><br/>
<b><i>Refer to the Xchangecore documentation about the parameters (http://www.xchangecore.org/)</i></b>
<br/>
<br/>
<b>Configuraiton Example:<b/><br/>
The following configuation will poll two diferent Xchangecore systems, every 10 minutes.<br/>
The first entry will output all product types, in XML format<br/>
The second entry will output only incidents and MapViewContext in KML format<br/>
<br/>
<br/>
<font size="2">
url1=https://hosta.domain.com/xchangecore/pub/search?full=true&productType=Incident&productType=Alert&productType=SOI&productType=MapViewContext&format=xml<br/>
username1={user1}<br/>
password1={password1}<br/>
pollInterval1=600<br/>
output1={output path 1}<br/>
url2=https://hostb.domain.com/xchangecore/pub/search?full=true&productType=Incident&productType=MapViewContext&format=kml<br/>
password2={password2}<br/>
username2={user2}<br/>
pollInterval2=600<br/>
output2={output path 2}<br/>
</font>
<b><u>Program Operation</u></b><br/>
The program is meant to run in the background as a service, and produce an output file on the local filesystem.<br/>
<br/>
<b>Command line:</b> <i>java -jar OpenFeedProcessor-1.0.jar</i>
<br/>
<li>A log file will be produced in the current working directory using log4j2.</li>
<li>It is possible to specify command line parameter: <b>-Dlog4j.configurationFile</b> to your own log4j2 configuration file.</li>
<li>If there is no configuration file, an example config file will be created and the program will exit</li>



