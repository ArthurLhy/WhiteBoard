package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.commons.codec.binary.Base64;

import pb.WhiteboardPeer;
import pb.WhiteboardServer;
import pb.managers.ClientManager;
import pb.managers.IOThread;
import pb.managers.PeerManager;
import pb.managers.ServerManager;
import pb.managers.endpoint.Endpoint;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version
	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	
	ClientManager clientManager;
	PeerManager peerManager;
	Endpoint epToWhiteboardServer;

	HashMap<String, ArrayList<Endpoint>> epToPeerClient=new HashMap<String, ArrayList<Endpoint>>();
	HashMap<String, ArrayList<Endpoint>> targetClient=new HashMap<String, ArrayList<Endpoint>>();
	
	/**
	 * Initialize the white board app.
	 * @throws InterruptedException 
	 * @throws UnknownHostException 
	 */
	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort){
		whiteboards=new HashMap<>();
		
		//*******************************************
		peerManager = new PeerManager(peerPort);
		
		// client to connect whiteboard server
		try {
			clientManager = peerManager.connect(whiteboardServerPort, whiteboardServerHost);
			this.peerport = whiteboardServerHost+":"+ peerPort;
			clientManager.on(PeerManager.peerStarted, (args)->{
				log.info("connecting to whiteboard server");		
				epToWhiteboardServer = (Endpoint)args[0];
				epToWhiteboardServer.on(WhiteboardServer.sharingBoard,(args2)->{
					String sharingBoard = (String) args2[0];
					if(!whiteboards.containsKey(getBoardName(sharingBoard))) {
						log.info("onSharing whiteboard: " + sharingBoard);
						addRemoteBoardToList(sharingBoard);
					}
				}).on(WhiteboardServer.unsharingBoard,(arg2)->{
					// delete the unsharing board
					String unsharingBoard = (String) arg2[0];
					deleteUnshareBoard(unsharingBoard);
				});
				//endpoint.emit(boardData, "");
			}).on(PeerManager.peerStopped, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				System.out.println("There was error while communication with peer: "
						+endpoint.getOtherEndpointId());
			});
			
			clientManager.start();
			
		} catch (UnknownHostException | InterruptedException e) {
			e.printStackTrace();
		}
		
		// peer manager to share the board and tell the white board server which board to share
		//ArrayList<Endpoint> endpointList = new ArrayList<Endpoint>();
		peerManager.on(PeerManager.peerStarted, (args)->{
			Endpoint endpoint = (Endpoint)args[0];
			if (epToPeerClient.get(selectedBoard.getName()) != null) {
				epToPeerClient.get(selectedBoard.getName()).add(endpoint);
			}
			else {
				ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
				endpoints.add(endpoint);
				epToPeerClient.put(selectedBoard.getName(), endpoints);
			}
	       	//endpointList.add(endpoint); // multiple other peers
			log.info("Connected From peer: "+endpoint.getOtherEndpointId());
			endpoint.on(getBoardData, (args2) -> {
				String boardName = (String) args2[0];
				String boardString = whiteboards.get(boardName).toString();
				endpoint.emit(boardData, boardString);
			}).on(listenBoard,(args2)->{
				String requestBoard = (String) args2[0];
				log.info("listen on board: " + requestBoard);
				endpoint.on(boardPathUpdate, (args3) -> {
					String data = (String) args3[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received updated path: " + data);
					drawSelectedWhiteboard();
					for (Endpoint end: epToPeerClient.get(boardName)) {
						end.emit(boardPathAccepted, data);
					}
				}).on(boardUndoUpdate, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received undo path: " + data);
					drawSelectedWhiteboard();
					for (Endpoint end: epToPeerClient.get(boardName)) {
						end.emit(boardUndoAccepted, data);
					}
				}).on(boardClearUpdate, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received clear board: " + data);
					drawSelectedWhiteboard();
					for (Endpoint end: epToPeerClient.get(boardName)) {
						end.emit(boardClearAccepted, data);
					}
				});
				// share the board that another peer requests
				shareRequestBoard(requestBoard, endpoint);
			}).on(boardUndoUpdate, (args1) -> {
				String data = (String) args1[0];
				String boardName = getBoardName(data);
				String boardData = getBoardData(data);
				selectedBoard.whiteboardFromString(boardName, boardData);
				log.info("Received undo path: " + data);
				drawSelectedWhiteboard();
			}).on(boardDeleted, (args1) -> {
				String unshareboard = (String) args1[0];
				log.info("Delete the board:" + unshareboard);
				deleteUnshareBoard(unshareboard);
			});
		}).on(PeerManager.peerStopped,(args)->{
	        Endpoint endpoint = (Endpoint)args[0];
	       	System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerError,(args)->{
			Endpoint endpoint = (Endpoint)args[0];
			System.out.println("There was an error communicating with the peer: "
	        			+endpoint.getOtherEndpointId());
		}).on(PeerManager.peerServerManager, (args)->{
        	ServerManager serverManager = (ServerManager)args[0];
        	serverManager.on(IOThread.ioThread, (args2)->{
        		String peerport = (String) args2[0];
            	log.info("using Internet address: "+peerport);
	        });
        });
		
		peerManager.start();
		
		//***********************************************************
		
		show(peerport);
		peerManager.joinWithClientManagers();
	}
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server 
	
	public void addRemoteBoardToList(String remoteBoardData) {
		String boardName = getBoardName(remoteBoardData);
		Whiteboard newBoard = new Whiteboard(boardName, true);
		newBoard.setShared(true);
		addBoard(newBoard, false);
	}
	
	// From whiteboard peer
	
	/**
	 * start up a client to connect to the peer which has the sharingBoard
	 * and get the sharingBoard data
	 * @param peerManager: this peer
	 * @param sharingBoard: the another peer we connect host:port:boardId
	 */
	public void createShareBoard(PeerManager peerManager,String sharingBoard) {
		String[] parts = sharingBoard.split(":", 3); // host:port:boardID
		ClientManager clientManager;
		try {
			clientManager = peerManager.connect(Integer.valueOf(parts[1]), parts[0]);
			clientManager.on(PeerManager.peerStarted, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				// request the board data
				endpoint.on(boardData,(args2)->{
					// host:port:boardid%version%PATHS
					String remoteBoardData = (String) args[0];
					addRemoteBoardToApp(remoteBoardData);
				});
			}).on(PeerManager.peerStopped, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
			}).on(PeerManager.peerError, (args)->{
				Endpoint endpoint = (Endpoint)args[0];
				System.out.println("There was error while communication with peer: "
						+endpoint.getOtherEndpointId());
			});
		} catch (NumberFormatException | UnknownHostException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addRemoteBoardToApp(String remoteBoardData) {
		String boardName = getBoardName(remoteBoardData);
		String data = getBoardData(remoteBoardData);
		Whiteboard newBoard = new Whiteboard(boardName, true);
		newBoard.whiteboardFromString(boardName, data);
		addBoard(newBoard, false);
	}


	
	/**
	 * delete the unshareboard
	 * @param sharingBoard
	 */
	public void deleteUnshareBoard(String sharingBoard) {
		log.info(sharingBoard + " is deleted");
		String[] parts = sharingBoard.split(":", 3); // host:port:boardID
		String sharedhost = parts[0];
		String sharedport = parts[1];
		try {
			if (!peerport.equals(sharedhost + ":" + sharedport)) {
				deleteBoard(sharingBoard);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Share the board if the 'share' box is checked
	 * @param requestBoard
	 * @param endpoint
	 */
	public void shareRequestBoard(String requestBoard, Endpoint endpoint) {
		
	}
	
	/**
	 * update the board that wanted to be shared to the whiteboard server
	 * @param peerManager
	 * @param whiteboardServerHost
	 * @param whiteboardServerPort
	 * @param shareBoard
	 * @throws UnknownHostException
	 * @throws InterruptedException
	 */
	public void updateShareBoard(PeerManager peerManager, String whiteboardServerHost, 
			int whiteboardServerPort, String shareBoard) throws UnknownHostException, InterruptedException {
		
	}
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		
	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard != null && epToWhiteboardServer != null && !epToPeerClient.isEmpty()) {
				if(!whiteboard.isRemote()) {
					this.epToWhiteboardServer.emit(WhiteboardServer.unshareBoard,selectedBoard.getName());
					try {
						for (Endpoint end: epToPeerClient.get(boardname)) {
							end.emit(boardDeleted, boardname);
						}
						whiteboards.remove(boardname);
					}
					catch (Exception e) {
						log.info("there is no such board" + boardname);
					}
				}
				else {
					// this.epToPeerClient.emit(unlistenBoard, selectedBoard.getName());
					whiteboards.remove(boardname);
				} 
		} 	
		else {
				whiteboards.remove(boardname);
		}
	}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */
	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				if (selectedBoard.isShared()) {
					if(!selectedBoard.isRemote() && !this.epToPeerClient.isEmpty()) {
						for (Endpoint end: epToPeerClient.get(selectedBoard.getName())) {
							end.emit(boardPathUpdate,selectedBoard.toString());
						}
					}
					if(selectedBoard.isRemote() && !this.targetClient.isEmpty()) {
						for (Endpoint end: targetClient.get(selectedBoard.getName())) {
							end.emit(boardPathUpdate,selectedBoard.toString());
						}
					}
				}
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				if (selectedBoard.isShared()) {
					if(!selectedBoard.isRemote() && !this.epToPeerClient.isEmpty()) {
						for (Endpoint end: epToPeerClient.get(selectedBoard.getName())) {
							end.emit(boardClearUpdate,selectedBoard.toString());
						}
					}
					if(selectedBoard.isRemote() && !this.targetClient.isEmpty()) {
						for (Endpoint end: targetClient.get(selectedBoard.getName())) {
							end.emit(boardClearUpdate, selectedBoard.toString());
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				if (selectedBoard.isShared()) {
					if(!selectedBoard.isRemote() && !this.epToPeerClient.isEmpty()) {
						for (Endpoint end: epToPeerClient.get(selectedBoard.getName())) {
							end.emit(boardUndoUpdate,selectedBoard.toString());
						}
					}
					if(selectedBoard.isRemote() && !this.targetClient.isEmpty()) {
						for (Endpoint end: targetClient.get(selectedBoard.getName())) {
							end.emit(boardUndoUpdate, selectedBoard.toString());
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
		if(selectedBoard.isRemote()) {

			String[] parts = selectedBoard.getName().split(":",3);
			ClientManager clientManager;
			try {
				clientManager = peerManager.connect(Integer.valueOf(parts[1]),parts[0]);
			} catch (NumberFormatException e) {
				System.out.println("Port is not a number: "+parts[1]);
				return;
			} catch (UnknownHostException e) {
				System.out.println("Could not find the peer IP address: "+parts[0]);
				return;
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
				return;
			}

			clientManager.on(PeerManager.peerStarted, (args) -> {
				Endpoint endpoint = (Endpoint) args[0];
				if (targetClient.get(selectedBoard.getName()) != null) {
					targetClient.get(selectedBoard.getName()).add(endpoint);
				}
				else {
					ArrayList<Endpoint> endpoints = new ArrayList<Endpoint>();
					endpoints.add(endpoint);
					targetClient.put(selectedBoard.getName(), endpoints);
				}
				endpoint.emit(getBoardData, selectedBoard.getName());
				endpoint.on(boardData, (args1) -> {
					// host:port:boardid%version%PATHS
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received board data: " + data);
					drawSelectedWhiteboard();
					endpoint.emit(listenBoard, boardName);
				}).on(boardPathUpdate, (args1) ->{
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received updated path: " + data);
					drawSelectedWhiteboard();
				}).on(boardUndoUpdate, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received undo path: " + data);
					drawSelectedWhiteboard();
				}).on(boardDeleted, (args1) -> {
					String unshareboard = (String) args1[0];
					deleteUnshareBoard(unshareboard);
					endpoint.emit(unlistenBoard, unshareboard);
				}).on(boardClearUpdate, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("Received clear board: " + data);
					drawSelectedWhiteboard();
				}).on(boardClearAccepted, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("acc clear board: " + data);
					drawSelectedWhiteboard();
				}).on(boardUndoAccepted, (args1) -> {
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("acc undo path: " + data);
					drawSelectedWhiteboard();
				}).on(boardPathAccepted, (args1) ->{
					String data = (String) args1[0];
					String boardName = getBoardName(data);
					String boardData = getBoardData(data);
					selectedBoard.whiteboardFromString(boardName, boardData);
					log.info("acc updated path: " + data);
					drawSelectedWhiteboard();
				});
			});
			clientManager.start();
		}
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
	    	selectedBoard.setShared(share);
	    	if(epToWhiteboardServer != null) {
	    		if(!selectedBoard.isRemote()) {
	    			if(selectedBoard.isShared()) {
	    				this.epToWhiteboardServer.emit(WhiteboardServer.shareBoard, selectedBoard.getName());
	    			} else {
	    				this.epToWhiteboardServer.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
	    			}
	    		} 
	    	} 
	    } else {
	    	log.severe("there is no selected board");
	    }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
		whiteboards.values().forEach((whiteboard)->{
			
		});
		
		if(epToWhiteboardServer != null) {
			epToWhiteboardServer.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
			epToWhiteboardServer.close();
		}
		
		if(!epToPeerClient.isEmpty()) {
			for (String key : epToPeerClient.keySet()) {
				for (Endpoint EP : epToPeerClient.get(key)){
					EP.close();
				}
			}
		}
		if(!targetClient.isEmpty()){
			for (String key : targetClient.keySet()) {
				for (Endpoint EP : targetClient.get(key)){
					EP.close();
				}
			}
		}
	    
		peerManager.shutdown();
		clientManager.shutdown();
	}

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) {
	            	setShare(e.getStateChange()==1);
	            }
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
