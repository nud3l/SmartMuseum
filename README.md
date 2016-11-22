# SmartMuseum
# Homework 3
## Task 1
4 x 4 example (N = 4)
1. Register queens in the game: Queen0, Queen1, Queen2, Queen3
2. Queen0
  - filled positions (array[])
    - (2, 0) -> safe
    - Send message to Queen1 (array[(2,0)])
3. Queen1
  - ACLMessage (array[(2,0)])
  - Strategy (0,1)
  - save or not (not on row, column, diagonal)
  - Send message to Queen2 (array[(2,0),(0,1)])
4. Queen2
  - ACLMessage (array[(2,0),(0,1)])
  - find position (0,2) -> not safe
  - find position (3,2) -> safe
  - Send message to Queen3 (array[(2,0),(0,1),(3,2)])
5. Queen3
  - ACLMessage (array[(2,0),(0,1),(3,2)])
  - find position (1,3) -> safe
  - Final (array[(2,0),(0,1),(3,2),(1,3)])


In any case, where we don't find a safe position
- Message to previous queen to move

## Task 2
- ControllerAgent and ControllerAgentGUI
  - Issue clone/move commands
  - Create containers
  - ControllerAgent -> Mobile Agent
    - move -> doMove, beforeMove, afterMove
    - clone -> doClone, beforeClone, afterClone
  - Implement GUI
- Mobile Agents
  - MobileAgent
  - MobileAgentGUI
  - Adjust ArtistManager (Auctioneer)
  - Adjust Curator (Bidder)
  - Implement doMove, beforeMove, ..., auctionLogic
  - Implement GUI
- Main Container: ControllerAgent
- Container 1: ArtistManager (clones 2)
  - One clones moves to container 2 and the other to 3
- Container 2: Curator (clones 2)
- Container 3: Curator (clones 2)
- Conduct auction in both containers
- Clones return to container 1 and decide on best price

# Homework 2
## Task 1
### [Auctioneer] -> Agent
- Parameters
 - Initial price
 - Artwork name
 - Reserve price
 - Rate of reduction
- Responsibilities
 1. Search for "Bidding" service -> is of bidder Agent
 2. ACLMessage INFORM
 3. Behaviour -> CFP -> Reserve price
   1. Initial price
   2. Modified price
 4. Behaviour
   - Handle propose messages (modify price, rate of reduction)
   - Handle not udnerstood messages
   - Send ACCEPT_PROPOSAL
   - Send REJECT_PROPOSAL


 ### [Bidder] -> Agent
 1. Register service in DF
 2. Receive INFORM
 3. Behaviour
   - Handle CFP
   - Handle accepted proposal
   - Handle rejected proposal
 - Handle two strategies

2 scenarios with at least 4 agents

## Task 2
Agents = {Profiler, Curator, ArtistManager}

Outcomes
- High quality
- Low quality

Actions
- ArtistManager
  - Sell high
  - Sell low
- Curator
  - Quote based on demand
  - Quote based on interest
- Profiler
  - view
  - not view

Each combination of actions:
{ArtistManager, Curator, Profiler} eg.:
{Sell high quality, Quote price on demand, view}
- develop preferences
 - Payoff matrix


# Homework 1
## Tour Guide Agent
1. Register "virtual tour" service
2. Add behaviour to listen for requests from profiler agent
  - matching interest
3. Add behaviour that builds virtual tour for profiler
  - Communicate with curator to get list of artifacts
	- Send and receive
  Interests: 1 Flower, 2 Portrait, etc.

## Curator Agent
1. Retrieve requests from virtual tour guide agent
	- send list of artifacts, e.g. {"Painting 1", "Flowers", "..."}
2. Retrieve requests from profiler agent
	- Detailed: id, name, creator, date, type, ..

## Profiler Agent
1. Read arguments from user
	- Interests and other info
2. Subscribe to the "DF" -> to use "virtual_tour" service
3. (One or all - parallel behaviour)
  - Search for a virtual tour guide agent (DF)
  - You might not find the agent quickly
    - Keep searching
  - Send requests to one or more virtual tour guide agent
  - Receive replies from one or more virtual tour guide agents
  - Send response to the accepted virtual tour guide agent
  - Receive the contents of the virtual tour
  - Communicate with curator to get details about an artifact
  - Receive reply from curator

UT Agent|--register-->| DF ( virtual_tour) |<--subscribe--|Profile|-->A1,A2,A3
