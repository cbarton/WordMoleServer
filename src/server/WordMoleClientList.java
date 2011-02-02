/*
 * WordMoleClientList.java
 * Copyright (C) 2010  Chris Barton
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *   
 *   Questions/Comments: c.chris.b@gmail.com
 *   WordMole is available free at http://wordmole.sourceforge.net/
 */

package server;

import java.util.ArrayList;
import java.util.Iterator;


import game.multiplayer.containers.Client;
import game.multiplayer.interfaces.WordMoleClient;

public class WordMoleClientList {
	private ArrayList<Client> clients;
	private ArrayList<WordMoleClient> wmClients;
	
	public WordMoleClientList(){
		clients = new ArrayList<Client>();
		wmClients = new ArrayList<WordMoleClient>();
	}
	
	/**
	 * Constructor
	 * @param client - Creates a new WordMoleClientList with element c
	 * @param name - Name of the first WordMoleClient
	 */
	public WordMoleClientList(WordMoleClient client, Client name){
		clients = new ArrayList<Client>();
		wmClients = new ArrayList<WordMoleClient>();
		add(client, name);
	}
	
	/**
	 * Adds a WordMoleClient with username to the list.
	 * @param client - WordMoleClient to add.
	 * @param name - Client of the WordMoleClient
	 */
	public synchronized void add(WordMoleClient client, Client name){
		clients.add(name);
		wmClients.add(client);
	}
	
	/**
	 * Updates the client in the List
	 * @param client - Client to be updated
	 */
	public synchronized boolean update(Client client){
		int idx = clients.indexOf(client);
		if ( idx >= 0 ){
			clients.set(idx, client);
			return true;
		} else return false;
	}
	
	/**
	 * Removes a WordMoleClient from the system.
	 * @param client - Client of the WordMoleClient
	 */
	public synchronized void remove(Client client){
		int idx = clients.indexOf(client);
		
		if ( idx >= 0 ){
			clients.remove(idx);
			wmClients.remove(idx);
		}
	}
	
	/**
	 * Returns the WordMoleClient with client.
	 * @param client - Client of the desired WordMoleClient
	 * @return WordMoleClient
	 */
	public synchronized WordMoleClient get(Client client){
		int idx = clients.indexOf(client);
		
		return (idx >= 0) ? wmClients.get(idx) : null;
	}
	
	/**
	 * Returns the Client with WordMoleClient.
	 * @param client - WordMoleClient of the desired Client
	 * @return Client
	 */
	public synchronized Client get(WordMoleClient client){
		int idx = wmClients.indexOf(client);
		
		return (idx >= 0) ? clients.get(idx) : null;
	}
	
	/**
	 * Returns an iterator over the elements in this collection.
	 * There are no guarantees concerning the order in which the elements are returned .
	 * @return Iterator<WordMoleClient>
	 */
	public synchronized Iterator<WordMoleClient> iterator(){
		return wmClients.iterator();
	}
	
	public Client[] clients(){
		return clients.toArray(new Client[0]);
	}
}
