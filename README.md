PairerPrototypeServer
=====================

To run with Maven, 
simply do ```maven package``` or something similar. A .war file is generated in the project-s "target" folder. Deploy this to a webserver (Tomcat 7).

The server respons to POST requests which are formatted like the following JSON message:
{"timestamp":123141421214, "device":"nexus", "sequence":[1,2,3,4,5,6,67,8,9]}
