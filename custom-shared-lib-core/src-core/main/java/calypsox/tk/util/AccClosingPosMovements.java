package calypsox.tk.util;

public class AccClosingPosMovements {
	
	private double positionPrev;
	private double currentMovement;
	public double getPositionPrev() {
		return positionPrev;
	}
	public void setPositionPrev(double positionPrev) {
		this.positionPrev = positionPrev;
	}
	public double getCurrentMovement() {
		return currentMovement;
	}
	public void setCurrentMovement(double currentMovement) {
		this.currentMovement = currentMovement;
	}
	public AccClosingPosMovements(double positionPrev, double currentMovement) {
		this.positionPrev = positionPrev;
		this.currentMovement = currentMovement;
	}
	
	

}
