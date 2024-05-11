package s.game;

import org.json.JSONObject;

import s.game.service.impl.ScratchGameServiceImpl;
import s.game.util.JsonUtils;

public class ScratchGame {
    
	 public static void main(String[] args) {
	        if (args.length < 4 || !args[0].equals("--config") || !args[2].equals("--betting-amount")) {
	            System.err.println("Usage: java -jar <your-jar-file>.jar --config <config_file_path> --betting-amount <amount>");
	            System.exit(1);
	        }

	        String configFilePath = args[1];
	        int betAmount = Integer.parseInt(args[3]);

	        try {
	            JSONObject configJson = JsonUtils.parseJsonFile(configFilePath);

	            // Initialize ScratchGameService and invoke game logic
	            ScratchGameServiceImpl gameService = new ScratchGameServiceImpl();
	            JSONObject result = gameService.evaluateGame(configJson, betAmount);

	            // Print result JSON
	            System.out.println(result.toString(4));

	        } catch (Exception e) {
	            e.printStackTrace();
	            throw new RuntimeException(e.getMessage());
	        }
	    }
	}