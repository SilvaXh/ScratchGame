package s.game.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import s.game.models.Matrix;

public class ScratchGameServiceImpl{

	public Matrix generateMatrix(int columns, int rows, JSONObject probabilities) {
        Matrix matrix = new Matrix();
        String[][] grid = new String[rows][columns];
        Random random = new Random();

        // Extract probabilities object
        JSONObject probabilitiesObj = probabilities.getJSONObject("probabilities");

        // Extract standard and bonus symbol probabilities from nested object
        JSONArray standardSymbols = probabilitiesObj.getJSONArray("standard_symbols");
        JSONObject bonusSymbolsObj = probabilitiesObj.getJSONObject("bonus_symbols");

        // Fill the matrix with symbols based on probabilities
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Generate a random number to determine the symbol
                double rand = random.nextDouble() * 100; // Range [0, 100)

                // Determine the symbol based on probabilities
                String symbol = determineSymbol(rand, standardSymbols, bonusSymbolsObj);
                grid[row][col] = symbol;
            }
        }

        matrix.setGrid(grid);
        return matrix;
    }

	private String determineSymbol(double rand, JSONArray standardSymbols, JSONObject bonusSymbolsObj) {
	    double cumulativeProbability = 0.0;
	    List<JSONObject> allSymbols = new ArrayList<>();

	    // Extract probabilities for standard symbols
	    for (Object obj : standardSymbols) {
	        if (obj instanceof JSONObject) {
	            JSONObject symbolInfo = (JSONObject) obj;
	            JSONObject symbolProbabilities = symbolInfo.getJSONObject("symbols");

	            // Loop through symbol probabilities for the given position
	            for (String symbolKey : symbolProbabilities.keySet()) {
	                JSONObject symbol = new JSONObject();
	                symbol.put("symbol", symbolKey);
	                symbol.put("probability", symbolProbabilities.getDouble(symbolKey));
	                allSymbols.add(symbol);
	            }
	        }
	    }

	    // Extract probabilities for bonus symbols
	    if (bonusSymbolsObj != null && bonusSymbolsObj.has("symbols")) {
	        JSONObject bonusSymbols = bonusSymbolsObj.getJSONObject("symbols");
	        for (String bonusSymbolKey : bonusSymbols.keySet()) {
	            JSONObject bonusSymbol = new JSONObject();
	            bonusSymbol.put("symbol", bonusSymbolKey);
	            bonusSymbol.put("probability", bonusSymbols.getDouble(bonusSymbolKey));
	            allSymbols.add(bonusSymbol);
	        }
	    }

	    // Determine the symbol based on probabilities
	    for (JSONObject symbol : allSymbols) {
	        double probability = symbol.getDouble("probability");
	        cumulativeProbability += probability;
	        
	        // Check if the random number falls within the current cumulative probability
	        if (rand <= cumulativeProbability) {
	            return symbol.getString("symbol");
	        }
	    }

	    return ""; // Default return if symbol is not determined
	}


    public JSONObject evaluateGame(JSONObject configJson, int betAmount) {
        // Extract configuration details
    	int columns = configJson.getInt("columns");
        int rows = parseInt(configJson.get("rows"));
        
        JSONObject symbolsObj = configJson.getJSONObject("symbols");  // Get JSONObject for symbols

        // Convert symbolsObj to a format suitable for processing
        JSONArray symbols = new JSONArray();
        for (String symbolName : symbolsObj.keySet()) {
            JSONObject symbolDetails = symbolsObj.getJSONObject(symbolName);
            // Include the symbol name in its properties for processing
            symbolDetails.put("name", symbolName);
            symbols.put(symbolDetails);
        }
        
        
        JSONObject winCombinationsObj = configJson.getJSONObject("win_combinations");

        // Convert winCombinationsObj to a JSONArray suitable for processing
        JSONArray winCombinations = new JSONArray();
        for (String winCombName : winCombinationsObj.keySet()) {
            JSONObject winCombDetails = winCombinationsObj.getJSONObject(winCombName);
            winCombDetails.put("name", winCombName);  // Include the combination name
            winCombinations.put(winCombDetails);
        }
        // Generate the matrix based on probabilities
        Matrix matrix = generateMatrix(columns, rows, configJson);
        String[][] grid = matrix.getGrid();
     // Print the matrix row by row
        System.out.println("Generated Matrix:");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Print each cell value
                System.out.print(grid[row][col] + " ");
            }
            // Move to the next row
            System.out.println();
        }
        
        // Extract probabilities object
        JSONObject probabilitiesObj = configJson.getJSONObject("probabilities");

        // Extract standard and bonus symbol probabilities from nested object
        JSONObject bonusSymbolsObj = probabilitiesObj.getJSONObject("bonus_symbols");
       
        // Evaluate the generated matrix against winning combinations
        JSONObject gameResult = evaluateMatrix(matrix, symbols, bonusSymbolsObj, winCombinations, betAmount);
        return gameResult;
    }

    private JSONObject evaluateMatrix(Matrix matrix, JSONArray symbols, JSONObject bonusSymbolsObj, JSONArray winCombinations, int betAmount) {
        JSONObject result = new JSONObject();
        String[][] grid = matrix.getGrid();
        Map<String, Map<Double, String>> rewardList = calculateReward(grid, winCombinations);
        double rewardAndBonus;
        double reward = calculate(rewardList, symbols, betAmount, result);
        if (reward > 0) {
        	 //Here if we have reward i check for bonus symobls
            List<String> bonusSymbols = getBonusSymbols(matrix, bonusSymbolsObj);
          
            rewardAndBonus = addBonusInReward(bonusSymbols, reward, symbols, result);
            result.put("reward", rewardAndBonus);
        }
        return result;
    }

    private double addBonusInReward(List<String> bonusSymbols, double reward, JSONArray symbols, JSONObject result) {
    	double rewardAndBonus = reward;
    	for (String symbol : bonusSymbols) {
             JSONObject symbolInfo = getSymbolInfo(symbols, symbol);
             if (symbolInfo != null) {
            	 rewardAndBonus =  applyBonusEffect(symbolInfo, reward, result);
             }
             result.put("applied_bonus_symbol", bonusSymbols);
         }
		return rewardAndBonus;
	}

    private double applyBonusEffect(JSONObject bonusDetails, double reward, JSONObject result) {
        String impact = bonusDetails.optString("impact");
        switch (impact) {
            case "multiply_reward":
                double multiplier = bonusDetails.optDouble("reward_multiplier", 1);
                reward *= multiplier;
                break;
            case "extra_bonus":
                double extra = bonusDetails.optDouble("extra", 0);
                reward += extra;
                break;
            default:
                break;
        }

        return reward;
    }

	private double calculate(Map<String, Map<Double, String>> rewardList, JSONArray symbols, int betAmount, JSONObject result) {
    	double totalReward = 0.0;

    	JSONObject winningCombinationOfMatrix = new JSONObject();
        // Iterate over each symbol in the rewardList map
        for (Map.Entry<String, Map<Double, String>> symbolEntry : rewardList.entrySet()) {
            String symbol = symbolEntry.getKey();
            Map<Double, String> symbolMap = symbolEntry.getValue();
           
            // Check if the symbol is in the specified symbols JSONArray
            JSONObject symbolInfo = getSymbolInfo(symbols, symbol);
            if(symbolInfo.getString("type").equalsIgnoreCase("standard")) {
            	double rewardMultiplierForSymbol = symbolInfo.getDouble("reward_multiplier");
                double rewardForSymbol =  betAmount * rewardMultiplierForSymbol;
                JSONArray winningConditions = new JSONArray();

                    // Iterate over entries
                    for (Map.Entry<Double, String> entry : symbolMap.entrySet()) {
                        Double key = entry.getKey();
                        winningConditions.put(entry.getValue());

                        rewardForSymbol =  rewardForSymbol * key;
                    }
                    totalReward += rewardForSymbol;
                    winningCombinationOfMatrix.put(symbol, winningConditions);
                    result.put("applied_winning_combinations", winningCombinationOfMatrix);
            }
        }

        return totalReward;
	}

    private JSONObject getSymbolInfo(JSONArray symbols, String symbol) {

        for (int i = 0; i < symbols.length(); i++) {
            JSONObject symbolObj = symbols.getJSONObject(i);
            if (symbolObj.getString("name").equalsIgnoreCase(symbol)) {
                return symbolObj;
            }
        }
        return null; // Symbol not found
    }
    
	// Method to calculate reward based on symbols, win combinations, and bet amount
    private static Map<String, Map<Double, String>> calculateReward(String[][] grid, JSONArray winCombinations) {
        Map<String, Map<Double, String>> elementWin = new HashMap<>();

        Map<String, Integer> elementCounts = countElementOccurrences(grid);
        
        Map<String, Integer> filteredCounts = filterElementCounts(elementCounts, grid.length);
        //if we have elements with count more then 3
        if(filteredCounts != null ) {
        	
            // Iterate over each filtered element count
            for (Map.Entry<String, Integer> entry : filteredCounts.entrySet()) {
                String symbol = entry.getKey();
                int count = entry.getValue();

                // Iterate over each win combination to check for eligible rewards
                for (int i = 0; i < winCombinations.length(); i++) {
                    JSONObject combo = winCombinations.getJSONObject(i);

                    // Get the win condition and required count from the win combination
                    String winCondition = combo.getString("when");

                    double rewardMultiplier = combo.getDouble("reward_multiplier");

                    switch (winCondition) {
                    case "same_symbols":
                        int requiredCount = combo.optInt("count");
                        if(requiredCount == count) {
                        	elementWin.put(symbol, new HashMap<>());
                        	elementWin.get(symbol).put(rewardMultiplier, combo.getString("name"));
                        }
                        break;
                    case "same_symbols_vertically":
                        break;
                    case "same_symbols_horizontally":
                        break;
                    case "same_symbols_diagonally_left_to_right":
                        break;
                    case "same_symbols_diagonally_right_to_left":
                        break;
                    default:
                        break;
                }
                
                }
            }
        	
        }
        return elementWin;
    }
    
 // Define a method to extract bonus symbols from the matrix
    private static  List<String>  getBonusSymbols(Matrix matrix, JSONObject bonusSymbolsObj) {
        JSONObject bonusSymbols = bonusSymbolsObj.getJSONObject("symbols");

    	String[][] grid = matrix.getGrid();
        List<String> bonusSymbol = new ArrayList<>();

        int numRows = grid.length;
        int numCols = grid[0].length;


        // Iterate through the entire matrix
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                String symbol = grid[row][col];

                // Check if the symbol is a bonus symbol
                if (isBonusSymbol(symbol, bonusSymbols)) {
                	bonusSymbol.add(symbol);
                }
            }
        }

        return bonusSymbol;
    }
    
    
    private static boolean isBonusSymbol(String symbol, JSONObject bonusSymbolsObj) {
    	return bonusSymbolsObj.has(symbol);
    }

	// Function to count occurrences of a symbol in the grid
    public static int countSymbols(String[][] grid, String symbol) {
        int count = 0;
        for (String[] row : grid) {
            for (String element : row) {
                if (element.equals(symbol)) {
                    count++;
                }
            }
        }
        return count;
    }
    
 // Function to count occurrences of each element in the matrix
    public static Map<String, Integer> countElementOccurrences(String[][] matrix) {
        Map<String, Integer> counts = new HashMap<>();

        int n = matrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                String element = matrix[i][j];
                counts.put(element, counts.getOrDefault(element, 0) + 1);
            }
        }

        return counts;
    }
    
    public static Map<String, Integer> filterElementCounts(Map<String, Integer> counts, int minCount) {
        Map<String, Integer> filteredCounts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String element = entry.getKey();
            int count = entry.getValue();

            if (count >= minCount) {
                filteredCounts.put(element, count);
            }
        }

        return filteredCounts;
    }

    public double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();  // Convert to double
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);  // Attempt to parse as string
            } catch (NumberFormatException e) {
                // Handle parsing error
                return 0.0;  // Default value
            }
        } else {
            
            return 0.0;  // Default value 
        }
    }


    public int parseInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();  // Convert to int
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);  // Parse string as int
            } catch (NumberFormatException e) {
                // Handle parsing error
                return 0;  // Default value
            }
        } else {
            return 0;  // Default value 
        }
    }

}
