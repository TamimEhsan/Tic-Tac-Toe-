package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Optional;
import static java.lang.System.exit;

public class Main extends Application {
    //-------------Graphics shits-----------\\
    private int HEIGHT = 500;
    private int WIDTH = 500;
    private Canvas canvas;
    private GraphicsContext ctx;
    private Text text;
    private Image board;
    private Image circle;
    private Image cross;
    private String headline;
    //-------------info shits-----------\\
    private int player;
    private int[][] boxes;
    private boolean running;
    private boolean connected;
    //-------------Networking shits-----------\\
    private String serverIP = "127.0.0.1";
    private Socket connection;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Thread thread;
    private String messagetoshow;
    private String messagetosend;

    public void init(){
        canvas = new Canvas(HEIGHT,WIDTH);
        ctx = canvas.getGraphicsContext2D();
        board = new javafx.scene.image.Image(getClass().getResourceAsStream("ttt.png"),0,0,true,false);
        circle = new javafx.scene.image.Image(getClass().getResourceAsStream("circle.png"),0,0,true,false);
        cross = new Image(getClass().getResourceAsStream("cross.png"),0,0,true,false);
        ctx.drawImage(board,100,100);
        Font theFont = Font.font( "Times New Roman", FontWeight.BOLD, 48 );
        ctx.setFont( theFont );
        headline = new String("Player 1");
        ctx.fillText(headline,150,80);
        boxes = new int[6][6];
        player = 1;
        running = false;
        connected = false;

        startrunninig();
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        StackPane pane = new StackPane(canvas);
        Scene scene = new Scene(pane);
        primaryStage.setTitle("Tic Tac Toe Client mofo");

        scene.setOnMouseClicked(e->{
            if(running){
                int x = (int)e.getX()/100;
                int y = (int)e.getY()/100;
                if( x>0 && x<4 && y>0 && y<4 && boxes[x][y] == 0 ){
                    boxes[x][y] = player;
                    player = 3-player;
                    update();
                    running = false;
                    messagetosend = new String("_"+x+y);
                    sendMessage(messagetosend);
                    check();
                }
            }
        });
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void update(){
        ctx.clearRect(0,0,HEIGHT,WIDTH);
        ctx.drawImage(board,100,100);
        if( player == 2 ){
            headline = new String("Your turn");
        } else{
            headline = new String("Player 1");
        }
        ctx.fillText(headline,150,80);
        for(int i=1;i<4;i++){
            for(int j=1;j<4;j++){
                if( boxes[i][j]!=0 ){
                    if( boxes[i][j]==1 ){ ctx.drawImage(circle,i*100+10,j*100+10); }
                    else{ ctx.drawImage(cross,i*100+10,j*100+10); }
                }
            }
        }
    }
    private void reset(){
        for(int i=0;i<4;i++){
            for(int j=0;j<4;j++)
                boxes[i][j] = 0;
        }
        running = false;
        player = 1;
        //update();

    }
    private void check(){
        int winner  = 0;
        for(int i=1;i<4;i++){
            if( boxes[i][1] == boxes[i][2] && boxes[i][2]== boxes[i][3] && boxes[i][1]!=0){
                winner = boxes[i][1];
            }
        }
        for(int i=1;i<4;i++){
            if( boxes[1][i] == boxes[2][i]  && boxes[2][i] == boxes[3][i] && boxes[1][i]!=0){
                winner = boxes[1][i];
            }
        }
        if( boxes[1][1]==boxes[2][2] && boxes[2][2]==boxes[3][3] && boxes[2][2]!=0 ){
            winner = boxes[1][1];
        }
        if( boxes[1][3]==boxes[2][2] && boxes[2][2]==boxes[3][1] && boxes[2][2]!=0 ){
            winner = boxes[1][3];
        }
        boolean all = true;
        for(int i=1;i<4;i++){
            for(int j=1;j<4;j++){
                if( boxes[i][j] == 0 )
                    all = false;
            }
        }
        if( winner!=0 ){
            running = false;
            reset();
            if( winner == 2 ) Dialogcontroller("Congratulations! You win");
            else Dialogcontroller("Alas! You lose.");

        } else if( all ){
            running = false;
            reset();
            Dialogcontroller("Game Tied");
        }
    }

    private void Dialogcontroller(final String message) {
        Dialog<ButtonType> dialog = new Dialog<ButtonType>();
        dialog.setTitle("Game Over screen for client");
        dialog.getDialogPane().setContentText(message+".... Start new game?");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if( result.isPresent() && result.get() == ButtonType.OK ){
            update();
        } else if( result.isPresent() && result.get() == ButtonType.CANCEL ){
            if(connected) {
                sendMessage("END");
                closecrap();
            }
            thread.stop();
            exit(0);
        }

    }

    // Netwroking shits
    private void startrunninig(){
        thread = new Thread(){
            @Override
            public void run() {
                try{
                    System.out.println("Waiting bitch");
                    connection = new Socket(InetAddress.getByName(serverIP),5000);
                    System.out.println("Connected");
                    output = new ObjectOutputStream(connection.getOutputStream());
                    output.flush();
                    input = new ObjectInputStream(connection.getInputStream());
                    System.out.println("All set");
                    connected = true;

                    do{
                        messagetoshow=(String)input.readObject();
                        int x = 0;
                        int y = 0;
                        switch (messagetoshow){
                            case "_11": x = 1; y =1; break;
                            case "_12": x = 1; y = 2; break;
                            case "_13": x = 1; y = 3; break;
                            case "_21": x = 2; y =1; break;
                            case "_22": x = 2; y = 2; break;
                            case "_23": x = 2; y = 3; break;
                            case "_31": x = 3; y =1; break;
                            case "_32": x = 3; y = 2; break;
                            case "_33": x = 3; y = 3; break;
                        }
                        boxes[x][y] = player;
                        player = 3-player;
                        update();
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                check();
                            }
                        });
                        running = true;
                    } while(messagetoshow!="END");

                } catch (IOException | ClassNotFoundException e){
                    System.out.println("Server ended");
                } finally {
                    closecrap();
                    connected = false;
                    running = false;
                }
            }
        };
        thread.start();
    }

    private void sendMessage(String message){
        try{
            output.writeObject(message);
            output.flush();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    // close streams and sockets
    private void closecrap(){
        try{
            output.close();
            input.close();
            connection.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        System.out.println("crap closed");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
