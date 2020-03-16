import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class Guru {

	static int[] values;
	static String[] entrants;
	static String[] legends;
	static int[] scores;
	static ArrayList<String[]> allPicks;
	static String[] results;
	static String[][] possibleResults;
	static File neighbors; 
	static int nextMatch;
	
	
	//main execution thread - initializes list of brackets, starts output. 
	//input argument (optional): how many matches to check (int), defaults to 1
	public static void main(String[] args) {
		populateValues();
		nextMatch = 0;
		allPicks = new ArrayList<String[]>();
		try {
			//changed default bracket file to allbrackets.txt
	        File inFile = new File("allbrackets.txt");
	        
	        neighbors = new File("neighbors.txt");
	        setUpLegends();
	        BufferedReader in = new BufferedReader(new FileReader(inFile));
	        String line;
	        ArrayList<String> players = new ArrayList<String>();
	        int count = 0;
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
	            	players.add(picks[0]);
	            	processPlayer(picks);
	            	count++;
	            }
	        }
	        entrants = new String[count];
	        players.toArray(entrants);
	        in.close();
	    } catch (IOException e) {
	        System.out.println("File Read Error: " + e.getMessage());
	    }
		scores = calculateScores(results);
		System.out.println("Current Match: " + nextMatch + " Remaining Brackets: " + entrants.length);
		outputClosestBrackets();
		//checkIllegalBrackets();
		//How many matches to check - default is 1
