package sudoku.processors;

import sudoku.backtrack.BacktrackBoard;
import sudoku.backtrack.containers.BacktrackColumnContainer;
import sudoku.backtrack.containers.BacktrackContainersDto;
import sudoku.backtrack.containers.BacktrackRowContainer;
import sudoku.backtrack.containers.BacktrackZoneContainer;
import sudoku.backtrack.processors.BacktrackProcessor;
import sudoku.backtrack.processors.SavedGame;
import sudoku.board.SudokuBoard;
import sudoku.board.SudokuElement;
import sudoku.containers.NumbersContainerProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static sudoku.board.SudokuBoard.MAX_INDEX;
import static sudoku.board.SudokuBoard.MIN_INDEX;
import static sudoku.board.SudokuElement.EMPTY_VALUE;


public class GameResolver {
    private NumbersValidator numbersValidator;
    private NumbersContainerProcessor numbersContainerProcessor;
    private boolean outcome = false;

    public GameResolver(NumbersValidator numbersValidator, NumbersContainerProcessor numbersContainerProcessor) {
        this.numbersValidator = numbersValidator;
        this.numbersContainerProcessor = numbersContainerProcessor;
    }

    public void solveSudoku() {
        updateBoard();
        do {
            if(isSudokuIsPossibleToSolve()) {
                System.out.println(numbersValidator.getSudokuBoard());
//                do {
//                    checkBoardForSingleAvailableValues();
//                    updateBoard();
//                } while (outcome);
                if (isSudokuFinished(numbersValidator.getSudokuBoard())){
                    break;
                }
                checkBoardElementsForMoreAvailableValues();
                updateBoard();
            } else {
                loadGame();
                updateBoard();
            }
        } while (!isSudokuFinished(numbersValidator.getSudokuBoard()));
    }

    private void updateCoordsAvailableValues(int x, int y, int zone) {
        SudokuBoard sudokuBoard = numbersValidator.getSudokuBoard();
        List<Integer> columns = numbersContainerProcessor.getColumnContainer().getColumns().get(x);
        List<Integer> rows = numbersContainerProcessor.getRowContainer().getRows().get(y);
        List<Integer> zones = numbersContainerProcessor.getZoneContainer().getZones().get(zone);

        Integer rowsSize = rows.size();
        Integer columnsSize = columns.size();
        Integer zonesSize = zones.size();

        Map<Integer, List<Integer>> sizesWithContainers = new HashMap<>();
        sizesWithContainers.put(rowsSize, rows);
        sizesWithContainers.put(columnsSize, columns);
        sizesWithContainers.put(zonesSize, zones);

        List<Integer> listsSizes = new ArrayList<>();
        listsSizes.add(rowsSize);
        listsSizes.add(columnsSize);
        listsSizes.add(zonesSize);

        Collections.sort(listsSizes);
        List<Integer> smallestContainerList = sizesWithContainers.get(listsSizes.get(0));

        List<Integer> availableForPosition = sudokuBoard.getBoard().get(y).getElements().get(x).getAvailableForPosition();
        availableForPosition.clear();
        for (Integer element : smallestContainerList)
            if (sizesWithContainers.get(listsSizes.get(1)).contains(element) &&
                    sizesWithContainers.get(listsSizes.get(2)).contains(element) &&
                    numbersValidator.coordsValidator(x + 1, y + 1, element)) {
                availableForPosition.add(element);
            }
    }

    private void updateBoard() {
        for (int y = MIN_INDEX; y < MAX_INDEX; y++) {
            for (int x = MIN_INDEX; x < MAX_INDEX; x++) {
                int tempValue = numbersValidator.getSudokuBoard().getBoard().get(y).getElements().get(x).getValue();
                int tempZone = numbersValidator.getSudokuBoard().getBoard().get(y).getElements().get(x).getZone() - 1;
                if (tempValue == EMPTY_VALUE) {
                    updateCoordsAvailableValues(x, y, tempZone);
                }
            }
        }
    }

    private void checkBoardForSingleAvailableValues() {
        List<SudokuElement> sudokuWithOneAvailableValueList = numbersValidator.getSudokuBoard().getBoard().stream()
                .flatMap(row -> row.getElements().stream())
                .filter(f -> f.getAvailableForPosition().size() == 1)
                .collect(Collectors.toList());
        boolean isReadyToExit;
        int listIndex = 0;
        do {
            if (sudokuWithOneAvailableValueList.size() > 0 && sudokuWithOneAvailableValueList.size() >= listIndex) {
                SudokuElement sudokuElement = sudokuWithOneAvailableValueList.get(0);
                for(SudokuElement element: sudokuWithOneAvailableValueList) {
                    System.out.println(element.getValue());
                    System.out.println(element.getPositionX());
                    System.out.println(element.getPositionY());
                    System.out.println(element.getAvailableForPosition().size());
                    System.out.println(element.getAvailableForPosition().get(0));
                    System.out.println("-----");
                }

                int zone = sudokuElement.getZone() - 1;
                int posX = sudokuElement.getPositionX();
                int posY = sudokuElement.getPositionY();
                int valueToInsert = sudokuElement.getAvailableForPosition().get(listIndex);

                boolean isNumberValid = numbersValidator.coordsValidator(posX + 1, posY + 1, valueToInsert);
                if (isNumberValid) {
                    sudokuElement.setValue(valueToInsert);
                    sudokuElement.getAvailableForPosition().clear();
                    numbersContainerProcessor.removeFromAvailableNumbers(posX + 1, posY + 1, zone + 1, valueToInsert);
                    outcome = true;
                    isReadyToExit = true;
                } else {
                    listIndex++;
                    outcome = false;
                    isReadyToExit = false;
                }
            } else {
                outcome = false;
                isReadyToExit = true;
            }
        } while (!isReadyToExit);
    }

