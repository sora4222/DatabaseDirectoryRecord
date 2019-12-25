# Database Directory Record

This application watches directories for changes in folders, when they are found the change is uploaded to
a database. The database records a hash of each file, the full location, and 
the computer name where the file is located.

The configuration of this application is a json file which can be specified and will be searched for by the application:
 1. A json configuration file which has it's location passed as a parameter to the application
 2. A json configuration located in ~/.directoryRecorder/Config.json
 3. The parameters are passed in when running the application (commandline)
 
The parameters are:
 *  databasePassword
 *  databaseUsername
 *  jdbcConnectionUrl like "jdbc:mysql://database-directory-database.example.com:3306/\<database here\>"
 *  rootLocations (which is a list of the locations to scan)
 *  excludeRegex (a list of regex for what files and folders to ignore)
 
```
{
  "databasePassword": "password",
  "databaseUsername": "username",
  "jdbcConnectionUrl": "jdbc:databasetype://your_uri/directory_records_db",
  "rootLocations": [
    "/opt/database_directory/root1",
    "/home/"
  ],
  "excludeRegex": [".txt", "/\.\\S+/"]
}

```
<b>Note</b> - The excludeRegex field has to escape it's backslash characters
whilst this is very annoying it can't be helped. So \\d becomes, or the second example  above
captures all directories starting with a dot.

Doing this will be eventually partnered with another project to create a GUI that will list all the
files I have on each of my computers and allow for transfers and quick search capabilities.

This was tested against a MySQL database.