 import java.util.LinkedList;
 import java.util.List;
 
 
 public class Generator {
 	
 	private Graph graph;
 	private int depth;
 	
 	public List<String> generateLadder(String start, int depth) {
 		List<String> ladder = new LinkedList<String>();
 		if (depth <= 0)
 			return ladder;
 		ladder.add(start);
 		graph = new Graph("dict" + start.length() + ".dat");
 		this.depth = depth;
		return findLadder(ladder, depth - 1);
 	}
 	
 	private List<String> findLadder(List<String> ladder, int toFind) {
 		if (toFind <= 0)
 			return ladder;
 		
 		String current = ladder.get(ladder.size() - 1);
 		List<String> search = graph.getAdjacentWords(current);
 		search.removeAll(ladder);
 		
 		for (String s : search) {
 			ladder.add(s);
 			findLadder(ladder, toFind - 1);
 			if (ladder.size() == depth)
 				break;
 			ladder.remove(ladder.size() - 1);
 		}
 		
 		return ladder;
 	}
 	
 	public static void main(String[] args) {
 		Generator g = new Generator();
 		List<String> ladder = g.generateLadder("too", 500);
 		for (int i = 0; i < ladder.size(); i++) {
 			System.out.println((i + 1) + " - " + ladder.get(i));
 		}
 	}
 	
 }
