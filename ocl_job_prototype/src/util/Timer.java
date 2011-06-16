package util;

public class Timer {
	private boolean hasStarted;
	private boolean isStopped;
	
	private long startMs, endMs;
	
	public Timer() {
		this.reset();
	}
	
	public void reset() {
		this.hasStarted = false;
		this.isStopped = false;
	}
	
	public void start() {
		this.startMs = System.currentTimeMillis();
		this.hasStarted = true;
	}
	
	public void stop() {
		this.endMs = System.currentTimeMillis();
		this.isStopped = true;
	}
	
	public double getTimeInSeconds() {
		if (this.hasStarted && this.isStopped) {
			return ((double)(this.endMs - this.startMs))/1000;
		} else
			return -1;
	}
	
	@Override
	public String toString() {
		return this.toString("time");
	}
	
	public String toString(String name) {
		return name + "=" + this.getTimeInSeconds();
	}
}
