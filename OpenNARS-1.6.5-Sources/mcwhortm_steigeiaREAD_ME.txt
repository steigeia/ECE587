This is Tanner's and Isaac's ReadMe for our NARS 1.6.5 Project.

The source files for this project were downloaded from the following link: https://drive.google.com/drive/folders/0B8Z4Yige07tBUk5LSUtxSGY0eVk

The zipped file was then extracted. The project can then be opened using your favorite Java IDE. We used Eclipse because it offers convenient debugging 
tools. The original source files will not compile as downloaded. The project is looking for files that weren't in the correct pacckage. We assumed that 
the developers moved these files to seperate directories while they tried to improve them. By moving the files to the correct directory/package aloud
the project to compile. 

The dependency graph was created using the Eclipse plugin Java Dependency Veiwer 1.0.10. This plugin was downloaded from the following website:
			https://marketplace.eclipse.org/content/java-dependency-viewer
All of the NARS packages were selected and the "veiw package dependency" tad was selected under the "Java Dependency viewer" tab wihtin Eclipse. This 
produces a visual graph and each of the elements could be moved around. We removed overlapping elements to make the graph more readable. A screenshot was
then taken of this. This process does not output a file.

For the profiler, we used hprof, which is built into the Java JDK. This prints out the profiler information to a text file. It simply does profiling on a
Java virtual machine. The following line was included as a VM arguement in the Eclipse IDE when the project is compiled or run:
-agentlib:hprof=heap=all,cpu=samples,file=out.hprof
This tells it to use hprof when its executed. heap=all means it collects all the heap information. cpu = samples means that it collects cpu data through 
sampling. file=out.hprof defines the output file name. It is a .hprof file however it can be read as a text file. This was done on a Windows environment.
To generate the out.hprof file, the NARS Launcher was exectuted and openNARS was selected from the title menu. NARS was then provided the 
forwardConditioning Narsese memory and the model ran for some time.

In order to obtain the directory diagram, the project had to be moved to a Linux environment. This was done by zipping the project and sending it to 
ourselves through email. Once the project was extracted on a Linux environment, a directory diagram was produced by executing the following command from 
the root directory of the project: tree . > tree.txt. The directory diagram is then outputted to the tree.txt file.

The doxygen html can be found at:
http://users.miamioh.edu/mcwhortm/ECE587Final/index.html