    private void checkBoardElementsForMoreAvailableValues() {
        Integer smallestListOfAvailableInPosition = numbersValidator.getSudokuBoard().getBoard().stream()
                .flatMap(row -> row.getElements().stream())
                .filter(f -> f.getAvailableForPosition().size() > 0)
                .mapToInt(m -> m.getAvailableForPosition().size())
                .min().orElseThrow(NoSuchElementException::new);

        List<SudokuElement> elementWithSmallestAvailableList = numbersValidator.getSudokuBoard().getBoard().stream()
                .flatMap(row -> row.getElements().stream())
                .filter(f -> f.getAvailableForPosition().size() == smallestListOfAvailableInPosition)
                .collect(Collectors.toList());

        int listIndex = 0;
        SudokuElement sudokuElement = elementWithSmallestAvailableList.get(0);
        int posX = sudokuElement.getPositionX();
        int posY = sudokuElement.getPositionY();
        int zone = sudokuElement.getZone();
        Integer valueToInsert = sudokuElement.getAvailableForPosition().get(listIndex);

        boolean isNumberValid = numbersValidator.coordsValidator(posX + 1, posY + 1, valueToInsert);
        if (!isNumberValid) {
            listIndex++;
            sudokuElement = elementWithSmallestAvailableList.get(listIndex);
        }

        sudokuElement.setValue(valueToInsert);
        createSnapshotOfBoard();
        numbersContainerProcessor.removeFromAvailableNumbers(posX + 1, posY + 1, zone, valueToInsert);
        sudokuElement.getAvailableForPosition().clear();
    }

    private boolean isSudokuIsPossibleToSolve() {
        List<SudokuElement> invalidElementsList = numbersValidator.getSudokuBoard().getBoard().stream()
                .flatMap(row -> row.getElements().stream())
                .filter(f -> (f.getAvailableForPosition().size() == 0) && (f.getValue() == EMPTY_VALUE))
                .collect(Collectors.toList());

        boolean isSolvable = false;
        if (invalidElementsList.size() == 0) {
            isSolvable = true;
        }
        return isSolvable;
    }

    private void createSnapshotOfBoard(){
        BacktrackBoard backtrackBoard = new BacktrackBoard();
        backtrackBoard.createDeepClonedBoard(numbersValidator.getSudokuBoard());
        BacktrackZoneContainer backtrackZoneContainer = new BacktrackZoneContainer();
        backtrackZoneContainer.createDeepClonedZoneContainer(numbersContainerProcessor);
        BacktrackRowContainer backtrackRowContainer = new BacktrackRowContainer();
        backtrackRowContainer.createDeepClonedRowContainer(numbersContainerProcessor);
        BacktrackColumnContainer backtrackColumnContainer = new BacktrackColumnContainer();
        backtrackColumnContainer.createDeepClonedColumnContainer(numbersContainerProcessor);
        BacktrackContainersDto backtrackContainersDto = new BacktrackContainersDto(backtrackColumnContainer, backtrackRowContainer, backtrackZoneContainer);
        SavedGame savedGame = new SavedGame(backtrackBoard, backtrackContainersDto);
        BacktrackProcessor.getInstance().saveGame(savedGame);
    }

    private void loadGame() {
        SavedGame loadedGame = BacktrackProcessor.getInstance().getAllSavedGames().peekFirst();
        numbersValidator.getSudokuBoard().setBoard(loadedGame.getBacktrackBoard().getClonedBoard());
        numbersContainerProcessor.getColumnContainer().setColumns(loadedGame.getBacktrackContainersDto().getBacktrackColumnContainer().getDeepClonedColumns());
        numbersContainerProcessor.getRowContainer().setRows(loadedGame.getBacktrackContainersDto().getBacktrackRowContainer().getDeepClonedRows());
        numbersContainerProcessor.getZoneContainer().setZones(loadedGame.getBacktrackContainersDto().getBacktrackZoneContainer().getDeepClonedZones());
        BacktrackProcessor.getInstance().getAllSavedGames().poll();
    }

    private boolean isSudokuFinished(SudokuBoard sudokuBoard){
        boolean isSudokuDone = sudokuBoard.getBoard().stream()
                .flatMap(row -> row.getElements().stream())
                .noneMatch(v -> v.getValue() == EMPTY_VALUE);
        return isSudokuDone;
    }
}