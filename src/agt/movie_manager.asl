// movie manager agent

/* Initial beliefs */
watchlist(["The Wolf of Wall Street", "Inception", "Avengers: Endgame", "The Godfather"]). // the agent believes that the user has a watchlist

/* Initial goals */
!start. // the agent has an initial goal to start

/* 
 * Plan for reacting to the addition of the goal !start
 * Triggering event: addition of goal !start
 * Context: true (the plan is always applicable)
 * Body: greets the world 
*/
@start_plan
+!start : true <-
    .print("Hello world").    

/* 
 * Plan for reacting to the addition of the belief available_container
 * Triggering event: addition of belief available_container
 * Context: true (the plan is always applicable)
 * Body: creates the goal to publish the watchlist to the LDP container
*/
@available_container_plan
+available_container(ContainerName) : true <-
    !publish_watchlist. // creates goal !publish_watchlist

/* 
 * Plan for reacting to the addition of the goal !available_container
 * Triggering event: addition of belief !available_container
 * Context: the agent believes that the user has a watchlist
 * Body: performs an action that publishes movies in an LDP container personal data within a file watchlist.txt
*/
@publish_watchlist_plan
+!publish_watchlist : watchlist(Movies) <-
    .print("Publishing movies from watchlist: ", Movies);
    publishData("personal-data", "watchlist.txt", Movies). // performs an action that publishes movies in an LDP container personal data within a file watchlist.txt

/* Import behavior of agents that work in CArtAgO environments */
{ include("$jacamoJar/templates/common-cartago.asl") }