import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
//import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
//import java.util.Arrays;
import java.util.Arrays;


public class GuruElimChecker {

	static int[] values; 
	static String[] entrants;
	static String[] legends;
	static int[] scores;
	static int[] scenarioScores;
	static ArrayList<String[]> allPicks;
	static String[] scenarioResults;
	static String[] results;
	static String[][] possibleResults;
	static File outFile;
	static String[] closeEntries;
	static int nextMatch;
	static int checkIndex;
	static int[] wrongMatches;
	static String winningScenario;
	static FileWriter writer;
	
	
	public GuruElimChecker(int[] INvalues,String[] INentrants, ArrayList<String[]> INallPicks, String[] INresults, String[][] INpossibleResults,
			int INnextMatch)
	{
		values = INvalues; 
		entrants = INentrants;
		allPicks = INallPicks;
		

		results = INresults;
		possibleResults = INpossibleResults;
		//static File neighbors;
		nextMatch = INnextMatch;
	}
	
	
	public static void main(String[] args) {
		populateValues();
		nextMatch = 0;
		allPicks = new ArrayList<String[]>();
		try {
	        File inFile = new File("allbrackets.txt");
	        //String player = "";
	        //I think this was used to spot check a particular player. May have been rolled into standard functionality when I added complex elimination logic?
//	        if(args.length < 1)
	        	checkIndex = 0;
//	        else
//	        	player = args[0];
//	        
	        
	        //neighbors = new File("neighbors.txt");
	        
	        setUpLegends();
	        BufferedReader in = new BufferedReader(new FileReader(inFile));
	        String line;
	        ArrayList<String> players = new ArrayList<String>();
	        int count = 0;
	        while ((line = in.readLine()) != null) {
	            String[] picks = line.split(",", -1);
	            if(picks[0].equals("ACTUAL"))
	            {
	            	processResults(picks);
	            }else if(picks[0].equals("POSSIBLE"))
	            {
	            	processPossibleResults(picks);
	            }else{
	            	players.add(picks[0]);
//	            	if(picks[0].equals(player))
//	            	{
//	            		checkIndex = count;
//	            	}
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
		fillPossiblesWithResults();
		//outputClosestBrackets();
//			if(args.length <= 0)
//				checkNext(1,"Spotcheck_");
//			else
//				checkNext(Integer.parseInt(args[0]),"Spotcheck_");
		
		calculateScenarios("");
	}
	
	//check the next _i_ results. This is recursive, branching until i=1, and resulting in 2^i executions.
	//filename stores the desired output filename, typically just a concatenation of the combination of results being checked.
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
				outFile = new File(filename+poss+".txt");
				//outputClosestBrackets();
				checkAllPlayers();
				nextMatch--;
			}else{
				nextMatch++;
				checkNext(i-1, filename+poss+"+");
				nextMatch--;
			}
		}
		possibleResults[nextMatch] = possibles;
		
	}
	
