zmq-protobuf-queue 
==================
 
zmq-protobuf-queue is an Java implementation of zeromq queues that transports google protobuf messages. 
I committed this project to Github as I found a lot of examples of zeromq and protobuf RPC implementations here on Github,
but no examples for zeromq publisher-subscriber nor pusher-pull (distributed task queue) models.  

You can find examples of each queue model (RPC, pub-sub, push-pull) in the example directory. zmq is a light-weight, 
brokerless queuing solution, compared to its counterparts such as RabbitMQ or ActiveMQ. It is my hope that my project 
can shed light for people looking for a more OOP and service-oriented architecture usage of zmq in Java.
Please feel free to look up my implementation and adapt it to your use. 


For the impatient
=================

To build and run zmq-protobuf-queue, download and unzip the source code, go to this
directory at the command line, and type::

    ant compile
    ant run-rpc
    ant run-pubsub
    ant run-taskqueue

This will in term compile the source code, run the rpc example, which spawns a RPC server that serves a "HelloService" 
and a RPC client that makes a "HelloService" call to the RPC server; a pub-sub example, which spawns a Weather publisher
service that broadcasts weather periodically and a Weather client that listens to the updates; a task queue example, 
which spawns a WordCount master node that submits documents to a group WordCount worker nodes to count the resulting words
and then tally up the total sum of words amongst all documents.


Dependencies
============

Aside from needing a Java 7+ JDK, you also need to have zmq binding, jzmq binding, google-protobuf installed.
Also, you need ant builder if you would like to run the examples from the included ant build scripts.

- zmq, see http://zeromq.org/area:download (required)
  The original zmq binding.

- jzmq, see https://github.com/zeromq/jzmq (required)
  Java language binding for libzmq.

- google-protobuf, see https://developers.google.com/protocol-buffers/ (required)
  Google protocol buffers.

- ant, see http://ant.apache.org/manual/install.html (optional)
  Apache Ant that can run the build scripts to compile the source code and run the examples.

Installation
============

First, **make sure that all dependencies are installed correctly**. Second, verify that your local library path 
contains libjzmq.a and libzmq.a and libprobuf.a. The local library path is assumed to be the default POSIX path
/usr/local/lib, if on Windows machine or your specific distro has a different path, you can edit the jzmq variable
in the root build.xml to point to the right local library path. 

Contributing, Patching
=========================

Feel free to fork, submit a pull request or copy the all the code shown here. 


Distribution Structure
======================

- README     		-- This file.
- src/base/        	-- The base classes.
- src/examples/     -- Code for examples that demonstrates the use of zmq's different queue models.
- src/messaging     -- Protobuf service scaffolding.
- src/pubsub    	-- Pubsub implementation.
- src/rpc    		-- RPC implementation.
- src/taskqueue   	-- Task queue (push-pull) implementation.
- lib				-- Libarary of external java jar dependencies.
- proto				-- Protobuf service definitions.