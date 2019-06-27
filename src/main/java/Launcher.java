import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Launcher {

    //@TODO Add option to change set program
    //@TODO Add force set program to use on first start(?)
    //@TODO Change Drive Scope to lower level scope
    //@TODO Add more encryption to auth if can

    private static final String APPLICATION_NAME = "OpenCloseSync";
    private static JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static String TOKENS_DIRECTORY_PATH = "tokens";
    private static Drive apiService;
    static Scanner userInput = new Scanner(System.in);

    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE); //unsafe, need alt
    private static final String CREDENTIALS_FILE_PATH =
            ".."+File.separator+"resources"+File.separator+"credentials.json";

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = Launcher.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException {
        System.out.println("1 for download file, 2 for upload file, 3 to set program to open, 4 for exit.");
        System.out.print("Select an option: ");
        int option = userInput.nextInt();

        try{
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            apiService = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch(IOException e){
            System.out.println(e.getMessage());
            System.out.println("An error has occurred.");
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // End auth, start activity
        DriveManager driverSide = new DriveManager();
        DesktopManager desktopSide = new DesktopManager();
        switch (option){
            case 1: {
                String loc = driverSide.downloadFile(apiService);
                desktopSide.runProgram(loc);
                break;
            }
            case 2: {
                desktopSide.killProgram();
                driverSide.uploadFile(apiService);
                break;
            }
            case 3: {

                break;
            }
            case 4: System.exit(0);
        }
    }
}
