package tablut;

import boardgame.Move;

public class RandomTablutPlayer extends TablutPlayer {
    public RandomTablutPlayer() {
        super("RandomPlayer");
    }

    public RandomTablutPlayer(String name) {
        super(name);
    }

    @Override
    public Move chooseMove(TablutBoardState boardState) {
        return boardState.getRandomMove();
    }
}
