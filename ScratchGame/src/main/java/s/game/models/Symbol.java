package s.game.models;

public class Symbol {

	private String name;
    private double rewardMultiplier;
    private String type;
    private Double extra; // Optional field for bonus symbols
    private String impact; // Optional field for bonus symbols
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getRewardMultiplier() {
		return rewardMultiplier;
	}
	public void setRewardMultiplier(double rewardMultiplier) {
		this.rewardMultiplier = rewardMultiplier;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Double getExtra() {
		return extra;
	}
	public void setExtra(Double extra) {
		this.extra = extra;
	}
	public String getImpact() {
		return impact;
	}
	public void setImpact(String impact) {
		this.impact = impact;
	}
	public Symbol() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Symbol(String name, double rewardMultiplier, String type, Double extra, String impact) {
		super();
		this.name = name;
		this.rewardMultiplier = rewardMultiplier;
		this.type = type;
		this.extra = extra;
		this.impact = impact;
	}
    
    
	
}
