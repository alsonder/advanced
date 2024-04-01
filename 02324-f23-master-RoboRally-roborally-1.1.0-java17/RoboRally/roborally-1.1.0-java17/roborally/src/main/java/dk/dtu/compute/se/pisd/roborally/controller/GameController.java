/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * The GameController class is responsible for controlling the game flow and executing player actions.
 * It interacts with the Board and Player classes to manipulate the game state.
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 */
public class GameController {

    final public Board board;

    public GameController(@NotNull Board board) {
        this.board = board;
    }



    /**
     * Moves the current player to a specified space on the board.
     * If the space is free, the current player is moved to that space.
     * The current player is then set to the next player in the list, or if there are no more players, it is set back to the first player.
     * The move counter of the board is also incremented.
     *
     * @param space The space to which the current player should be moved. This should be a free space.
     * @author Aleksander Sonder
     */
    public void moveCurrentPlayerToSpace(@NotNull Space space, @NotNull Heading heading) {
        Player currentPlayer = board.getCurrentPlayer();
        if (space.isFree()) {
            board.incrementMoveCounter();
            currentPlayer.setSpace(space);
            int nextPlayerIndex = (board.getPlayerNumber(currentPlayer) + 1) % board.getPlayersNumber();
            board.setCurrentPlayer(board.getPlayer(nextPlayerIndex));
        }
    }


