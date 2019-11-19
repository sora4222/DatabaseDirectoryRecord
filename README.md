# Database Directory Record

This application watches directories for changes in folders, when they are found the change is uploaded to
a database. The database records a hash of each file, the full location, and 
the computer name where the file is located.

The configuration of this application is a json file which can be specified and will be searched for by the application:
 1. A json configuration file which has it's location passed as a parameter to the application
 2. A json configuration located in ~/.directoryRecorder/Config.json
 3. Multiple parameters that are passed in when running the application: 

Doing this will be eventually partnered with another project to create a GUI that will list all the
files I have on each of my computers and allow for transfers and quick search capabilities.

The tested database is a MySQL database.