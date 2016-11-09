# SmartMuseum
ID2209 Homework 1

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
