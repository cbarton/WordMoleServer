/*
 * InvitationImpl.java
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
package game.multiplayer.invitation;

import server.WordMoleClientList;
import server.WordMoleServerImpl;
import server.callbacks.AsyncCallback;
import server.callbacks.Callback;
import server.callbacks.Notifier;
import game.multiplayer.containers.Client;
import game.multiplayer.containers.Game;
import game.multiplayer.containers.Invite;
import game.multiplayer.interfaces.Invitation;
import game.multiplayer.interfaces.WordMoleClient;
import game.multiplayer.interfaces.WordMoleServer;
import game.states.PlayerState;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;
import java.util.Vector;

/**
 * This class represents the Invitation Implementation of the Remote Interface for the 
 * WordMoleServer.
 * @author Chris Barton
 */
public class InvitationImpl extends UnicastRemoteObject implements Invitation{
	private static final long serialVersionUID = -3701348241375867326L;
	
	private WordMoleClientList clientList;
	private WordMoleClient host;
	private WordMoleServer server;
	private Invite thisInvite;
	private Vector<Client> acceptedClients;
	private AsyncCallback clientCallback;
	private int clientsLeftToVote;
	
	/**
	 * Constructor
	 * @param invite - Invite that this server is based on.
	 * @param server - WordMoleServer main server
	 * @param callback - AsyncCallback
	 * @throws RemoteException 
	 */
	public InvitationImpl(Invite invite, WordMoleServer server, AsyncCallback callback) throws RemoteException{
		super();
		this.server = server;
		thisInvite = invite;
		clientCallback = callback;
		clientsLeftToVote = invite.getNumInvitees();
		
		//register for async callback
	      clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	          	setupInvitation();
	            n.resetCallbackTime();
	         }
	      });
	}
	
	/**
	 * Sets up the Invitation.
	 */
	public void setupInvitation(){
		clientList = new WordMoleClientList();
		acceptedClients = new Vector<Client>();
		
		host = ((WordMoleServerImpl) server).getClient(thisInvite.getHost());
		updateClientState(thisInvite.getHost(), PlayerState.WAITING);
		
		acceptedClients.add(thisInvite.getHost());

		for ( Client invitee : thisInvite.getInvitees() ){
			WordMoleClient client = ((WordMoleServerImpl) server).getClient(invitee);
			
			try{
				client.receiveInvite(this);
				clientList.add(client, invitee);
				updateClientState(invitee, PlayerState.WAITING);
			} catch(RemoteException re){
				try {
					// If server cannot reach client, then its an auto-reject.
					makeDecision(invitee, false);
					server.register(client, invitee, false);
				} catch (RemoteException e) {
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
	
	public void initializeGame(final boolean initialize) throws RemoteException {
		//register for async callback
	      clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	          	gameTime(initialize);
	            n.resetCallbackTime();
	         }
	      });
	}
	
	public void gameTime(boolean init){
		WordMoleClient c = null;
		try{
			if ( !init ){
				updateClientState(thisInvite.getHost(), PlayerState.CONNECTED);
				for ( Iterator<WordMoleClient> it = clientList.iterator(); it.hasNext(); ){
					c = it.next();
					c.cancelInvite();
					updateClientState(clientList.get(c), PlayerState.CONNECTED);
				}
			}else{
				Game game = new Game(acceptedClients.toArray(new Client[0]));
				server.createGame(game);
			}
		} catch (RemoteException re){
			if ( c != null )
				try {
					server.register(c, clientList.get(c), false);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
		}
	}

	public synchronized void makeDecision(final Client client, boolean decision) throws RemoteException {
		if ( decision )
			acceptedClients.add(client);
		else{
			//register for async callback
		      clientCallback.doCallback(new Callback() {
		         public void executeCallback(Notifier n, Object arg) {
		          	updateClientState(client, PlayerState.CONNECTED);
		            n.resetCallbackTime();
		         }
		      });
		}
		clientsLeftToVote--;
		host.receiveInviteDecision(client, decision);
	}
	
	public Invite getInvite() throws RemoteException{
		return thisInvite;
	}
	
	public boolean ready() throws RemoteException{
		return clientsLeftToVote <= 0;
	}	
}
