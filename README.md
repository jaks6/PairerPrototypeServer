PairerPrototypeServer
=====================

To run with Maven, 
simply do ```maven package``` or something similar. A .war file is generated in the project-s "target" folder. Deploy this to a webserver (Tomcat 7).

The server responds to http POST requests with contents formatted such as the following example:
```
{"device":"LT26w","timestamp":1397730440343,"sequence":[0,0,0,0,0,28,48,35,39,37,39,42,42,35,48,40,44,37,40,47,33,45,46,48,33,32,29,41,43,44,51,52,31,63,94,195,180,166,136,183,186,121,146,115,228,136,165,178,124,75]}```
