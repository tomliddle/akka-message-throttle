
Limits work done by implementing a throttle on messages sent to a specified actor.

Messages are sent to the specified actor at the start of the time period.

With a workAmount of 1 and a smaller time period, this can even out the delivery of messages

The limits to the number of queued work messages are the limits to immutable.Queue not the Akka message queue.



Further work

Allowing functionality for flow control or back-pressure, potentially in feedback messages

Routing to a number of target actors rather than one