	public static void calculateScenarios(String scene)
	{
		String[] possibles = getPossibles(nextMatch);
		for(String poss : possibles)
		{
			possibleResults[nextMatch] = new String[1];
			possibleResults[nextMatch][0] = poss;
			results[nextMatch] = poss;
			
			//if the current match is the final, print the winner(s), else continue to iterate.
			if(nextMatch == 149)
			{
				scores = calculateScores(results);
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
		//possibleResults[nextMatch] = new String[possibles.length];
		//possibleResults[nextMatch] = possibles;
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
	
	//loop through all players, checking if they are alive given a specific scenario (that is stored in results)
	public static void checkAllPlayers()
	{
		try {
			System.out.println(outFile.getName()+": ");
			writer = new FileWriter(outFile);
			for(int i = 0; i < entrants.length; i++)
			{
				checkIndex = i;
				checkPlayer();
			}
			System.out.println();
			writer.close();
		}catch(IOException e) {
			System.out.println("problem with output");
			//return false;
			//System.exit(1);
		}
	}
	
	//run through brackets to see if this player has been eliminated
	public static void checkPlayer()
	{
		try {
			
			scenarioResults = new String[150];
			ArrayList<Integer> differences = new ArrayList<Integer>();
			//set scenarioResults to current result or player's bracket when not impossible
			for(int i=0; i < 150; i++)
			{
				if(i < nextMatch){
					scenarioResults[i] = results[i];
				}else{
					//check if a pick has been disqualified by previous results. 
					//If it is still possible, assume it happens, else add the match number to the list to iterate through.
					if(isValid(allPicks.get(checkIndex)[i],i)){
						scenarioResults[i] = allPicks.get(checkIndex)[i];
					}else{
						scenarioResults[i] = "";
						differences.add(i);
					}
				}
			}
			//if there aren't any matches to check specifically (i.e. no picked winners that lost in previous rounds)
			//	just check to see if they win if everything breaks right.
			if(differences.size() == 0)
			{
				if(outputScenarioWinner("any combination+"))
				{
					writer.write("\t"+entrants[checkIndex]+" is ALIVE");
				}else{
					System.out.println(entrants[checkIndex]);
					writer.write("\t"+entrants[checkIndex]+" is DEAD");
				}
			}else{
				//find later round matches to iterate through, where the player is already guaranteed to be wrong
				wrongMatches = new int[differences.size()];


				for(int i = 0; i < wrongMatches.length; i++)
				{
					wrongMatches[i] = differences.get(i).intValue();
				}

				//recurse through results, checking from left-most first. When you reach the end of the list of matches, check scores
				//	and print the winner for the given combination.
				boolean isAlive = checkPlayerHelper(0,"");

				//if player is the winner, end execution
				if(isAlive)
				{
					writer.write("\t"+entrants[checkIndex]+" is ALIVE");
				}else{
					writer.write("\t"+entrants[checkIndex]+" is DEAD");
					System.out.println(entrants[checkIndex]);
				}
			}
			writer.write("\n");
			
		}	
		catch (IOException e) {
			System.out.println("problem with output");
			System.exit(1);
		}
	}
	
	//individual player checker - iterates through _wrongMatches_, and determines if 
	public static boolean checkPlayerHelper(int i, String scenario)
	{
		boolean result = false;
		if(i >= wrongMatches.length)
		{
			return outputScenarioWinner(scenario);
		}
		String[] possibles = getPlayerPossibles(wrongMatches[i]);
		
		for(String poss : possibles)
		{
			scenarioResults[wrongMatches[i]] = poss;
			result = checkPlayerHelper(i+1, scenario+poss+"+");
			if(result)
				break;
		}
		scenarioResults[wrongMatches[i]] = "";
		//if player is the winner, end execution, else print scenario and winners
		return result;
	}
	
	//checks the scores for all players for a given scenario to see if a player is alive or not. Returns true/false if they are/not.
	public static boolean outputScenarioWinner(String scene)
	{
		try{
			boolean result = false;
			scores = calculateScores(scenarioResults);
			int maxscore = scores[0];
			for(int i = 1; i < scores.length; i++)
			{
				if(scores[i] > maxscore)
					maxscore = scores[i];
			}
			scene = scene.substring(0,scene.length()-1);
			writer.write("Winner(s) for " + scene +": ");
			for(int j = 0; j < scores.length; j++)
			{
				if(scores[j]==maxscore){
					if(j == checkIndex){
						result = true;
						winningScenario = scene;
					}
					writer.write(entrants[j]+" ");
				}
			}
			writer.write("with "+maxscore+" points. "+entrants[checkIndex]+" had "+scores[checkIndex]+" points.\n");
			return result;
		}catch(IOException e) {
			System.out.println("problem with output");
			return false;
			//System.exit(1);
		}
	}
	
	
	//gets possible winners for a match within a specific situation, in which the player being checked is already guaranteed to miss the match.
	//should check _scenarioResults_ instead of _results_
	public static String[] getPlayerPossibles(int match)
	{
		String[] result;
		int start;
		
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
				temp.add(scenarioResults[match-8]);
				temp.add(legends[match-120]);
				temp.remove(scenarioResults[((match-120)/2)+128]); //don't allow a character to win if they're the winner in this player's losers bracket for the corresponding match
			}else if(match < 132)
			{
				temp.add(legends[(match-128)*2]);
				temp.add(legends[(match-128)*2+1]);
				temp.add(scenarioResults[(match-128)*2+112]);
				temp.add(scenarioResults[(match-128)*2+113]);
				temp.remove(scenarioResults[(match-128)*2+120]);
				temp.remove(scenarioResults[(match-128)*2+121]);
				//temp.add(getScenarioLoser((match-128)*2+120));
				//temp.add(getScenarioLoser((match-128)*2+121));
			}else if(match < 136)
			{
				temp.add(scenarioResults[(match-132)*2+120]);
				temp.add(scenarioResults[(match-132)*2+121]);
				temp.remove(scenarioResults[((match-132)/2)+142]);
			}else if(match < 140)
			{
				temp.add(scenarioResults[(match-8)]);
				temp.add(scenarioResults[(match-136)*2+120]);
				temp.add(scenarioResults[(match-136)*2+121]);
				temp.remove(scenarioResults[(match-4)]);
				//temp.add(getScenarioLoser(match-4));
			}else if(match < 144)
			{
				temp.add(scenarioResults[(match-140)*2+132]);
				temp.add(scenarioResults[(match-140)*2+133]);
				if(match < 142)
					temp.remove(scenarioResults[match+4]);
			}else if(match < 146)
			{
				temp.add(scenarioResults[(match-2)]);
				temp.add(scenarioResults[(match-144)*2+132]);
				temp.add(scenarioResults[(match-144)*2+133]);
				temp.remove(scenarioResults[(match-4)]);
				//temp.add(getScenarioLoser(match-4));
			}else if(match == 146)
			{
				temp.add(scenarioResults[match-6]);
				temp.add(scenarioResults[match-5]);
				temp.remove(scenarioResults[match+2]);
			}else if(match == 147)
			{
				temp.add(scenarioResults[match-3]);
				temp.add(scenarioResults[match-2]);
			}else if(match == 148)
			{
				temp.add(scenarioResults[match-1]);
				temp.add(scenarioResults[match-8]);
				temp.add(scenarioResults[match-7]);
				temp.remove(scenarioResults[(match-2)]);
				//temp.add(getScenarioLoser(match-2));
			}else{
				temp.add(scenarioResults[match-3]);
				temp.add(scenarioResults[match-1]);
			}
			result = temp.toArray(new String[temp.size()]);
			
			return result;
		}
		for(int i = start; i < start+2; i++)
		{
			temp.add(scenarioResults[i]);
		}
		result = temp.toArray(new String[temp.size()]);
		
		return result;
	}
	
	//returns a list of possible winners for a given (next) match.
	//assumes that all matches before the one asked for have completed.
	public static String[] getPossibles(int match)
	{
		String[] result;
		int start;
		//if(!possibleResults[match][0].equals(""))
			//return possibleResults[match];
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
				temp.add(results[(match-136)*2+120]);
				temp.add(results[(match-136)*2+121]);
				temp.remove(results[(match-4)]);
				//temp.add(getScenarioLoser(match-4));
			}else if(match < 144)
			{
				temp.add(results[(match-140)*2+132]);
				temp.add(results[(match-140)*2+133]);
				//if(match < 142)
					//temp.remove(results[match+4]);
			}else if(match < 146)
			{
				temp.add(results[(match-2)]);
				temp.add(results[(match-144)*2+132]);
				temp.add(results[(match-144)*2+133]);
				temp.remove(results[(match-4)]);
				//temp.add(getScenarioLoser(match-4));
			}else if(match == 146)
			{
				temp.add(results[match-6]);
				temp.add(results[match-5]);
				//temp.remove(results[match+2]);
			}else if(match == 147)
			{
				temp.add(results[match-3]);
				temp.add(results[match-2]);
			}else if(match == 148)
			{
				temp.add(results[match-1]);
				temp.add(results[match-8]);
				temp.add(results[match-7]);
				temp.remove(results[(match-2)]);
				//temp.add(getScenarioLoser(match-2));
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
	
	//get the loser of a finals division match
	public static String getScenarioLoser(int matchNum)
	{
		if(matchNum < 128)
		{
			if(scenarioResults[matchNum] == legends[matchNum - 120])
				return scenarioResults[matchNum - 8];
			else
				return legends[matchNum - 120];
		}else if(matchNum > 131 && matchNum < 136)
		{
			if(scenarioResults[matchNum] == scenarioResults[(matchNum - 132)*2+120])
				return scenarioResults[(matchNum - 132)*2+121];
			else
				return scenarioResults[(matchNum - 132)*2+120];
		}else if(matchNum > 139 && matchNum < 142)
		{
			if(scenarioResults[matchNum] == scenarioResults[(matchNum - 140)*2+132])
				return scenarioResults[(matchNum - 140)*2+133];
			else
				return scenarioResults[(matchNum - 140)*2+132];
		}else if(matchNum == 146){
			//matchNum should be 146
			if(scenarioResults[matchNum] == scenarioResults[140])
				return scenarioResults[141];
			else
				return scenarioResults[140];
		}else
		{
			return "error";
		}
		
	}
	
	//fills in the _values_ array with the point values for each match.
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
						isValid(pick, matchNum-4) )
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
