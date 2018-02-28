package tablut;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Math;

public class Coord {
	public final int x; // make it final so they cannot be changed
	public final int y;
	
	public Coord(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Returns a list of all the coordinates between this coord
	 * and another coordinate. Note that it assumes they are
	 * either in the same row or same column, otherwise it will
	 * return an empty list. 
	 * */
	public List<Coord> getCoordsBetween(Coord c) {
		List<Coord> coords = new ArrayList<Coord>();
		boolean updateY = this.x == c.x; // update y if x's are the same
		boolean updateX = this.y == c.y; // update x if y's are the same
			
		// So tedious, but necessary, I guess...
		int start, end;
		if (updateX && !updateY) {
			start = this.x;
			end = c.x;
		}
		else if (updateY && !updateX) {
			start = this.y;
			end = c.y;
		}
		else {
			return coords;
		}
		
		// Set the increment and then do the incrementation.
		int incr = (start > end) ? -1 : 1;
		int i = start;
		while (i != end) {
			i += incr;
			if (updateX) coords.add(new Coord(i, this.y));
			if (updateY) coords.add(new Coord(this.x, i));
		}
		return coords;
	}
	
	/**
	 * Returns the maximum coordinate difference between two coords.
	 */
	public int maxDifference(Coord c) {
		return Math.max(Math.abs(this.x - c.x), Math.abs(this.y - c.y));
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Coord))
			return false;
		Coord c = (Coord) obj;
		return this.x == c.x && this.y == c.y;
	}
	
	@Override
	public String toString() {
		return String.format("(%d %d)", this.x, this.y);
	}
	
	// Debugging
	public static void main(String[] args) {
		boolean testEquality = true;
		boolean testBetweenCoords = true;
		
		Coord a = new Coord(1,1);
		Coord b = new Coord(1,1);
		Coord c = new Coord(1,2);
		Coord d = new Coord(2,1);
		List<Coord> coords = Arrays.asList(a,b,c,d);

		if (testEquality) {
			for (Coord coord1 : coords) {
				for (Coord coord2 : coords) {
					System.out.print(coord1.toString() + " =?= " + coord2.toString() + ": ");
					System.out.println(coord1.equals(coord2));
				}
			}
		}
		
		if (testBetweenCoords) {
			Coord beg = new Coord(1, 1);
			Coord endRow = new Coord(1, 4);
			Coord endCol = new Coord(4, 1);
			Coord endDiag = new Coord(4, 4);
			coords = Arrays.asList(endRow, endCol, endDiag);
			for (Coord co : coords) {
				for (Coord start : Arrays.asList(beg, co)) {
					for (Coord end : Arrays.asList(co, beg)) {
						if (start.equals(end)) continue;
						System.out.println("\nStart: " + start.toString());
						for (Coord through : start.getCoordsBetween(end)) {
							System.out.println("    -> " + through.toString());
						}
						System.out.println("->End: " + end.toString());
					}
				}
			}
			
		}
	}
}
