/*
 * GameImpl.java
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

package game.multiplayer.game;


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import game.multiplayer.containers.Client;
import game.multiplayer.containers.Game;
import game.multiplayer.interfaces.MultiplayerGameFrame;
import game.multiplayer.interfaces.WordMoleClient;
import game.multiplayer.interfaces.WordMoleServer;
import game.states.PlayerState;
import server.WordMoleServerImpl;
import server.callbacks.AsyncCallback;
import server.callbacks.Callback;
import server.callbacks.Notifier;

/**
 * This class represents the Game Implementation of the Remote Interface for the 
 * WordMoleServer.
 * @author Chris Barton
 */
public class GameImpl extends UnicastRemoteObject implements game.multiplayer.interfaces.Game{
	private static final long serialVersionUID = -3401400218005293018L;
	
	private int num_players, players_in_session = 0;
	private boolean inSession = false;
	private Game thisGame;
	private WordMoleServer server;
	private AsyncCallback clientCallback;
	private ArrayList<Client> clientList;
	private ArrayList<MultiplayerGameFrame> multList;
	
	/**
	 * Constructor
	 * @param game - Game containing this Game.
	 * @param server - WordMoleServer
	 * @throws RemoteException 
	 */
	public GameImpl(Game game, WordMoleServer server, AsyncCallback cb) throws RemoteException{
		super();
		thisGame = game;
		this.server = server;
		num_players = game.getPlayers().length;
		clientCallback = cb;
		
		clientList = new ArrayList<Client>();
		multList = new ArrayList<MultiplayerGameFrame>();
		for ( Client player : thisGame.getPlayers() ){
			WordMoleClient client = ((WordMoleServerImpl) server).getClient(player);
			
			try{
				client.receiveGame(this);
				updateClientState(player, PlayerState.PLAYING);
			} catch(RemoteException re){
				try{
					server.register(client, player, false);
				} catch (RemoteException e){
					e.printStackTrace();
				}
			} 
		}
	}
	
	/**
	 * Updates the clients state.
	 * @param client - Client - client to be updated.
	 * @param state - PlayerState - state to be updated.
	 */
	private void updateClientState(Client client, PlayerState state){
		client.state = state;
		((WordMoleServerImpl)server).updateClient(client);
	}

	public synchronized void register(final Client client, final MultiplayerGameFrame mult, boolean connected) throws RemoteException {
		Callback cb = null;
		
		if ( !connected ){
			clientList.remove(client);
			multList.remove(mult);
			cb = new Callback(){ 
				public void executeCallback(Notifier n, Object arg){
					removePlayer(client);
					n.resetCallbackTime();
				}
			};
		} 
	
		else {
			clientList.add(client);
			multList.add(mult);
			if ( ++players_in_session == num_players ){
			inSession = true;
			cb = new Callback(){ 
				public void executeCallback(Notifier n, Object arg){
					startGame();
					n.resetCallbackTime();
				}
			};
		}
		}
		if ( cb != null ){
			clientCallback.doCallback(cb);
		}
	}
	
	/**
	 * Starts up the game.
	 */
	public void startGame(){
		for ( int i = 0; i < clientList.size(); i++ ){
			try {
				multList.get(i).startGame();
			} catch (RemoteException e) {
				try {
					register(clientList.get(i), multList.get(i), false);
					((WordMoleServerImpl)server).register(clientList.get(i), false);
				} catch (RemoteException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Removes a player from the game.
	 * @param client - Client to be removed
	 */
	public synchronized void removePlayer(Client client){
		for ( int i = 0; i < multList.size(); i++ ){
			try{
				multList.get(i).disconnectPlayer(client);
			} catch ( RemoteException re){
				try {
					register(clientList.get(i), multList.get(i), false);
					((WordMoleServerImpl)server).register(clientList.get(i), false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

	public void updateScore(final Client client, final int score) throws RemoteException {
		clientCallback.doCallback(new Callback(){
			public void executeCallback(Notifier n, Object args){
				update(client, score);
				n.resetCallbackTime();
			}
		});
	}
	
	/**
	 * Updates the client's score
	 * @param client
	 * @param score
	 */
	public synchronized void update(Client client, int score){
		for ( int i = 0; i < multList.size(); i++ ){
			try{
				multList.get(i).updateScore(client, score);
			} catch ( RemoteException re){
				try {
					register(clientList.get(i), multList.get(i), false);
					((WordMoleServerImpl)server).register(clientList.get(i), false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				
			}
		}
	}

	public synchronized void winGame(final Client winner) throws RemoteException {
		if ( inSession ){
			clientCallback.doCallback(new Callback(){
				public void executeCallback(Notifier n, Object args){
					sendWinner(winner);
					n.resetCallbackTime();
				}
			});
			inSession = false;
		}
	}
	
	public void sendWinner(Client winner){
		for ( int i = 0; i < multList.size(); i++ ){
			try{
				multList.get(i).notifyWinner(winner);
			} catch ( RemoteException re){
				try {
					register(clientList.get(i), multList.get(i), false);
					((WordMoleServerImpl)server).register(clientList.get(i), false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public Game getGame() throws RemoteException {
		return thisGame;
	}
}
