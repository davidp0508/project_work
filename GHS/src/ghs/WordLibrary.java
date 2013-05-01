package ghs;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import dao.CardDAO;
import dao.GameRoomDAO;
import dao.GenreDAO;
import dao.MyDAOException;
import dao.PlayerDAO;
import databean.Card;
import databean.Genre;

public class WordLibrary {

	static CardDAO cardDao;
	static GenreDAO genreDao;

	static String jdbcDriverName;
	static String jdbcURL;

	HashMap<Integer,ArrayList<Integer>> givenCards = new HashMap<Integer,ArrayList<Integer>>();

	/*
	 * Set up mysql connection
	 */
	public void initWordLib(){

		String jdbcDriverName = "com.mysql.jdbc.Driver";
		String jdbcURL        =  "jdbc:mysql:///word_library";

		try {
			cardDao = new CardDAO( jdbcDriverName, jdbcURL, "cards");
			genreDao = new GenreDAO(jdbcDriverName, jdbcURL, "genres");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Card getCard(int playerId, int roomId, String tableName) throws MyDAOException{
		Card card = new Card();

		ArrayList<Integer> givenCardIds = givenCards.get(roomId);

		//		String tableName = "";
		//
		//		switch(choice){
		//		case 1: 		
		//			tableName = "Movies";
		//			break;
		//		case 2: 
		//			tableName = "Words";
		//			break;
		//		}

		if(givenCardIds == null){
			givenCardIds = new ArrayList<Integer>();
			card = fetchCard(playerId, roomId, tableName);	
		}else{
			do{
				card = fetchCard(playerId, roomId, tableName);				
			}while(givenCardIds.contains(card.getCardId()));
		}

		givenCardIds.add(card.getCardId());
		givenCards.put(roomId,givenCardIds);
		return card;
	}


	private Card fetchCard(int playerId, int roomId, String tableName) throws MyDAOException{

		Card card = new Card();
		ArrayList<Integer> cardIndices = new ArrayList<Integer>();
		cardIndices = (ArrayList<Integer>) genreDao.read(tableName);
		int randomIndex = getrnd(cardIndices.size());
		card = cardDao.read(cardIndices.get(randomIndex - 1));
		return card;
	}


	static int getrnd(int range) {
		Random rand = new Random();
		return (1 + rand.nextInt(range));
	}

	// for primary backup use only
	public void updateHashList(int playerId, int roomId, int cardId){

		ArrayList<Integer> givenCardIds = givenCards.get(roomId);
		if(givenCardIds == null){
			givenCardIds = new ArrayList<Integer>();
		}
		givenCardIds.add(cardId);
		givenCards.put(roomId,givenCardIds);
	}


	/*
	 * For populating db
	 */
	public void populateDb(){

		try{
			FileInputStream fstream = new FileInputStream("movieList.txt");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br= new BufferedReader(new InputStreamReader(in));
			String strLine;

			while((strLine= br.readLine())!=null) {

				Card card = new Card();
				String[] terms = strLine.split("\t");
				card.setEasy(terms[0]);
				card.setMedium(terms[1]);
				card.setHard(terms[2]);

				int id = cardDao.create(card);
				genreDao.create("Movies", id);

			}
		}catch( Exception e){
			e.printStackTrace();
		}
	}

}
