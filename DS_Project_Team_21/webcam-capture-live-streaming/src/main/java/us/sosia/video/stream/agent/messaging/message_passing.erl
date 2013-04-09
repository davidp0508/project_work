%% @author David
%% @doc @todo Add description to message_passing.
%% @author David Pearson
%% @doc @todo Add description to token_ring.
%% This module inspired by trigonakis.com's Intro to Erlang: Message Passing segment

%%To Do -- Receiver code, additional uni/multi capabilities, how to integrate Han's code.

-module(message_passing).
-export([unicastSend/1, multicastSend/1, recvMsg/0, start/1]).
%-export([unicastSend/1, multicastSend/1, startMC/1, recvMsg/0, start/1]).


unicastSend({Name, Node, Payload}) ->
	io:format("Passed in name ~p, node ~p, and payload ~p~n", [Name, Node, Payload]),
	{Name,Node}!{Payload,node()},
	io:format("successfully sent message to ~p (~p)~n", [Name, Node]).
	%recvMsg() can't go here, b/c it doesn't return to RPC


%startMC({Name, Node, Payload}) -> no longer needed; can pass the names in as a list of tuples inside the main tuple.
	%Users = [{david, '192.168.1.44'}, {joe, '192.168.1.44'}, {local_server, '192.168.1.44'}],
	%multicastSend({Name, Node, Payload, Users}).
	%multicastSend({Name, Node, Payload}, Users).


multicastSend({Name, Node, Payload, Users}) ->
	case lists:keytake(Name, 1, Users) of
		{_, {NodeName, IP}, UserList} -> io:format("found user ~p with ip ~p~nOriginal tuple list is ~p", [NodeName, IP, Users]), 
										    unicastSend({NodeName, list_to_atom(lists:concat([NodeName, '@', IP])), Payload}),
											io:format("Data is ~p~n", [UserList]),
											case UserList =/= [] of
												true ->	[NextNodeInfo|_] = UserList,
														{NextNodeName,_} = NextNodeInfo,
														io:format("NNI: ~p~n", [NextNodeName]), 
														multicastSend({NextNodeName, Node, Payload, UserList});
												_ -> io:format("bleh") %need to get rid of the printout
											end;
		false -> io:format("failed to find user ~p~n", [Name]);
		Values -> io:format("Failed and found ~p~n", [Values])
 	end.


recvMsg() ->
	%Will need to be modified based on what we need to do at Erlang's level. 
	receive
		{Payload, FromName} ->        
		io:format("Got message ~p from ~p!~n", [Payload, FromName]),
		case Payload of %here we can add a bunch of cases for tokens, etc...
			ack -> io:format("Got an ack; done sending.~n", []), 
				   recvMsg();
			_ -> unicastSend({local_server, FromName, ack}),
				   recvMsg()
		end;
		Message -> io:format("Hello World!~n", []) %dummy clause
	%after 20000 ->
	%	done
	end.


start(Name) ->
	case register(Name, spawn(message_passing, recvMsg, [])) of
		true -> %yes -> 
			io:format("Successful add~n");
		false -> %no -> 
			io:format("Unable to add ~p~n", [Name])
	end.