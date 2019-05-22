# Motuclient Java Project
@author <smarty@cls.fr>: Scrum master, Software architect, Quality assurance, Continuous Integration manager   

>How to read this file? 
Use a markdown reader: 
plugins [chrome](https://chrome.google.com/webstore/detail/markdown-preview/jmchmkecamhbiokiopfpnfgbidieafmd?utm_source=chrome-app-launcher-info-dialog) exists (Once installed in Chrome, open URL chrome://extensions/, and check "Markdown Preview"/Authorise access to file URL.), 
or for [firefox](https://addons.mozilla.org/fr/firefox/addon/markdown-viewer/)  (anchor tags do not work)
and also plugin for [notepadd++](https://github.com/Edditoria/markdown_npp_zenburn).

>Be careful: Markdown format has issue while rendering underscore "\_" character which can lead to bad variable name or path.


# Summary
* [Overview](#Overview)
* [Build](#Build)
* [Installation](#Installation)
    * [Prerequisites](#InstallationPre)
    * [Using Maven](#InstallationMaven)
* [Integration examples](#IntegrationExamples)
    * [Out of a Java spring projects](#IntegrationExamplesNonSpring)
    * [In a Java spring projects](#IntegrationExamplesInSpring)
* [API](#API)
    * [Using Spring API](#APISpring)
        * [Download](#APIExamplesDownload)
        * [GetSize](#APIExamplesGetSize)	
        * [DescribeProduct](#APIExamplesDescribeProduct)
* [Licence](#Licence)

	
	
# <a name="Overview">Overview</a>
Motu client "motuclient-java" is a Java API used to connect to Motu HTTP server in order to:  

* __extract__ the data of a dataset, with geospatial, temporal and variable criterias (default option)   
* __get the size__ of an extraction with geospatial, temporal and variable criterias  
* __get information__ about a dataset  

This program can be integrated into a processing chain in order to automate the downloading of products via the Motu.  
  
  
# <a name="Build">Build</a>  
From the "motuclient-java-parent" folder runs the command:  
  
```
mvn clean install -Dmaven.test.skip=true
[...]
[INFO] BUILD SUCCESS
[...]
```  

This creates Jar archives in the target folder:

* motuclient-java-tools/target/motuclient-java-tools-X.Y.Z.jar: Archive containing the API



# <a name="Installation">Installation</a> 

## <a name="InstallationPre">Prerequisites</a>
You must use Java version 1.8 or later.  

## <a name="InstallationMaven">Using Maven</a>
Add in your pom.xml the following maven dependency:  
```  
<dependency>  
  <groupId>cls.atoll.motu.client</groupId>  
  <artifactId>motuclient-java-tools</artifactId>  
  <version>${project.version}</version>  
</dependency>  
```  

# <a name="IntegrationExamples">Integration examples</a> 
## <a name="IntegrationExamplesNonSpring">Out of a Java spring projects</a> 
See source code:   
* /motuclient-java-non-spring/src/main/java/cls/motu/MotuClientNonSpringApplication.java
* /motuclient-java-non-spring/src/main/resources/spring-context.xml

## <a name="IntegrationExamplesInSpring">In a Java spring project</a> 
See source code:   
* /motuclient-java/src/main/java/cls/motu/MotuClientApplication.java

# <a name="API">API</a>  
## <a name="APISpring">Using Spring API</a>  
See source code:   
* /motuclient-java/src/main/java/cls/motu/MotuClientApplication.java



# <a name="APIExamples">API examples</a>   

## <a name="APIExamplesDownload">Download</a>  
See source code:   
* /motuclient-java/src/main/java/cls/motu/MotuClientApplication.java

## <a name="APIExamplesGetSize">GetSize</a>  
See source code:   
* /motuclient-java/src/main/java/cls/motu/MotuClientApplication.java

## <a name="APIExamplesDescribeProduct">DescribeProduct</a>  
See source code:   
* /motuclient-java/src/main/java/cls/motu/MotuClientApplication.java




# <a name="Licence">Licence</a> 
This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.  
  
This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.  
  
You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.  
