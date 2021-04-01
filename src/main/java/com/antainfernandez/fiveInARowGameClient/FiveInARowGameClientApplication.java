package com.antainfernandez.fiveInARowGameClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class FiveInARowGameClientApplication {
   private static final Logger log = LoggerFactory.getLogger(FiveInARowGameClientApplication.class);
    private final String createGameEndPoint = "http://localhost:8081/game/new";
    private final String joinGameEndPoint = "http://localhost:8081/game/join";
    private final String playGameEndPoint = "http://localhost:8081/game/play";
    private final String gamesToJoinEndPoint = "http://localhost:8081/game/list";
    private final String gameStatusEndPoint = "http://localhost:8081/game/status/{gameId}";

    private final String webSocketEndPoint = "ws://localhost:8081/websocket-game";

    SimpleClientHttpRequestFactory httpRequestFactory;
    RestTemplate restTemplate;

    public FiveInARowGameClientApplication() {
        httpRequestFactory = new SimpleClientHttpRequestFactory();
        restTemplate = new RestTemplate(httpRequestFactory);
    }

    public GameStatusDto createGame(Player player) throws URISyntaxException, JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-COM-PERSIST", "true");
        HttpEntity<Player> newGameRequest = new HttpEntity<>(player, headers);
        ResponseEntity<String> result = restTemplate.
                postForEntity(new URI(createGameEndPoint), newGameRequest, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(result.getBody(), GameStatusDto.class);
    }


    public GameStatusDto joinGame(Player player, String gameId) throws URISyntaxException, JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        JoinGameRequest joinGameRequest = new JoinGameRequest();
        joinGameRequest.setPlayer(player);
        joinGameRequest.setGameID(gameId);
        headers.set("X-COM-PERSIST", "true");
        HttpEntity<JoinGameRequest> joinRequest = new HttpEntity<>(joinGameRequest, headers);
        ResponseEntity<String> joinResult = restTemplate.postForEntity(new URI(joinGameEndPoint), joinRequest, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(joinResult.getBody(), GameStatusDto.class);
    }

    public GameStatusDto playGame(Player player, String gameId, String column) throws URISyntaxException, JsonProcessingException {
        PlayRequest playRequest = new PlayRequest();
        playRequest.setPlayer(player);
        playRequest.setGameID(gameId);
        playRequest.setColumn(Integer.parseInt(column));
        HttpHeaders headers1 = new HttpHeaders();
        headers1.set("X-COM-PERSIST", "true");
        HttpEntity<PlayRequest> playGameRequest = new HttpEntity<>(playRequest, headers1);
        ResponseEntity<String> playGameResult = null;
        playGameResult = restTemplate.postForEntity(new URI(playGameEndPoint), playGameRequest, String.class);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(playGameResult.getBody(), GameStatusDto.class);
    }

    public GameStatusDto getGameStatus(String gameId) {
        Map<String, String> vars = new HashMap<>();
        vars.put("gameId", gameId);
        GameStatusDto response =
                this.restTemplate.getForObject(gameStatusEndPoint, GameStatusDto.class, vars);
        return response;
    }

    public String getGamesToJoin() {
        String[] s = restTemplate.getForObject(gamesToJoinEndPoint, String[].class);
        StringBuilder builder = new StringBuilder();
        Arrays.stream(s).forEach(s1 -> builder.append(s1).append("\n"));
        return builder.toString();
    }


    public static class MyStompSessionHandler extends StompSessionHandlerAdapter {

        private Logger logger = LoggerFactory.getLogger(MyStompSessionHandler.class);

        String gameId;
        Player player;
        AtomicBoolean play;

        public MyStompSessionHandler(String gameId, Player player, AtomicBoolean play) {
            this.gameId = gameId;
            this.player = player;
            this.play = play;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            session.subscribe("/topic/game/" + gameId, this);
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            logger.error("Got an exception", exception);
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return GameStatusDto.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            GameStatusDto gameDto = (GameStatusDto) payload;
            if (gameDto.currentPlayer.equals(player.name) || gameDto.getGameStatus().equals("OVER")) {
                play.set(true);
            }
        }

        public AtomicBoolean getPlay() {
            return play;
        }

        public void setPlay(AtomicBoolean play) {
            this.play = play;
        }
    }

    public static class PlayRequest {

        String gameID;
        Player player;
        int column;

        public String getGameID() {
            return gameID;
        }

        public void setGameID(String gameID) {
            this.gameID = gameID;
        }

        public Player getPlayer() {
            return player;
        }

        public void setPlayer(Player player) {
            this.player = player;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }
    }


    public static class Player {

        private String name;

        private char disk;

        public Player() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public char getDisk() {
            return disk;
        }

        public void setDisk(char disk) {
            this.disk = disk;
        }
    }

    private static class JoinGameRequest {

        String gameID;
        Player player;

        public String getGameID() {
            return gameID;
        }

        public void setGameID(String gameID) {
            this.gameID = gameID;
        }

        public Player getPlayer() {
            return player;
        }

        public void setPlayer(Player player) {
            this.player = player;
        }
    }

    public static class GameStatusDto {

        private String id;
        private String player1;
        private String player2;
        private String player1Disk;
        private String player2Disk;
        private String gameStatus;
        private String currentPlayer;
        private String winner;
        private String board;

        public GameStatusDto() {
            super();
        }


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPlayer1() {
            return player1;
        }

        public void setPlayer1(String player1) {
            this.player1 = player1;
        }

        public String getPlayer2() {
            return player2;
        }

        public void setPlayer2(String player2) {
            this.player2 = player2;
        }

        public String getPlayer1Disk() {
            return player1Disk;
        }

        public void setPlayer1Disk(String player1Disk) {
            this.player1Disk = player1Disk;
        }

        public String getPlayer2Disk() {
            return player2Disk;
        }

        public void setPlayer2Disk(String player2Disk) {
            this.player2Disk = player2Disk;
        }

        public String getGameStatus() {
            return gameStatus;
        }

        public void setGameStatus(String gameStatus) {
            this.gameStatus = gameStatus;
        }

        public String getCurrentPlayer() {
            return currentPlayer;
        }

        public void setCurrentPlayer(String currentPlayer) {
            this.currentPlayer = currentPlayer;
        }

        public String getWinner() {
            return winner;
        }

        public void setWinner(String winner) {
            this.winner = winner;
        }

        public String getBoard() {
            return board;
        }

        public void setBoard(String board) {
            this.board = board;
        }
    }


    public static void main(String[] args) throws IOException, URISyntaxException {
        WebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        StompSessionHandler sessionHandler = null;
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        FiveInARowGameClientApplication app = new FiveInARowGameClientApplication();

        Scanner scanner = new Scanner(System.in);


        String gameId = null;
        String board = null;
        Player player = new Player();
        GameStatusDto gameStatusDto = null;

        System.out.println("Welcome to connect 5 Game ...");
        System.out.println("Enter name");
        String playerName = scanner.nextLine();
        player.setName(playerName);

        String option;
        while (true) {
            System.out.println("Enter option : create new game (c) , join game (j)");
            option = scanner.nextLine();
            if (option.equals("c") || option.equals("j")) {
                break;
            }
        }

        if (option.equals("c")) {
            gameStatusDto = app.createGame(player);
            gameId = gameStatusDto.getId();
            player.setDisk(gameStatusDto.getPlayer1Disk().charAt(0));
            System.out.println(gameStatusDto.getBoard());
            System.out.println("New game with id " + gameId +
                    "  has been created - Player 1:" + gameStatusDto.getPlayer1() + "  plays with " +
                    gameStatusDto.getPlayer1Disk());
            System.out.println("Wait until other player join");

        } else if (option.equals("j")) {
            String gamesToJoin = app.getGamesToJoin();
            System.out.println("Enter game to join :");
            System.out.println(gamesToJoin);
            gameId = scanner.nextLine();

            gameStatusDto = app.joinGame(player, gameId);
            player.setDisk(gameStatusDto.getPlayer2Disk().charAt(0));
            System.out.println("Joined game with id " + gameId +
                    "Player 1:" + gameStatusDto.getPlayer1() + " plays with " +
                    gameStatusDto.getPlayer1Disk() +
                    "Player 2:" + gameStatusDto.getPlayer2() + " plays with " +
                    gameStatusDto.getPlayer2Disk());
        }

        //subscribe to stomp , every time there is a play, notify and change play to true
        AtomicBoolean play = new AtomicBoolean();
        play.set(false);
        sessionHandler = new MyStompSessionHandler(gameId, player, play);
        stompClient.connect(app.webSocketEndPoint, sessionHandler);

        //run game until is over
        boolean gameOver = false;
        while (!gameOver) {

            if (play.get()) {
                play.set(false);
                gameStatusDto = app.getGameStatus(gameId);

                if (gameStatusDto.getWinner().equals(gameStatusDto.getPlayer1()) || gameStatusDto.getWinner().equals(gameStatusDto.getPlayer2()) || gameStatusDto.getGameStatus().equals("OVER")) {
                    System.out.println(gameStatusDto.getBoard());
                    System.out.println("Game over - Player  " + gameStatusDto.getWinner() +" wins !!!");
                    gameOver = true;
                } else {
                    System.out.println(gameStatusDto.getBoard());
                    System.out.println("Player :" + gameStatusDto.getCurrentPlayer() + " Enter column from 1 -9: ");
                    String column;
                    while (true) {
                        column = scanner.nextLine();
                        if (column.matches("[1-9]")) {
                            break;
                        }
                    }
                    gameStatusDto = app.playGame(player, gameId, column);
                    System.out.println(gameStatusDto.getBoard());
                    System.out.println(
                            "Game : " + gameId +
                                    " Player 1: " +
                                    gameStatusDto.getPlayer1() +
                                    " (" + gameStatusDto.getPlayer1Disk() + ") " +
                                    " Player 2: " + gameStatusDto.getPlayer2() +
                                    " (" + gameStatusDto.getPlayer2Disk() + ") ");
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stompClient.stop();
    }

}
