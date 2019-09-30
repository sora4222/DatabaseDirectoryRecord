# Aims for release version 1
 * Use event based subscribing to obtain all the files that have changed
 * Use the current scanner to obtain a seed of files. Potentially changing it to make the initial files.
 * The database accesses need some form of batching to increase performance and reduce overhead
 
## Planning of changes
The current scanner goes through and obtains each file and directory checks it for changes in the database. Perhaps
instead it can go through and still check the files for changes and for each directory obtain a WatchService. The
check of all the files during the startup will also allow for the seed of all the files for the database.

The database needs to reduce its overhead as the number of files I currently have on my system is going to cause thousands
of individual requests each time the system is run through.