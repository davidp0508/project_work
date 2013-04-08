%% @author David
%% @doc @todo Add description to message_passing.
%% @author David Pearson
%% @doc @todo Add description to token_ring.
%% This module inspired by trigonakis.com's Intro to Erlang: Message Passing segment

%%To Do -- Receiver code, additional uni/multi capabilities, how to integrate Han's code.

-module(message_passing).
-export([unicastSend/1, multicastSend/1, recvMsg/1, start/1]).


unicastSend({Name, Node, Payload}) ->
	io:format("Passed in name ~p, node ~p, and payload ~p~n", [Name, Node, Payload]),
	{Name,Node}!{Payload,node()},
	io:format("successfully sent message to ~p (~p)~n", [Name, Node]).%,
	%case Payload of
	%	ack -> io:format("sent ACK!~n", []), Return = ack; %we're sending an ACK, so we can safely return the fact that we got an ack
	%	_ -> io:format("blah!~n", []), Return = Payload %need to get the response, not what I'm sending...
	%end,
	%Return.
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
														%io:format("NNI: ~p~n", [NextNodeName]), 
														multicastSend({NextNodeName, Node, Payload, UserList});
												_ -> io:format("") %need to get rid of the printout
											end;
		false -> io:format("failed to find user ~p~n", [Name]);
		Values -> io:format("Failed and found ~p~n", [Values])
 	end.


recvMsg(NodeList) ->
	%Will need to be modified based on what we need to do at Erlang's level. 
	io:format("In recvMsg()~n", []),
	receive
		{Payload, FromName} ->        
		io:format("Got message ~p from ~p!~n", [Payload, FromName]),
		io:format("Node list is ~p~n", [NodeList]),	%here we should check to see if we have a monitor connection to this node, and make one if not...if sender_server, check for the src@serverIP in the list (create as separate function)
		case Payload of %here we can add a bunch of cases for tokens, etc...
			{_, _, ack, _} -> io:format("Got an ack; done sending.~n", []),
				   recvMsg(NodeList);
			{Sender, _, _, _} -> [_|IP] = re:split(atom_to_list(FromName), "@", [{return, list}]), %extract the IP of the sender
%% 								io:format("Sender ~p from IP ~p~n", [Sender,list_to_atom(IP)]),
								 [IP_raw|_] = IP, %have to do this to make it a list, even though it looks like a list now...
				 unicastSend({Sender, list_to_atom(lists:concat([Sender, '@', list_to_atom(IP_raw)])), {node(), Sender, ack, 1}}),
				 unicastSend({sender_server, FromName, {self(), sender_server, ack, 1}}), %need this line (right now) to make it respond and stop waiting 
				 %Logic will need to be added to make the payload dynamic instead of hard-coded
				 recvMsg(NodeList);
			_ -> io:format("Message does not match an expected format~n", []) %shouldn't happen unless the message is malformed
		end;
		Message -> io:format("Hello World!~n", []) %dummy clause
	end,
	otherthing.


start(Name) ->
	io:format("received name ~p~n", [Name]),
	NodeList = [Name], %add self to global node list to monitor who is alive (work in progress)
	case register(Name, spawn(message_passing, recvMsg, [NodeList])) of
		true -> %yes -> 
			io:format("Successful add~n");
		false -> %no -> 
			io:format("Unable to add ~p~n", [Name])
	end.