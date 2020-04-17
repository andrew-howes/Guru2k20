import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Guru {

	static int[] values;
	static String[] entrants;
	static int[] scores;
	static ArrayList<String[]> allPicks;
	static String[] results;
	static String[][] possibleResults;
	static File neighbors; 
	static File allDifferences; 
	static int nextMatch;
	static File deadFile;
	static ArrayList<String> dead;
	
	//main execution thread - initializes list of brackets, starts output. 
	//input argument (optional): how many matches to check (int), defaults to 1
	public static void main(String[] args) {
		populateValues();
		nextMatch = 0;
		allPicks = new ArrayList<String[]>();
		try {
			deadFile = new File("deadEntrants.txt");
			bringOutYourDead();
			//changed default bracket file to allbrackets.txt
	        File inFile = new File("allbrackets.txt");
	        
	        neighbors = new File("neighbors.txt");
	        allDifferences = new File("allDifferences.csv");
	        BufferedReader in = new BufferedReader(new FileReader(inFile));
	        String line;
	        ArrayList<String> players = new ArrayList<String>();
	        while ((line = in.readLine()) != null) {
	            String[] picks = line.split(",", -1);
	            //master results bracket
	            if(picks[0].equals("ACTUAL"))
	            {
	            	processResults(picks);
	            }//possible results bracket - only really matters for round 1.
	            else if(picks[0].equals("POSSIBLE"))
	            {
	            	processPossibleResults(picks);
	            	
	            }else{
	            	//skip this player if they've been eliminated.
	            	if(dead.contains(picks[0]))
	            	{
	            		continue;
	            	}
	            	players.add(picks[0]);
	            	processPlayer(picks);
	            }
	        }
			fillPossiblesWithResults();
	        entrants = new String[players.size()];
	        players.toArray(entrants);
	        in.close();
	    } catch (IOException e) {
	        System.out.println("File Read Error: " + e.getMessage());
	    }
		scores = calculateScores(results);
		System.out.println("Current Match: " + nextMatch + " Remaining Brackets: " + entrants.length);
		outputClosestBrackets(true);
		//outputAllDifferences();
		//How many matches to check - default is 1
		if(args.length <= 0)
			checkNext(1,"");
		else
			checkNext(Integer.parseInt(args[0]),"");
		
		
		//calculateScenarios("");
	}

	//simulates the next 'i' matches to find eliminations
	public static void checkNext(int i, String filename)
	{
		String[] possibles = getPossibles(nextMatch);
		for(String poss : possibles)
		{
			possibleResults[nextMatch] = new String[1];
			possibleResults[nextMatch][0] = poss;
			results[nextMatch] = poss;
			scores = calculateScores(results);
			if(i <= 1)
			{
				nextMatch++;
				neighbors = new File(filename+poss+".txt");
				outputClosestBrackets(false);
				nextMatch--;
			}else{
				nextMatch++;
				checkNext(i-1, filename+poss+"+");
				nextMatch--;
			}
		}
		possibleResults[nextMatch] = possibles;
		
	}
	
	//calculates the winners for the remaining matches.
	//when simulating multiple matches at once, *scene* will contain a plus-delimited list of simulated winners to this point.
	public static void calculateScenarios(String scene)
	{
		String[] possibles = getPossibles(nextMatch);
		for(String poss : possibles)
		{
			possibleResults[nextMatch] = new String[1];
			possibleResults[nextMatch][0] = poss;
			results[nextMatch] = poss;
			scores = calculateScores(results);
			//if the current match is the final, print the winner(s), else continue to iterate.
			if(nextMatch == 126)
			{
				String newScene = scene+poss;
				outputWinner(newScene);
			}else{
				nextMatch++;
				calculateScenarios(scene+poss+"+");
				nextMatch--;
			}
			possibleResults[nextMatch][0] = "";
			results[nextMatch] = "";
		}
		possibleResults[nextMatch] = new String[possibles.length];
		possibleResults[nextMatch] = possibles;
	}
	
	//outputs the winner(s) for a given scenario.
	public static void outputWinner(String scene)
	{
		int maxscore = scores[0];
		for(int i = 1; i < scores.length; i++)
		{
			if(scores[i] > maxscore)
				maxscore = scores[i];
		}
		System.out.print("Winner(s) for " + scene +": ");
		for(int j = 0; j < scores.length; j++)
		{
			if(scores[j]==maxscore)
				System.out.print(entrants[j]+" ");
		}
		System.out.println();
	}
	
	//gets the list of possible winners for a given match
	//assumes that the previous matches have been played or simulated at this point. 
	//returns a list of possible winners for a given (next) match.
		//assumes that all matches before the one asked for have completed.
	public static String[] getPossibles(int match)
	{
		String[] result;
		int start;
		if(!possibleResults[match][0].equals(""))
			return possibleResults[match];
		ArrayList<String> temp = new ArrayList<String>();
		if(match < 96)
		{
			start = (match-64)*2;
		}else if(match < 112)
		{
			start = (match-96)*2+64;
		}else if(match < 120)
		{
			start = (match-112)*2+96;
		}else if(match < 124)
		{
			start = (match-120)*2+112;
		}else if(match < 126)
		{
			start = (match-124)*2+120;
		}else
		{
			start = 124;
		}
		for(int i = start; i < start+2; i++)
		{
			if(i < nextMatch)
			{
				temp.add(results[i]);
			}else{
				for(int j = 0; j < possibleResults[i].length; j++)
				{
					temp.add(possibleResults[i][j]);
				}
			}
		}
		result = temp.toArray(new String[temp.size()]);
		
		return result;
	}
	
	//create the list of point values for a given match number.
	public static void populateValues()
	{
		values = new int[127];
		for(int i = 0; i < 127; i++)
		{
			if(i < 64)
				values[i] = 1;
			else if (i < 96)
				values[i] = 2;
			else if (i < 112)
				values[i] = 4;
			else if (i < 120)
				values[i] = 8;
			else if (i < 124)
				values[i] = 16;
			else if (i < 126)
				values[i] = 32;
			else 
				values[i] = 64;
		}
	}
	
	//output the closest brackets for each entrant, and prints the eliminations given a specific result.
	public static void outputClosestBrackets(boolean removeDead)
	{
		try {
			FileWriter writer = new FileWriter(neighbors);
			
			String winner = neighbors.getName();
			
			winner = winner.substring(0,winner.indexOf("."));
			if(! winner.equals("neighbors"))
				System.out.println("Elims for a "+winner+" win:");
			
			writer.write("<span class=\"nocode\">\n");
			writer.write("updated through "+results[nextMatch-1]+"'s win\n");
			int[][] comparisons;
			int minscore;
			String out;
			ArrayList<Integer> minIDs = new ArrayList<Integer>();
			int[] diffmatches;
			boolean hasPrinted = false;
			for(int player = 0; player < entrants.length; player++)
			{
				comparisons = new int[entrants.length][3];
				for(int second = 0; second < entrants.length; second++)
				{
					comparisons[second] = getDifferenceScore(player, second);
				}
				minscore = 700;//64*8 + 14*8+76
				minIDs.clear();
				for(int i = 0; i < entrants.length; i++)
				{
					if(i != player)
					{
						//if(comparisons[i][1] < minscore)
						//if((scores[i]-scores[player]) + comparisons[i][2] < minscore)
						if((comparisons[i][2]-(scores[i]-scores[player])) < 5 ||
								(scores[player]-scores[i]) + comparisons[i][2] < minscore)
						{
							if(minscore > 5)
								minIDs.clear();
							//minscore = comparisons[i][1];
							if(comparisons[i][2]-(scores[i]-scores[player]) < minscore)
								minscore = (comparisons[i][2]-(scores[i]-scores[player]));
							minIDs.add(i);
						//}else if(comparisons[i][1] == minscore)
						}else if((scores[player]-scores[i]) + comparisons[i][2] == minscore)
						{
							minIDs.add(i);
						}
					}
				}
				out = "";
				writer.write(entrants[player]+"'s closest brackets: - current score: " 
								+ scores[player] + " count: " + minIDs.size() + "\n");
				hasPrinted = false;
				for(Integer i : minIDs)
				{
					if((comparisons[i][2]-(scores[i]-scores[player]))<0 || minscore>=0)
					{
						out += "  " + entrants[i] + " -";
						out += " total difference: " + comparisons[i][1];
						out += " current deficit: "+ (scores[i]-scores[player]); 
						out += " possible gain: " + comparisons[i][2] +"\n";
						out += "    elimination cushion: " + (comparisons[i][2]-(scores[i]-scores[player])) + "\n";
						out += "\tdifferences: ";
						diffmatches = getDifferentMatches(player,i);
						out += Arrays.toString(diffmatches)+"\n";
						if((scores[i]-scores[player]) > comparisons[i][2])
						{
							out += "Should be dead\n";
							
							if(!hasPrinted){
								System.out.print(entrants[player] + " by " + entrants[i]);
								hasPrinted = true;
								if(removeDead)
								{
									dead.add(entrants[player]);
								}
							}else
								System.out.print(", " + entrants[i]);
						}
					}
				}
				if(hasPrinted) System.out.println();
				writer.write(out);
			}
			System.out.println();
			writer.write("</span>\n");
			writer.close();
			
			//remove players that are dead from consideration (first-run only)
			if(removeDead)
			{
				writeDead();
			}
			
			
		} catch (IOException e) {
			System.out.println("problem with output");
			System.exit(1);
		}
		//System.out.println("Done getting differences");
	}
	
	//output the closest brackets for each entrant, and prints the eliminations given a specific result.
		public static void outputAllDifferences()
		{
			try {
				FileWriter writer = new FileWriter(allDifferences);
				
				int[][] comparisons;
				
				writer.write("Player");
				for(int player = 0; player < entrants.length; player++)
				{
					writer.write(","+entrants[player]);
				}
				writer.write("\n");
				

				for(int player = 0; player < entrants.length; player++)
				{
					writer.write(entrants[player]);
					comparisons = new int[entrants.length][3];
					for(int second = 0; second < entrants.length; second++)
					{
						comparisons[second] = getDifferenceScore(player, second);
						writer.write(","+comparisons[second][1]);
					}
					writer.write("\n");
				}
				System.out.println();
				writer.close();
			} catch (IOException e) {
				System.out.println("problem with output");
				System.exit(1);
			}
			//System.out.println("Done getting differences");
		}
	
	//returns the list of match numbers that have different picks in the given brackets. 
	public static int[] getDifferentMatches(int first, int second)
	{
		String[] firstPicks = allPicks.get(first);
		String[] lastPicks = allPicks.get(second);
		
		ArrayList<Integer> differences = new ArrayList<Integer>();
		
		for(int i = 0; i < firstPicks.length; i++)
		{
			if(!firstPicks[i].equals(lastPicks[i]))
			{
				differences.add(i+1);
			}
		}
		int[] result = new int[differences.size()];
		for(int i = 0; i < result.length; i++)
		{
			result[i] = differences.get(i).intValue();
		}
		return result;
	}
	
	//gets the possible point difference between two brackets, along with the absolute number of differences and the points to make up.
	public static int[] getDifferenceScore(int first, int second)
	{
		String[] firstPicks = allPicks.get(first);
		String[] lastPicks = allPicks.get(second);
		int[] result = new int[3];
		//number of differences, point value, possible points to make up
		result[0] = result[1] = result[2] = 0;
		for(int i = 0; i < firstPicks.length; i++)
		{
			if(!firstPicks[i].equals(lastPicks[i]))
			{
				result[1] += values[i];
				result[0]++;
				if(i >= nextMatch && isValid(firstPicks[i],i))
				{
					result[2]+=values[i];
				}
			}
		}
		
		return result;
	}
	
	//return if a pick is valid for a given match - i.e can the player still earn points if they picked it. 
	//recurses for later matches.
	public static boolean isValid(String pick, int matchNum)
	{
		if(matchNum < 64)
		{
			if(matchNum < nextMatch)
			{
				return results[matchNum].equals(pick);
			}
			
			for(int i = 0; i < possibleResults[matchNum].length; i++)
			{
				if(possibleResults[matchNum][i].equals(pick))
					return true;
			}
			return false;
		}else if(matchNum < 96)
		{
			if(possibleResults[matchNum][0].equals(""))
				return isValid(pick, (matchNum-64)*2) ||
						isValid(pick, (matchNum-64)*2+1);
			else
				return possibleResults[matchNum][0].equals(pick);
		}else if(matchNum < 112)
		{
			if(possibleResults[matchNum][0].equals(""))
				return isValid(pick, (matchNum-96)*2+64) ||
						isValid(pick, (matchNum-96)*2+65);
			else
				return possibleResults[matchNum][0].equals(pick);
		}else if(matchNum < 120)
		{
			if(possibleResults[matchNum][0].equals(""))
				return isValid(pick, (matchNum-112)*2+96) ||
						isValid(pick, (matchNum-112)*2+97);
			else
				return possibleResults[matchNum][0].equals(pick);
		}else if(matchNum < 124)
		{
			if(possibleResults[matchNum][0].equals(""))
				return isValid(pick, (matchNum-120)*2+112) ||
						isValid(pick, (matchNum-120)*2+113);
			else
				return possibleResults[matchNum][0].equals(pick);
		}else if(matchNum < 126)
		{
			if(possibleResults[matchNum][0].equals(""))
				return isValid(pick, (matchNum-124)*2+120) ||
						isValid(pick, (matchNum-124)*2+121);
			else
				return possibleResults[matchNum][0].equals(pick);
		}else
		{
			return isValid(pick, 124)||isValid(pick,125);
		}
	}
	

	//reads in the list of possible results for the first round (the participants in each match)
		public static void processPossibleResults(String[] possible)
		{
			possibleResults = new String[127][0];
			String[] parts;
			for(int i = 0; i < 127; i++)
			{
				parts = possible[i+1].split(";");
				possibleResults[i] = parts;
			}
		}
		
		//reads the actual results that have occurred so far.
		public static void processResults(String[] picks)
		{
			results = new String[127];
			results = Arrays.copyOfRange(picks, 1, picks.length);
			for(int i = 1; i < results.length; i++)
			{
				if(results[i].equals("")){
					nextMatch = i;
					break;
				}
			}
		}
		
		//fills in possible results with the actual results. This keeps me from needing to update the Possible row in the .txt document every time.
		public static void fillPossiblesWithResults()
		{
			for(int i = 0; i < nextMatch; i++)
			{
				possibleResults[i] = new String[1];
				possibleResults[i][0] = results[i];
			}
		}
		
		
		//read in selected winners for a player. Ignore the first item, since it's the player name.
		//make sure the semicolon is removed from the end of the line.
		public static void processPlayer(String[] picks)
		{
			String[] playerPicks = new String[picks.length-1];
			playerPicks = Arrays.copyOfRange(picks, 1, picks.length);

			allPicks.add(playerPicks);
		}
		
		//calculate scores for all players, given a set of results.
		public static int[] calculateScores(String[] resultsToCheck)
		{
			int[] scores = new int[entrants.length];
			//results = checkResults(preResults);
			for(int i = 0; i < resultsToCheck.length; i++)
			{
				if(!resultsToCheck[i].equals(""))
				{
					//for each player
					for(int j = 0; j < entrants.length; j++)
					{
						//if the player's pick for the match is equal to the result
						if(allPicks.get(j)[i].equals(resultsToCheck[i]))
						{
							//increase their points by the value of the match
							scores[j] += values[i];
						}
					}
				}else{
					break;
				}
			}
			return scores;
		}
		public static void bringOutYourDead()
		{
			try {
				BufferedReader in = new BufferedReader(new FileReader(deadFile));
		        String line;
		        dead = new ArrayList<String>();
		        while ((line = in.readLine()) != null) {
		            String player = line;
		            dead.add(player);
		        }	        
		        in.close();
			} catch (IOException e) {
		        System.out.println("File Read Error: " + e.getMessage());
		    }
		}
		
		public static void writeDead()
		{
			
			List<String> players = new ArrayList<String>(Arrays.asList(entrants));
			for(String deadGuy : dead)
			{
				if(players.contains(deadGuy))
				{
					allPicks.remove(players.indexOf(deadGuy));
					players.remove(deadGuy);
				}
				entrants = new String[players.size()];
		        players.toArray(entrants);
			}
			try {
				FileWriter ghostwriter = new FileWriter(deadFile);
				for(String deadGuy : dead)
				{
					ghostwriter.write(deadGuy + "\n");
				}
				ghostwriter.close();
			}catch (IOException e) {
		        System.out.println("File Read Error: " + e.getMessage());
		    }
		}
}
