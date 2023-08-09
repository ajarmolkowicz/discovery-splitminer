#!/bin/bash

mvn install:install-file -Dfile=./lib/BPMN.jar -DgroupId=org.processmining -DartifactId=BPMN -Dversion=LATEST -Dpackaging=jar
mvn install:install-file -Dfile=./lib/GraphViz.jar -DgroupId=org.processmining.plugins.graphviz -DartifactId=GraphViz -Dversion=LATEST -Dpackaging=jar
mvn install:install-file -Dfile=./lib/ProM-Contexts-6.8.34.jar -DgroupId=org.processmining.contexts -DartifactId=ProM-Contexts -Dversion=6.8.34 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/ProM-Framework-6.8.60.jar -DgroupId=org.processmining.framework -DartifactId=ProM-Framework -Dversion=6.8.60 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/ProM-Models-6.7.20.jar -DgroupId=org.processmining.models -DartifactId=ProM-Models -Dversion=6.7.20 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/ProM-Plugins.jar -DgroupId=org.processmining.plugins -DartifactId=ProM-Plugins -Dversion=LATEST -Dpackaging=jar
mvn install:install-file -Dfile=./lib/slickerbox-1.0rc1.jar -DgroupId=com.fluxicon.slickerbox -DartifactId=slickerbox -Dversion=1.0rc1 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/tablelayout.jar -DgroupId=info.clearthought.layout -DartifactId=tablelayout -Dversion=LATEST -Dpackaging=jar
mvn install:install-file -Dfile=./lib/uitopia-6.5.jar -DgroupId=org.deckfour.uitopia -DartifactId=uitopia -Dversion=6.5 -Dpackaging=jar
mvn install:install-file -Dfile=./lib/Widgets.jar -DgroupId=org.processmining -DartifactId=Widgets -Dversion=LATEST -Dpackaging=jar
