package com.example.myapplication

data class SeedFlashcard(val question: String, val answer: String, val explanation: String)

object DefaultStudyContent {
    val byTopic: Map<String, List<SeedFlashcard>> = mapOf(
        "Algorithms" to listOf(
            SeedFlashcard("What is the average case time complexity of Quick Sort?", "O(n log n)", "Quick Sort has average time complexity O(n log n)."),
            SeedFlashcard("Which algorithm repeatedly swaps adjacent out-of-order elements?", "Bubble Sort", "Bubble Sort compares neighboring items and swaps them when they are out of order."),
            SeedFlashcard("What does dynamic programming use to avoid repeated work?", "Memoization", "Memoization stores earlier answers so the same subproblem is not recalculated."),
            SeedFlashcard("Which data structure is commonly used by breadth-first search?", "Queue", "Breadth-first search explores level by level using a queue."),
            SeedFlashcard("Depth-first search is commonly implemented with which structure?", "Stack or recursion", "Depth-first search follows one path using a stack, often through recursion."),
            SeedFlashcard("Dijkstra’s algorithm finds shortest paths when edge weights are…", "Non-negative weights", "Dijkstra’s algorithm assumes edge weights are non-negative."),
            SeedFlashcard("Binary search requires data to be…", "Sorted", "Binary search halves the search range and therefore needs sorted data."),
            SeedFlashcard("What is the time complexity of Merge Sort?", "O(n log n)", "Merge Sort divides and merges in O(n log n) time."),
            SeedFlashcard("In a max heap, which value is at the root?", "The maximum value", "A max heap keeps its largest value at the root."),
            SeedFlashcard("What makes a sorting algorithm stable?", "Equal items keep their original relative order", "A stable sort preserves the relative order of equal items."),
            SeedFlashcard("Big O notation describes an algorithm’s…", "Growth rate as input grows", "Big O describes how an algorithm’s resource use grows with input size."),
            SeedFlashcard("What is the purpose of a recursion base case?", "To stop recursion", "The base case ends recursive calls and prevents infinite recursion."),
            SeedFlashcard("Dynamic programming is especially useful when subproblems…", "Overlap and have optimal substructure", "Dynamic programming helps when subproblems overlap and have optimal substructure."),
            SeedFlashcard("What does Selection Sort find on each pass?", "The next minimum item", "Selection Sort finds the next minimum item on each pass."),
            SeedFlashcard("Insertion Sort grows which part of the collection?", "A sorted prefix", "Insertion Sort inserts each new item into the sorted prefix."),
            SeedFlashcard("A graph is made up of vertices and…", "Edges", "A graph consists of vertices connected by edges."),
            SeedFlashcard("Which graph representation is often compact for sparse graphs?", "Adjacency list", "An adjacency list stores only existing edges, which suits sparse graphs."),
            SeedFlashcard("What is the average lookup time in a hash table?", "O(1)", "Hash tables provide average O(1) lookup with a suitable hash function."),
            SeedFlashcard("What is Quick Sort’s worst-case time complexity?", "O(n²)", "Quick Sort can degrade to O(n²) when partitions are repeatedly unbalanced."),
            SeedFlashcard("Topological ordering is defined for which kind of graph?", "A directed acyclic graph", "Topological order applies to directed acyclic graphs (DAGs)."),
            SeedFlashcard("A greedy algorithm makes which kind of choice at each step?", "A locally best choice", "Greedy algorithms choose the best local option at each step."),
            SeedFlashcard("Which principle describes a stack?", "Last in, first out", "A stack follows last in, first out (LIFO)."),
            SeedFlashcard("Which principle describes a queue?", "First in, first out", "A queue follows first in, first out (FIFO)."),
            SeedFlashcard("In an unweighted graph, which algorithm finds a shortest path?", "Breadth-first search", "Breadth-first search finds shortest paths in an unweighted graph."),
        ),
        "Databases" to listOf(
            SeedFlashcard("Which key uniquely identifies a row in a table?", "Primary key", "A primary key uniquely identifies every row."),
            SeedFlashcard("What does a foreign key reference?", "Another table’s primary key", "A foreign key links a row to a key in another table."),
            SeedFlashcard("What does First Normal Form require?", "Atomic column values", "1NF stores one atomic value in each cell and removes repeating groups."),
            SeedFlashcard("What issue does normalization mainly reduce?", "Data duplication and update anomalies", "Normalization organizes related data to reduce duplication and anomalies."),
            SeedFlashcard("Which ACID property means a transaction is all-or-nothing?", "Atomicity", "Atomicity ensures a transaction fully succeeds or has no effect."),
            SeedFlashcard("Which SQL clause filters grouped results?", "HAVING", "HAVING filters groups after aggregation; WHERE filters rows before grouping."),
            SeedFlashcard("What does an INNER JOIN return?", "Rows with matching join values", "INNER JOIN keeps records with matching values in both joined tables."),
            SeedFlashcard("What is a database index designed to improve?", "Query lookup speed", "Indexes speed up reads, while using extra storage and write work."),
            SeedFlashcard("What does transaction isolation protect against?", "Interference between concurrent transactions", "Isolation controls how concurrent transactions see each other’s changes."),
            SeedFlashcard("Which SQL command changes existing rows?", "UPDATE", "UPDATE modifies values in existing rows."),
        ),
        "Networks" to listOf(
            SeedFlashcard("Which protocol provides reliable, ordered delivery?", "TCP", "TCP establishes a connection and retransmits lost data to provide ordered delivery."),
            SeedFlashcard("What does DNS resolve?", "Domain names to IP addresses", "DNS translates human-readable host names into IP addresses."),
            SeedFlashcard("Which protocol is connectionless and low overhead?", "UDP", "UDP sends datagrams without a connection handshake or delivery guarantee."),
            SeedFlashcard("What does a router do?", "Forwards packets between networks", "Routers forward packets between different IP networks."),
            SeedFlashcard("What does HTTPS add to HTTP?", "TLS encryption and authentication", "HTTPS protects HTTP traffic with TLS encryption and server authentication."),
            SeedFlashcard("What does DHCP commonly provide to a device?", "An IP configuration lease", "DHCP can assign an IP address, subnet mask, gateway, and DNS settings."),
            SeedFlashcard("What does bandwidth describe?", "Link data-carrying capacity", "Bandwidth is the capacity of a link; latency is the delay."),
            SeedFlashcard("What is a subnet mask used to identify?", "Network and host portions of an IP address", "A subnet mask separates the network bits from the host bits."),
            SeedFlashcard("Which layer routes IP packets in the OSI model?", "Network", "The Network layer handles logical addressing and routing."),
            SeedFlashcard("What is latency?", "The delay for data to travel", "Latency measures delay, commonly round-trip time."),
        ),
        "Operating Systems" to listOf(
            SeedFlashcard("What is a process?", "A program in execution with managed resources", "A process is a running program with its own execution state and resources."),
            SeedFlashcard("What is a thread?", "A unit of execution within a process", "Threads execute within a process and share its address space."),
            SeedFlashcard("What does a CPU scheduler choose?", "Which ready task runs next", "The scheduler selects a ready process or thread to use the CPU."),
            SeedFlashcard("What is a context switch?", "Saving one task state and restoring another", "A context switch changes the running task by saving and restoring execution state."),
            SeedFlashcard("What is virtual memory?", "An address-space abstraction backed by memory and storage", "Virtual memory provides processes an address space backed by RAM and, when needed, storage."),
            SeedFlashcard("What does a semaphore help coordinate?", "Access to shared resources", "Semaphores coordinate access and signaling between concurrent tasks."),
            SeedFlashcard("What causes a race condition?", "Outcome depends on unsynchronized timing", "A race occurs when concurrent operations access shared state without safe coordination."),
            SeedFlashcard("What is a system call?", "A request from a program to the OS", "System calls let programs request services from the operating system."),
        )
    )
}

