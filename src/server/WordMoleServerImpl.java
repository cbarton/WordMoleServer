/*
 * WordMoleServerImpl.java
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

import game.multiplayer.containers.Client;
import game.multiplayer.containers.Game;
import game.multiplayer.containers.Invite;
import game.multiplayer.game.GameImpl;
import game.multiplayer.interfaces.Invitation;
import game.multiplayer.interfaces.WordMoleClient;
import game.multiplayer.interfaces.WordMoleServer;
import game.multiplayer.invitation.InvitationImpl;
import game.states.PlayerState;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.*;

import logger.Logger;

import server.callbacks.AsyncCallback;
import server.callbacks.Callback;
import server.callbacks.Notifier;

/**
 * This class represents the WordMoleServer Implementation of the Remote Interface for the 
 * WordMoleServer.
 * @author Chris Barton
 */
@SuppressWarnings("serial")
public class WordMoleServerImpl extends JFrame implements WordMoleServer{
	private WordMoleClientList clientList;
	private DefaultListModel playerListModel;
	private AsyncCallback clientCallback = new AsyncCallback(Thread.NORM_PRIORITY-1, 3);
	private Logger logger = new Logger(this);
	
	public WordMoleServerImpl(){
		super("Word Mole Server");
		setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
						
		JPanel northPnl = new JPanel();
		northPnl.setLayout(new BoxLayout(northPnl, BoxLayout.X_AXIS));
		JLabel header = new JLabel("Word Mole Server Platform");
		northPnl.add(header);
		
		playerListModel = new DefaultListModel();
		JList playerList = new JList(playerListModel){
			public void paintComponent(Graphics g){
				BufferedImage img = null;
				Graphics2D g2d = (Graphics2D) g;
				
				super.paintComponent(g);
				
				// Give it an outline
				g.setColor(Color.BLACK);
				g.drawRect(0, 0, getWidth()-1, getHeight()-1);
				
				// Give it the back image.
				try {
					img = ImageIO.read(getClass().getResource(("images/icon.png")));
					
					BufferedImage bfimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TRANSLUCENT);
					Graphics2D bfimgG = bfimg.createGraphics();
					bfimgG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)); // Overlay the transparency
					bfimgG.drawImage(img, null, 0, 0); // Draw it on the buffered image
					g2d.drawImage(bfimg, null, getWidth()/2-img.getWidth(null)+19, getHeight()/2-img.getHeight(null)); // Draw it on the component (centered)
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		playerList.setMaximumSize(new Dimension(300, 400 ));
	
		add(northPnl);
		add(playerList);
		
		clientList = new WordMoleClientList();
		
		addWindowListener(new WindowAdapter(){
			public void windowClosed(WindowEvent e){
				clientCallback.stop();
			}
		});
		clientCallback.start();
		logger.setVisible(true);
	}
	
	public synchronized void register(WordMoleClient client, final Client name, final boolean connected) throws RemoteException {
		logger.log(name.name + " " + connected + " connected in REGISTER");
		if ( connected ){
			clientList.add(client, name);
			playerListModel.addElement(name);
		} else{
			clientList.remove(name);
			playerListModel.removeElement(name);
		}
		
		//register for async callback
	      clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	            sendClient(clientList.clients());
	            n.resetCallbackTime();
	         }
	      });
	}
	
	public synchronized void reregister(final Client client) throws RemoteException{
		logger.log(client.name + " reregistered in REREGISTER");
		client.state = PlayerState.CONNECTED;
		clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	        	updateClient(client);
	            n.resetCallbackTime();
	         }
	      });
	}
	
	public synchronized void register(Client name, boolean connected){
		logger.log(name.name + " connected in REGISTER");
		WordMoleClient client = clientList.get(name);
		if ( connected ){
			clientList.add(client, name);
			playerListModel.addElement(name);
		} else{
			clientList.remove(name);
			playerListModel.removeElement(name);
		}
		
		//register for async callback
	      clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	            sendClient(clientList.clients());
	            n.resetCallbackTime();
	         }
	      });
	}
	
	public synchronized void updateClient(Client client){
		logger.log(client.name + " is now " + client.state);
		if ( clientList.update(client) )
			sendClient(clientList.clients());
		else
			register(client, false);
	}
	
	/**
	 * Sends out the clients to update the playerList on the WordMoleClient
	 * @param clients - Client[] - all Clients.
	 */
	public synchronized void sendClient(Client [] clients){
		WordMoleClient currentClient = null;
		try{
			for ( Iterator<WordMoleClient> it = clientList.iterator(); it.hasNext(); ){
				currentClient = it.next();
				currentClient.updateClientList(clients);
			}
		}catch (RemoteException re){
			try {
				register(currentClient, clientList.get(currentClient), false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Invitation createInvite(Invite invite) throws RemoteException {
		logger.log(invite.getHost().name + " created an invite in CREATEINVITE");
		InvitationImpl ii = new InvitationImpl(invite, this, clientCallback);
		return ii;
	}

	//TODO add in the word mole client list if the client has invite? or has game?
	public synchronized void createGame(Game game) throws RemoteException {
		logger.log(game.getPlayers()[0].name + " has a game going in CREATEGAME");
		GameImpl gi = new GameImpl(game, this, clientCallback);
	}

	public void postMessage(final Client sender, final String message) throws RemoteException {
		logger.log(sender.name + " sent a message");
	      //register for async callback
	      clientCallback.doCallback(new Callback() {
	         public void executeCallback(Notifier n, Object arg) {
	            sendMessage(sender, message);
	            n.resetCallbackTime();
	         }
	      });		
	}
	
	/**
	 * Sends the message to everyone that is connected.
	 * @param sender - String -  Sender of the message.
	 * @param message - String - Message to be displayed.
	 */
	public synchronized void sendMessage(Client sender, String message){
		WordMoleClient currentClient = null;
		try{
			for ( Iterator<WordMoleClient> it = clientList.iterator(); it.hasNext(); ){
				currentClient = it.next();
				if ( !clientList.get(currentClient).state.equals(PlayerState.PLAYING) ){
					currentClient.displayMessage("  " + sender + ": " + message + "\n");
				}
			}
		}catch (RemoteException re){
			try {
				register(currentClient, clientList.get(currentClient), false);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the WordMoleClient associated with the username.
	 * @param client - Client - Username for the requested WordMoleClient.
	 * @return WordMoleClient corresponding to the username.
	 */
	public WordMoleClient getClient(Client client){
		return clientList.get(client);
	}

	private void bind(){
		try {
			UnicastRemoteObject.exportObject(this);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String [] args) throws RemoteException{
		WordMoleServerImpl wm = new WordMoleServerImpl();	
		//System.setSecurityManager(new RMISecurityManager());
		
		Registry reg = LocateRegistry.createRegistry(8888);
		reg.rebind("WordMoleServer", wm);
		wm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		wm.setSize(new Dimension(300,450));
		wm.setVisible(true);
		wm.bind();
	}
}