    /**
     * Starts the programming phase of the game.
     * Sets the game phase to PROGRAMMING, sets the current player to the first player, and resets the step counter to 0.
     * For each player, it clears their program fields and fills their card fields with random command cards.
     * The program fields and card fields are made visible for the players.
     */
    public void startProgrammingPhase() {
        board.setPhase(Phase.PROGRAMMING);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            if (player != null) {
                for (int j = 0; j < Player.NO_REGISTERS; j++) {
                    CommandCardField field = player.getProgramField(j);
                    field.setCard(null);
                    field.setVisible(true);
                }
                for (int j = 0; j < Player.NO_CARDS; j++) {
                    CommandCardField field = player.getCardField(j);
                    field.setCard(generateRandomCommandCard());
                    field.setVisible(true);
                }
            }
        }
    }

    /**
     * Generates a random CommandCard.
     * It first gets all possible Command values, then selects a random one to create a new CommandCard.
     *
     * @return a new CommandCard with a random Command
     */
    private CommandCard generateRandomCommandCard() {
        Command[] commands = Command.values();
        int random = (int) (Math.random() * commands.length);
        return new CommandCard(commands[random]);
    }

    /**
     * Finishes the programming phase of the game.
     * It makes all program fields invisible, then makes the first program field visible.
     * Sets the game phase to ACTIVATION, sets the current player to the first player, and resets the step counter to 0.
     */
    public void finishProgrammingPhase() {
        makeProgramFieldsInvisible();
        makeProgramFieldsVisible(0);
        board.setPhase(Phase.ACTIVATION);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);
    }

    /**
     * Makes the program fields visible for a given register.
     * If the register is within the valid range (0 to Player.NO_REGISTERS), it iterates over all players.
     * For each player, it retrieves the CommandCardField for the given register and sets it to visible.
     *
     * @param register The register for which the program fields should be made visible.
     */
    private void makeProgramFieldsVisible(int register) {
        if (register >= 0 && register < Player.NO_REGISTERS) {
            for (int i = 0; i < board.getPlayersNumber(); i++) {
                Player player = board.getPlayer(i);
                CommandCardField field = player.getProgramField(register);
                field.setVisible(true);
            }
        }
    }

    /**
     * Makes all program fields invisible for all players.
     * It iterates over all players and for each player, it retrieves each CommandCardField in their program and sets it to invisible.
     */
    private void makeProgramFieldsInvisible() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            for (int j = 0; j < Player.NO_REGISTERS; j++) {
                CommandCardField field = player.getProgramField(j);
                field.setVisible(false);
            }
        }
    }

    /**
     * Executes all programs for all players.
     * It sets the step mode of the board to false, indicating that all steps should be executed at once.
     * Then it calls the continuePrograms method to execute the programs.
     */
    public void executePrograms() {
        board.setStepMode(false);
        continuePrograms();
    }

    /**
     * Executes the next step for all players.
     * It sets the step mode of the board to true, indicating that only one step should be executed.
     * Then it calls the continuePrograms method to execute the step.
     */
    public void executeStep() {
        board.setStepMode(true);
        continuePrograms();
    }

    /**
     * Continues executing the programs for all players.
     * It repeatedly calls the executeNextStep method as long as the game phase is ACTIVATION and the board is not in step mode.
     */
    private void continuePrograms() {
        do {
            executeNextStep();
        } while (board.getPhase() == Phase.ACTIVATION && !board.isStepMode());
    }

    /**
     * Executes the next step in the game.
     * If the game phase is ACTIVATION and there is a current player, it retrieves the CommandCard from the current player's program at the current step.
     * If the CommandCard is not null, it executes the command on the current player.
     * Then it increments the player number to move to the next player.
     * If there are no more players, it increments the step and sets the current player back to the first player.
     * If there are no more steps, it starts the programming phase.
     * If the game phase is not ACTIVATION or there is no current player, it throws an assertion error.
     */
    private void executeNextStep() {
        Player currentPlayer = board.getCurrentPlayer();
        if (board.getPhase() == Phase.ACTIVATION && currentPlayer != null) {
            int step = board.getStep();
            if (step >= 0 && step < Player.NO_REGISTERS) {
                CommandCard card = currentPlayer.getProgramField(step).getCard();
                if (card != null) {
                    Command command = card.command;
                    executeCommand(currentPlayer, command);
                }
                int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
                if (nextPlayerNumber < board.getPlayersNumber()) {
                    board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
                } else {
                    step++;
                    if (step < Player.NO_REGISTERS) {
                        makeProgramFieldsVisible(step);
                        board.setStep(step);
                        board.setCurrentPlayer(board.getPlayer(0));
                    } else {
                        startProgrammingPhase();
                    }
                }
            } else {
                // this should not happen
                assert false;
            }
        } else {
            // this should not happen
            assert false;
        }
    }

    /**
     * Executes a given command for a specified player.
     * If the player and command are not null and the player's board is the same as this controller's board, it executes the command.
     * The command is executed based on its type: FORWARD, RIGHT, LEFT, FAST_FORWARD. For other command types, it does nothing.
     *
     * @param player The player for whom the command should be executed.
     * @param command The command to be executed.
     */
    private void executeCommand(@NotNull Player player, Command command) {
        if (player != null && player.board == board && command != null) {
            board.incrementMoveCounter();
            // XXX This is a very simplistic way of dealing with some basic cards and
            //     their execution. This should eventually be done in a more elegant way
            //     (this concerns the way cards are modelled as well as the way they are executed).

            switch (command) {
                case FORWARD:
                    this.moveForward(player, player.getHeading());
                    break;
                case RIGHT:
                    this.turnRight(player);
                    break;
                case LEFT:
                    this.turnLeft(player);
                    break;
                case FAST_FORWARD:
                    this.fastForward(player);
                    break;
                default:
                    // DO NOTHING (for now)
            }
        }
    }

    /**
     * Moves the player 1 tile forward, by taking the player and heading as argument
     *
     * @param player, The player who the command will be executed on
     * @param heading, the current heading of the player
     */

    public void moveForward(@NotNull Player player, Heading heading) {
        Space source = player.getSpace();
        Space destination = board.getNeighbour(source, heading);
        moveCurrentPlayerToSpace(destination, heading);
    }

    // TODO Task2
    public void fastForward(@NotNull Player player) {
    }

    /**
     * Turns the player heading right
     * @param player The player who uses the turn right command
     */
    public void turnRight(@NotNull Player player) {
        Heading heading = player.getHeading();
        if(heading == Heading.SOUTH){
            player.setHeading(Heading.WEST);
        } else if (heading == Heading.EAST) {
            player.setHeading(Heading.SOUTH);
        }
        else if (heading == Heading.NORTH) {
            player.setHeading(Heading.EAST);
        }
        else if (heading == Heading.WEST) {
            player.setHeading(Heading.NORTH);
        }
    }

    /**
     * Turns the player heading left
     *
     * @param player The player who uses the turn left command
     */
    public void turnLeft(@NotNull Player player) {
        Heading heading = player.getHeading();
        if(heading == Heading.SOUTH){
            player.setHeading(Heading.EAST);
        } else if (heading == Heading.EAST) {
            player.setHeading(Heading.NORTH);
        }
        else if (heading == Heading.NORTH) {
            player.setHeading(Heading.WEST);
        } else if (heading == Heading.WEST) {
            player.setHeading(Heading.SOUTH);
        }
    }

    /**
     * Moves a CommandCard from a source CommandCardField to a target CommandCardField.
     * If the source CommandCardField has a card and the target CommandCardField is empty, the card is moved.
     * The card in the source CommandCardField is then set to null.
     * If the move is successful, the method returns true. If the move is not successful (i.e., the source is empty or the target is not empty), it returns false.
     *
     * @param source The source CommandCardField from which the card should be moved.
     * @param target The target CommandCardField to which the card should be moved.
     * @return true if the move was successful, false otherwise.
     */
    public boolean moveCards(@NotNull CommandCardField source, @NotNull CommandCardField target) {
        CommandCard sourceCard = source.getCard();
        CommandCard targetCard = target.getCard();
        if (sourceCard != null && targetCard == null) {
            target.setCard(sourceCard);
            source.setCard(null);
            return true;
        } else {
            return false;
        }
    }

    /**
     * A method called when no corresponding controller operation is implemented yet. This
     * should eventually be removed.
     */
    public void notImplemented() {
        // XXX just for now to indicate that the actual method is not yet implemented
        assert false;
    }

}
