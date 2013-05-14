
The source code for a pretty old personal project (2005).
More information can be find there : http://reuse.pagesperso-orange.fr/exproxy/

Quickly converted to use SBT (http://www.scala-sbt.org/) instead of ANT

If your main network interface is br0 for example, run like this : 
sbt "run br0"

default is eth0
sbt run
is the same as :
sbt "run eth0"

