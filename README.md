PairerPrototypeServer
=====================

To run with Maven, 
simply do ```maven package``` or something similar. A .war file is generated in the project-s "target" folder. Deploy this to a webserver (Tomcat 7).

The server responds to http POST requests with contents formatted such as the following example:
```{"device":"LT26w","timestamp":1397730930543,"sequence":[0,0,0,0,0,60,51,49,73,61,59,61,55,49,48,46,51,58,65,48,65,43,82,119,275,288,68,40,47,73,97,63,88,116,69,204,163,243,182,183,238,292,349,264,143,203,57,92,72,64],"mac":"D0:51:62:93:E8:CE"}
