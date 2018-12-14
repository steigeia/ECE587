Tanner McWhorter and Isaac Steiger ReadME

Open-NARS 1.6.5 can be downloaded from the following site: https://drive.google.com/drive/folders/0B8Z4Yige07tBUk5LSUtxSGY0eVk
	-Please note that if you are to download the source files that we worked with for this project, then the model will
	 not compile. The developers moved some of the files around, which caused quite a bit of errors throughout the 
         project. These errors were corrected by moving the files to their correct diretories.
	-The original nars gui is included in the project as a reference to base our changes off of. It's called OpenNARS_GUI_Original.jar

Our alterations to this model can be found at the following link: https://github.com/steigeia/ECE587/tree/master/OpenNARS-1.6.5-Sources

Once you have downloaded either of the models, please extract the zip folder to a desired workplace.

This model requires Java 8 and an IDE that can work with Java (IDE if you would like to make alterations). We used Eclipse because of its useful 
features. We would recomend using the same IDE. Once you have opened your desired IDE, the project should be loaded into the workplace. In 
Eclipse, this can be done by clicking file->open projects from system directory. A screen will appear and ask for you to select the root folder 
for the NARS project (Project_OpenNARS-1.6.5-Sources). Leave everything default and the IDE will perform some setup checks. In 
order to import the project, you simply need to click the finish button. The project should open in the project workspace.

Before executing a NARS model, you should specify the path for the Narsese memory that you would like to load in. This can be done
by copying and pasting the file path into the index.txt file that is in the root folder (Project_OpenNARS-1.6.5-Sources). If you would like to
launch NARS with no memory, then provide a path to an empty text file. Our testing and benchmark memories can be found in the DATA
directory in the OpenNARS-1.6.5-Sources folder.

The NARS GUI that we worked with can be accessed through the following methods (either will work the same):
	-Executing the OpenNARS_GUI_OurVersion.jar by either double clicking it on a Windows machine or by calling the folloing
	command in the command line of a Linux machine: java -jar OpenNARS_GUI_OurVersion.jar
	-by right clicking the root folder (Project_OpenNARS-1.6.5-Sources) in Eclipse and selecting run as and then selecting Java
	Application. A window will appear asking you to choose from the different applicaitons that have been developed for NARS. 
	Select "Launcher - nars.lab,launcher" from the list to start the NARS GUI.
	-The following command can be run from the command line of a Linux machine to compile and execute the GUI launcher, however 
	you will need to change each of the filepaths (we did not use this method):
	C:\Program Files\Java\jdk1.8.0_191\bin\javaw.exe -Dfile.encoding=Cp1252 -classpath "C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto
	\OpenNARS-1.6.5-Sources\bin;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\asm-all-5.0.3.jar;C:\Users\isaac_000
	\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\commons-math3-3.3.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto
	\OpenNARS-1.6.5-Sources\lib\encog-core-3.3.0.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib
	\grappa-1.0.0-beta.9-1.0.0-beta.9.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\gson-2.2.4.jar;C:\Users
	\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\guava-18.0.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto
	\OpenNARS-1.6.5-Sources\lib\gui\jgrapht-ext-0.9.1-SNAPSHOT.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib
	\gui\jgraphx.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\gui\org.abego.treelayout.core-1.0.1.jar;C:
	\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\gui\processing_org-core.jar;C:\Users\isaac_000\Downloads
	\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\java_websocket.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources
	\lib\javolution-core-java-6.1.0-20140103.154957-9.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib
	\jgrapht-core-0.9.1-SNAPSHOT.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\lab\jannlab-0.10-alpha.jar;C:
	\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\lab\JavaRLGlueCodec.jar;C:\Users\isaac_000\Downloads
	\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\lab\jbox2d-library-2.2.1.2-SNAPSHOT.jar;C:\Users\isaac_000\Downloads
	\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\log4j-1.2.16	.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto
	\OpenNARS-1.6.5-Sources\lib\test\hamcrest-core-1.3.jar;C:\Users\isaac_000\Downloads\OpenNARS-1.6.5-Sources_Auto\OpenNARS-1.6.5-Sources\lib\test
	\junit-4.11.jar" nars.lab.launcher.Launcher
	
	-The main file that should be launched is the Launcher.java file in the nars.lab.launcher folder

In order to run the various benchmarks and test memories simply provide the memory file path to the index.txt file and then execute the GUI.
The model will begin evauating the provided memory. This is then repeated for the other memories. These tests could not be included in one
file and this is further explained in the report that accompanies this project.

The files that are required to build our report can be found in the REPORT folder in the root folder (Project_OpenNARS-1.6.5-Sources).

The Benchmarks and testing data can be found in the root folder (Project_OpenNARS-1.6.5-Sources). Our excel woorkbook that was used to generate
the plots that were used for the report are in this folder as well.

It should be noted that this project and report was developed entirely on a Windows machine, however the GUI jar was tested on a Linux machine. We never
built the model on a Linux machine.