//		if(args.length <= 0)
//			checkNext(1,"");
//		else
//			checkNext(Integer.parseInt(args[0]),"");
		
		calculateScenarios("");
	}
	
	public static void checkIllegalBrackets()
	{
		File conflictedPlayers = new File("conflicts.txt");
		FileWriter writer;
		try {
			writer = new FileWriter(conflictedPlayers);
		 
			int[][] conflicts = {
					{120,128},{121,128},{122,129},{123,129},
					{124,130},{125,130},{126,131},{127,131},
					{132,136},{133,137},{134,138},{135,139},
					{140,144},{141,145},{146,148}
			};try {
			for(int player = 0; player < entrants.length; player++)
			{
				String[] picks = allPicks.get(player);
				for(int i = 0; i < 15; i++)
				{
					if(picks[conflicts[i][0]].equals(picks[conflicts[i][1]]))
					{
						
							writer.write(entrants[player]+" has a conflict between matches "+conflicts[i][0]+" and "+conflicts[i][1]+ " picking "+picks[conflicts[i][1]]+"\n");
						
					}
				}
			}
			writer.close();
			} catch (IOException e) {
							
			}
		}catch (IOException e1) {
			
		}
		
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
				outputClosestBrackets();
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
			if(nextMatch == 149)
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
			}else
			{
				//start of finals division
				if(match < 128)
				{
					temp.add(results[match-8]);
					temp.add(legends[match-120]);
				}else if(match < 132)
				{
					temp.add(getLoser((match-128)*2+120));
					temp.add(getLoser((match-128)*2+121));
				}else if(match < 136)
				{
					temp.add(results[(match-132)*2+120]);
					temp.add(results[(match-132)*2+121]);
				}else if(match < 140)
				{
					temp.add(results[(match-8)]);
					temp.add(getLoser(match-4));
				}else if(match < 144)
				{
					temp.add(results[(match-140)*2+132]);
					temp.add(results[(match-140)*2+133]);
				}else if(match < 146)
				{
					temp.add(results[(match-2)]);
					temp.add(getLoser(match-4));
				}else if(match == 146)
				{
					temp.add(results[match-6]);
					temp.add(results[match-5]);
				}else if(match == 147)
				{
					temp.add(results[match-3]);
					temp.add(results[match-2]);
				}else if(match == 148)
				{
					temp.add(results[match-1]);
					temp.add(getLoser(match-2));
				}else{
					temp.add(results[match-3]);
					temp.add(results[match-1]);
				}
				result = temp.toArray(new String[temp.size()]);
				
				return result;
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
		
		//get the loser of a finals division match
		public static String getLoser(int matchNum)
		{
			if(matchNum < 128)
			{
				if(results[matchNum].equals(legends[matchNum - 120]))
					return results[matchNum - 8];
				else
					return legends[matchNum - 120];
			}else if(matchNum > 131 && matchNum < 136)
			{
				if(results[matchNum].equals(results[(matchNum - 132)*2+120]))
					return results[(matchNum - 132)*2+121];
				else
					return results[(matchNum - 132)*2+120];
			}else if(matchNum > 139 && matchNum < 142)
			{
				if(results[matchNum].equals(results[(matchNum - 140)*2+132]))
					return results[(matchNum - 140)*2+133];
				else
					return results[(matchNum - 140)*2+132];
			}else if(matchNum == 146){
				//matchNum should be 146
				if(results[matchNum].equals(results[140]))
					return results[141];
				else
					return results[140];
			}else
			{
				return "error";
			}
			
		}
	
	//create the list of point values for a given match number.
	public static void populateValues()
	{
		values = new int[150];
		for(int i = 0; i < 150; i++)
		{
			if(i < 64)
				values[i] = 1;
			else if (i < 96)
				values[i] = 2;
			else if (i < 112)
				values[i] = 4;
			else if (i < 132)
				values[i] = 8;
			else if (i < 136)
				values[i] = 16;
			else if (i == 140 || i == 141)
				values[i] = 32;
			else if (i == 146)
				values[i] = 64;
			else if (i == 149)
				values[i] = 76;
			else //covers the rest of the losers bracket.
				values[i] = 8;
		}
	}
	
	//output the closest brackets for each entrant, and prints the eliminations given a specific result.
	public static void outputClosestBrackets()
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
		}
		if(matchNum < 96)
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
		}else
		{
			//if it has already happened, return the winner
			if(!possibleResults[matchNum][0].equals(""))
				return possibleResults[matchNum][0].equals(pick);
			//start of finals division
			if(matchNum < 128)
			{
				return legends[matchNum-120].equals(pick) 
						|| isValid(pick, matchNum-8);
			}else if(matchNum < 132)
			{
				return ( !results[(matchNum-128)*2+120].equals(pick) && 
						isValid(pick, (matchNum-128)*2+112) || legends[matchNum-128].equals(pick))
						|| ( !results[(matchNum-128)*2+121].equals(pick) && 
								isValid(pick, (matchNum-128)*2+113) || legends[matchNum-127].equals(pick));
			}else if(matchNum < 136)
			{
				return isValid(pick, (matchNum-132)*2+120) ||
						isValid(pick, (matchNum-132)*2+121);
			}else if(matchNum < 140)
			{
				return ( !results[(matchNum-4)].equals(pick) && 
						isValid(pick, (matchNum-136)*2+120) ||
						isValid(pick, (matchNum-136)*2+121) )
						|| isValid(pick, (matchNum-8));
			}else if(matchNum < 144)
			{
				return isValid(pick, (matchNum-140)*2+132) ||
						isValid(pick, (matchNum-140)*2+133);
			}else if(matchNum < 146)
			{
				return ( !results[(matchNum-4)].equals(pick) && 
						isValid(pick, matchNum-4) )
						|| isValid(pick, (matchNum-2));
			}else if(matchNum == 146)
			{
				return isValid(pick, matchNum-6) ||
						isValid(pick, matchNum-5);
			}else if(matchNum == 147)
			{
				return isValid(pick, matchNum-3) ||
						isValid(pick, matchNum-2);
			}else if(matchNum == 148)
			{
				return ( !results[(matchNum-2)].equals(pick) && 
						isValid(pick, matchNum-2) )
						|| isValid(pick, (matchNum-1));
			}else{
				return isValid(pick, matchNum-3) ||
						isValid(pick, matchNum-1);
			}
		}
	}
	

	//reads in the list of possible results for the first round (the participants in each match)
		public static void processPossibleResults(String[] possible)
		{
			possibleResults = new String[150][0];
			String[] parts;
			for(int i = 0; i < 150; i++)
			{
				parts = possible[i+1].split("; ");
				possibleResults[i] = parts;
			}
		}
		
		//reads the actual results that have occurred so far.
		public static void processResults(String[] picks)
		{
			results = new String[150];
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
		
		
		//enter the seeded characters for the legends bracket.
		public static void setUpLegends()
		{
			legends = new String[8];
			
			legends[0] = "Link";
			legends[1] = "Mega Man";
			legends[2] = "Cloud Strife";
			legends[3] = "Crono";
			legends[4] = "Solid Snake";
			legends[5] = "Sonic the Hedgehog";
			legends[6] = "Samus Aran";
			legends[7] = "Mario";
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
}
