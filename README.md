# &nbsp; &nbsp; &nbsp;&nbsp; &nbsp; &nbsp; &nbsp;&nbsp; &nbsp;&nbsp;Akka based simulation of Chord, a cloud overlay algorithm
#### Course Project - CS 441 &nbsp; &nbsp; &nbsp;  &nbsp; &nbsp; &nbsp;  &nbsp; &nbsp; &nbsp;  &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;&nbsp; &nbsp; &nbsp;&nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; Team : Ashesh Singh, Ajith Nair, Karan Raghani

The aim of the project is to design and implement a cloud simulator that uses the Chord algorithm for distribution of work in the datacenters. **Chord** is a protocol and algorithm for a peer-to-peer **distributed hash table**. Chord provides support for just one operation i.e. given a key, it maps the key on to a node. Data location can be easily implemented on top of Chord by associating a key with each data item , and storing the key/data pair at the node to which the key maps. Chord adapts efficiently as nodes join and leave the system. 

Chord uses **consistent hashing** to assign Nodes and Keys identifiers. Consistent hashing is integral to the robustness and performance of Chord because both keys and nodes are uniformly distributed in the same identifier space with a negligible possibility of collision. Each node has a **successor** and a predecessor. The successor to a node is the next node in the identifier circle in a clockwise direction. The predecessor is counter-clockwise. If there is a node for each possible ID, the successor of node 0 is node 1, and the predecessor of node 0 is node 2^m − 1 (m is the number of bits used to represent hashed values of nodes and keys); however, normally there are "holes" in the sequence. 
### Dependencies 

Listed below are different dependencies required for this porjects:

* [Scalatest.FunSuite] - Unit testing framework for the Scala
* [slf4j] - Simple Logging Facade for Java 
* [Typesafe] - to manage configuration files
* [Akka] - Actor-based message-driven runtime, Part of Lightbend Platform

### Design
For the cloud simulation we chose a **List of Movies** as the data that will be **stored** and **fetched** by the users.
For the put request the user node will pick a Movie title from the file and pass it on to the Load Balancer. Load balancer then performs consistent hashing on the file name and direct it to one of the live computation nodes(storage units) which are present in the system. If the hash value for the file is consistent with the current node then the file is stored and the database for that unit is updated. Else a lookup is performed in the fingertable of the node to find the right successor for file and the file is passsed to that storage node present in the system. This process continues untill the file is stored. A simillar process is followed when a get request is made by the user. Once the file requested is located it is returned to the master node which in turns returns it to the user.

The system is initialized using The number of users, storage nodes and read/write ratio provided by the user. Once the system is initialized, a storage node can leave and join the system. Stabelization is implemented to facilitate node leaving and node joining characterstics. 

### Implementation
The **Users** are implemented as **Client Actors** in the system.
Client Actor passes work

The **Load Balancer** is implemented as **Master Actor** in the system.
takes req from clinet and hash it

The **Computation Nodes(storage nodes)** are implemented as **Workers Actor** in the system.
performs the requested computation

The **Finger Table** is implemented as **Find Successor**

**TO-DO ----- WRITE THE MESSAGES USE IN MESSAGE PASSING**

### Execution Details
To execute the program:
1. Clone the repo on your system
2. Create the fat jar of the the project
```
sbt clean compile assembly
```
3. Now to execute the program, 
```
start-all.sh
```
4. Then type the following command to run the spark application using YARN 
```
spark-submit --class MonteCarloSim --master yarn --deploy-mode client jarfile /output
```
#### Execution Results
+ DataFrame 1: Once we fetch the data of a stock, dataframe simmilar to below one is created.
```
+-------------------+------+---------+------------------+
|          timestamp| close|LastClose|            Change|
+-------------------+------+---------+------------------+
|2019-07-08 00:00:00|134.84|     null|              null|
|2019-07-09 00:00:00|134.29|   134.84|0.6911056438734419|
|2019-07-10 00:00:00|132.64|   134.29|  0.68698481114936|
|2019-07-11 00:00:00|133.96|   132.64|0.6981107163576235|
|2019-07-12 00:00:00|138.36|   133.96|0.7094365974590054|
|2019-07-15 00:00:00|139.64|   138.36| 0.697762129621669|
|2019-07-16 00:00:00|139.09|   139.64|0.6911758890844956|
|2019-07-17 00:00:00|135.73|   139.09|0.6809951322845895|
|2019-07-18 00:00:00|134.89|   135.73|0.6900480045256244|
|2019-07-19 00:00:00|136.23|   134.89|0.6981018958172076|
|2019-07-22 00:00:00|135.24|   136.23|0.6895070022531822|
|2019-07-23 00:00:00| 138.1|   135.24|0.7036654636969691|
|2019-07-24 00:00:00|131.91|    138.1|0.6704809349343113|
|2019-07-25 00:00:00|134.71|   131.91|0.7037045518247025|
|2019-07-26 00:00:00|132.92|   134.71|0.6864811098833251|
|2019-07-29 00:00:00|134.46|   132.92| 0.698923424068583|
|2019-07-30 00:00:00|132.95|   134.46|0.6875163040855118|
|2019-07-31 00:00:00|131.67|   132.95|0.6883217169252361|
|2019-08-01 00:00:00|126.79|   131.67|0.6744421507287931|
|2019-08-02 00:00:00|124.54|   126.79|0.6842346421125712|
+-------------------+------+---------+------------------+
```
+ The final Profit values evaluated are stored in a Dataframe with Stock name and max Profit that can be made in each.

```
+------+------------------+
|Stocks|            Profit|
+------+------------------+
|   CAT| 99.94521614697946|
|  CSCO|13.540697302137378|
|  EBAY| 7.102708247072532|
|  FAST|6.3617879637359245|
+------+------------------+
```
### Running the Test Cases:

To run the test cases please input the following command on terminal after cloning the repo 
```
sbt clean compile test
```
The test cases runs as follows:
```
[info] MonteCarloTest:
[info] - Able to Load Configuration
[info] - Test for API to fetch Data 
[info] - Profit Calculated correctness
[info] - Each stock has an associated investment
[info] - Correct Stock Symbol is used
[info] Run completed in 10 seconds, 389 milliseconds.
[info] Total number of tests run: 5
[info] Suites: completed 1, aborted 0
[info] Tests: succeeded 5, failed 0, canceled 0, ignored 0, pending 0
[info] All tests passed.
[success] Total time: 37 s, completed Nov 25, 2019 11:12:29 PM
```
#### Result Analysis
#### Docker Implementation
Please Refer to the embeded youtube link for details regarding deployment using Docker

[EMR deployment video](https://www.youtube.com/user/kabcdefghijkl/videos?view=0&sort=dd&shelf_id=0 "title")
