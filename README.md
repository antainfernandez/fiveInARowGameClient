# antainfernandez  5-in-a-Row - Client

5-in-a-Row, a variation of the famous Connect Four game, is a two-player connection game
in which the players first choose a color and then take turns dropping colored discs from the
top into a nine-column, six-row vertically suspended grid. The pieces fall straight down,
occupying the next available space within the column. The objective of the game is to be the
first to form a horizontal, vertical, or diagonal line of five of one's own discs.

The implementation is a server-client communication
Communicate between the server with REST api, and webSocket.  The client can create a game or 
join a created game. When the client creates the game they will have to wait until another player
joins the game. When the player joins the game it will show a message to them to say which game they have 
joined, who they will play with and the disk type. At this time the game can start.
The server stores the games in memory.

  # How to use 

   +Setup and run the server
    
     $ git clone https://github.com/antainfernandez/fiveInARowGameClient.git
     $ cd fiveInARowGameClient
     $ ./gradlew clean build jar
     $ cd build/libs
      
      copy jar file , use like java -jar <the jar file>.jar
      
      important ! port is 8081 and end points are setup in variables change if server is in different port
 #Setup clients	
   		
   - [5-in-a-Row - Client](https://github.com/antainfernandez/fiveInARowGameClient)