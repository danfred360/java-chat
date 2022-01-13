package com.npole.app;

public class ClientGUI extends Application {
    private String server, username;
    private int port;
    private boolean connected;

    public TextArea chatArea = new TextArea();
    public ListView<String> listView = new ListView<String>();

    private Client client;

    // app preferences
    public Preferences prefs = Preferences.userNodeForPackage(ClientGUI.class);

    // detect os
    private static String OS = System.getProperty("os.name").toLowerCase();

    void cleanup() {
        // stop, reset - clean up window ??
    }

    // show server select dialog
    @Override
    public void start(Stage serverStage) {
        connected = false;

        serverStage.setTitle("Chat Setup");
        serverStage.show();
        serverStage.getIcons.getIcons().add(new Image(this.getClass().getResourceAsStream("res/icon64.png")));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
    }
}
